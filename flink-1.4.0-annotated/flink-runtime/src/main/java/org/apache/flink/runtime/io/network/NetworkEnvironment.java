/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.io.network;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.io.disk.iomanager.IOManager;
import org.apache.flink.runtime.io.disk.iomanager.IOManager.IOMode;
import org.apache.flink.runtime.io.network.api.writer.ResultPartitionWriter;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.io.network.partition.ResultPartition;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.consumer.SingleInputGate;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.query.KvStateClientProxy;
import org.apache.flink.runtime.query.KvStateRegistry;
import org.apache.flink.runtime.query.KvStateServer;
import org.apache.flink.runtime.query.TaskKvStateRegistry;
import org.apache.flink.runtime.state.internal.InternalKvState;
import org.apache.flink.runtime.taskmanager.Task;
import org.apache.flink.runtime.taskmanager.TaskManager;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Network I/O components of each {@link TaskManager} instance. The network environment contains
 * the data structures that keep track of all intermediate results and all data exchanges.
 *
 * NetworkEnvironment 是每个 TaskManager 实例的网络 I/O 组件。
 * NetworkEnvironment 包含跟踪所有中间结果和所有数据交换的数据结构。
 */
public class NetworkEnvironment {

	private static final Logger LOG = LoggerFactory.getLogger(NetworkEnvironment.class);

	private final Object lock = new Object();

	private final NetworkBufferPool networkBufferPool;

	private final ConnectionManager connectionManager;

	private final ResultPartitionManager resultPartitionManager;

	private final TaskEventDispatcher taskEventDispatcher;

	/** Server for {@link InternalKvState} requests. */
	/** InternalKvState 请求的服务器 */
	private KvStateServer kvStateServer;

	/** Proxy for the queryable state client. */
	/** 可查询的状态客户端代理 */
	private KvStateClientProxy kvStateProxy;

	/** Registry for {@link InternalKvState} instances. */
	/** InternalKvState 实例注册表 */
	private final KvStateRegistry kvStateRegistry;

	private final IOManager.IOMode defaultIOMode;

	private final int partitionRequestInitialBackoff;

	private final int partitionRequestMaxBackoff;

	/** Number of network buffers to use for each outgoing/incoming channel (subpartition/input channel). */
	/** 用于每个输入/输出通道(input channel/subpartition)的网络缓冲区的数量。 */
	private final int networkBuffersPerChannel;

	/** Number of extra network buffers to use for each outgoing/incoming gate (result partition/input gate). */
	/** 每个输入输出 Gate (input gate/result partition)使用的额外网络缓冲区数量。 */
	private final int extraNetworkBuffersPerGate;

	private boolean isShutdown;

	public NetworkEnvironment(
			NetworkBufferPool networkBufferPool,
			ConnectionManager connectionManager,
			ResultPartitionManager resultPartitionManager,
			TaskEventDispatcher taskEventDispatcher,
			KvStateRegistry kvStateRegistry,
			KvStateServer kvStateServer,
			KvStateClientProxy kvStateClientProxy,
			IOMode defaultIOMode,
			int partitionRequestInitialBackoff,
			int partitionRequestMaxBackoff,
			int networkBuffersPerChannel,
			int extraNetworkBuffersPerGate) {

		this.networkBufferPool = checkNotNull(networkBufferPool);
		this.connectionManager = checkNotNull(connectionManager);
		this.resultPartitionManager = checkNotNull(resultPartitionManager);
		this.taskEventDispatcher = checkNotNull(taskEventDispatcher);
		this.kvStateRegistry = checkNotNull(kvStateRegistry);

		this.kvStateServer = kvStateServer;
		this.kvStateProxy = kvStateClientProxy;

		this.defaultIOMode = defaultIOMode;

		this.partitionRequestInitialBackoff = partitionRequestInitialBackoff;
		this.partitionRequestMaxBackoff = partitionRequestMaxBackoff;

		isShutdown = false;
		this.networkBuffersPerChannel = networkBuffersPerChannel;
		this.extraNetworkBuffersPerGate = extraNetworkBuffersPerGate;
	}

	// --------------------------------------------------------------------------------------------
	//  Properties
	// --------------------------------------------------------------------------------------------

	public ResultPartitionManager getResultPartitionManager() {
		return resultPartitionManager;
	}

	public TaskEventDispatcher getTaskEventDispatcher() {
		return taskEventDispatcher;
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public NetworkBufferPool getNetworkBufferPool() {
		return networkBufferPool;
	}

	public IOMode getDefaultIOMode() {
		return defaultIOMode;
	}

	public int getPartitionRequestInitialBackoff() {
		return partitionRequestInitialBackoff;
	}

	public int getPartitionRequestMaxBackoff() {
		return partitionRequestMaxBackoff;
	}

	public KvStateRegistry getKvStateRegistry() {
		return kvStateRegistry;
	}

	public KvStateServer getKvStateServer() {
		return kvStateServer;
	}

	public KvStateClientProxy getKvStateProxy() {
		return kvStateProxy;
	}

	public TaskKvStateRegistry createKvStateTaskRegistry(JobID jobId, JobVertexID jobVertexId) {
		return kvStateRegistry.createTaskRegistry(jobId, jobVertexId);
	}

	// --------------------------------------------------------------------------------------------
	//  Task operations
	// --------------------------------------------------------------------------------------------

	public void registerTask(Task task) throws IOException {
		final ResultPartition[] producedPartitions = task.getProducedPartitions();
		final ResultPartitionWriter[] writers = task.getAllWriters();

		if (writers.length != producedPartitions.length) {
			throw new IllegalStateException("Unequal number of writers and partitions.");
		}

		synchronized (lock) {
			if (isShutdown) {
				throw new IllegalStateException("NetworkEnvironment is shut down");
			}

			for (int i = 0; i < producedPartitions.length; i++) {
				final ResultPartition partition = producedPartitions[i];
				final ResultPartitionWriter writer = writers[i];

				// Buffer pool for the partition
				BufferPool bufferPool = null;

				try {
					int maxNumberOfMemorySegments = partition.getPartitionType().isBounded() ?
						partition.getNumberOfSubpartitions() * networkBuffersPerChannel +
							extraNetworkBuffersPerGate : Integer.MAX_VALUE;
					bufferPool = networkBufferPool.createBufferPool(partition.getNumberOfSubpartitions(),
						maxNumberOfMemorySegments);
					partition.registerBufferPool(bufferPool);

					resultPartitionManager.registerResultPartition(partition);
				} catch (Throwable t) {
					if (bufferPool != null) {
						bufferPool.lazyDestroy();
					}

					if (t instanceof IOException) {
						throw (IOException) t;
					} else {
						throw new IOException(t.getMessage(), t);
					}
				}

				// Register writer with task event dispatcher
				taskEventDispatcher.registerWriterForIncomingTaskEvents(writer.getPartitionId(), writer);
			}

			// Setup the buffer pool for each buffer reader
			final SingleInputGate[] inputGates = task.getAllInputGates();

			for (SingleInputGate gate : inputGates) {
				BufferPool bufferPool = null;

				try {
					if (gate.getConsumedPartitionType().isCreditBased()) {
						// Create a fixed-size buffer pool for floating buffers and assign exclusive buffers to input channels directly
						bufferPool = networkBufferPool.createBufferPool(extraNetworkBuffersPerGate, extraNetworkBuffersPerGate);
						gate.assignExclusiveSegments(networkBufferPool, networkBuffersPerChannel);
					} else {
						int maxNumberOfMemorySegments = gate.getConsumedPartitionType().isBounded() ?
							gate.getNumberOfInputChannels() * networkBuffersPerChannel +
								extraNetworkBuffersPerGate : Integer.MAX_VALUE;
						bufferPool = networkBufferPool.createBufferPool(gate.getNumberOfInputChannels(),
							maxNumberOfMemorySegments);
					}
					gate.setBufferPool(bufferPool);
				} catch (Throwable t) {
					if (bufferPool != null) {
						bufferPool.lazyDestroy();
					}

					ExceptionUtils.rethrowIOException(t);
				}
			}
		}
	}

	public void unregisterTask(Task task) {
		LOG.debug("Unregister task {} from network environment (state: {}).",
				task.getTaskInfo().getTaskNameWithSubtasks(), task.getExecutionState());

		final ExecutionAttemptID executionId = task.getExecutionId();

		synchronized (lock) {
			if (isShutdown) {
				// no need to do anything when we are not operational
				return;
			}

			if (task.isCanceledOrFailed()) {
				resultPartitionManager.releasePartitionsProducedBy(executionId, task.getFailureCause());
			}

			ResultPartitionWriter[] writers = task.getAllWriters();
			if (writers != null) {
				for (ResultPartitionWriter writer : writers) {
					taskEventDispatcher.unregisterWriter(writer);
				}
			}

			ResultPartition[] partitions = task.getProducedPartitions();
			if (partitions != null) {
				for (ResultPartition partition : partitions) {
					partition.destroyBufferPool();
				}
			}

			final SingleInputGate[] inputGates = task.getAllInputGates();

			if (inputGates != null) {
				for (SingleInputGate gate : inputGates) {
					try {
						if (gate != null) {
							gate.releaseAllResources();
						}
					}
					catch (IOException e) {
						LOG.error("Error during release of reader resources: " + e.getMessage(), e);
					}
				}
			}
		}
	}

	public void start() throws IOException {
		synchronized (lock) {
			Preconditions.checkState(!isShutdown, "The NetworkEnvironment has already been shut down.");

			LOG.info("Starting the network environment and its components.");

			try {
				LOG.debug("Starting network connection manager");
				connectionManager.start(resultPartitionManager, taskEventDispatcher);
			} catch (IOException t) {
				throw new IOException("Failed to instantiate network connection manager.", t);
			}

			if (kvStateServer != null) {
				try {
					kvStateServer.start();
				} catch (Throwable ie) {
					kvStateServer.shutdown();
					kvStateServer = null;
					throw new IOException("Failed to start the Queryable State Data Server.", ie);
				}
			}

			if (kvStateProxy != null) {
				try {
					kvStateProxy.start();
				} catch (Throwable ie) {
					kvStateProxy.shutdown();
					kvStateProxy = null;
					throw new IOException("Failed to start the Queryable State Client Proxy.", ie);
				}
			}
		}
	}

	/**
	 * Tries to shut down all network I/O components.
	 */
	public void shutdown() {
		synchronized (lock) {
			if (isShutdown) {
				return;
			}

			LOG.info("Shutting down the network environment and its components.");

			if (kvStateProxy != null) {
				try {
					LOG.debug("Shutting down Queryable State Client Proxy.");
					kvStateProxy.shutdown();
				} catch (Throwable t) {
					LOG.warn("Cannot shut down Queryable State Client Proxy.", t);
				}
			}

			if (kvStateServer != null) {
				try {
					LOG.debug("Shutting down Queryable State Data Server.");
					kvStateServer.shutdown();
				} catch (Throwable t) {
					LOG.warn("Cannot shut down Queryable State Data Server.", t);
				}
			}

			// terminate all network connections
			try {
				LOG.debug("Shutting down network connection manager");
				connectionManager.shutdown();
			}
			catch (Throwable t) {
				LOG.warn("Cannot shut down the network connection manager.", t);
			}

			// shutdown all intermediate results
			try {
				LOG.debug("Shutting down intermediate result partition manager");
				resultPartitionManager.shutdown();
			}
			catch (Throwable t) {
				LOG.warn("Cannot shut down the result partition manager.", t);
			}

			taskEventDispatcher.clearAll();

			// make sure that the global buffer pool re-acquires all buffers
			networkBufferPool.destroyAllBufferPools();

			// destroy the buffer pool
			try {
				networkBufferPool.destroy();
			}
			catch (Throwable t) {
				LOG.warn("Network buffer pool did not shut down properly.", t);
			}

			isShutdown = true;
		}
	}

	public boolean isShutdown() {
		synchronized (lock) {
			return isShutdown;
		}
	}
}
