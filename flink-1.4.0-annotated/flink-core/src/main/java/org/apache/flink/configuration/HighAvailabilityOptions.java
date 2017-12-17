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

package org.apache.flink.configuration;

import org.apache.flink.annotation.PublicEvolving;

import static org.apache.flink.configuration.ConfigOptions.key;

/**
 * The set of configuration options relating to high-availability settings.
 * 与高可用性设置相关的配置项。
 */
@PublicEvolving
public class HighAvailabilityOptions {

	// ------------------------------------------------------------------------
	//  Required High Availability Options
	// ------------------------------------------------------------------------

	/** 
	 * Defines high-availability mode used for the cluster execution.
	 * A value of "NONE" signals no highly available setup.
	 * To enable high-availability, set this mode to "ZOOKEEPER".
	 *
	 * 定义用于集群执行的高可用性模式。
	 *   “NONE” 表示没有启用高可用模式。
	 *   “Zookeeper” 表示要启用高可用性。
	 */
	public static final ConfigOption<String> HA_MODE = 
			key("high-availability")
			.defaultValue("NONE")
			.withDeprecatedKeys("recovery.mode");

	/**
	 * The ID of the Flink cluster, used to separate multiple Flink clusters 
	 * Needs to be set for standalone clusters, is automatically inferred in YARN and Mesos.
	 *
	 * Flink 集群的 ID，用于分离多个Flink集群
	 * Standalone 集群需要设置（默认 /default），Yarn 和 Mesos 集群会自动推断的
	 */
	public static final ConfigOption<String> HA_CLUSTER_ID = 
			key("high-availability.cluster-id")
			.defaultValue("/default")
			.withDeprecatedKeys("high-availability.zookeeper.path.namespace", "recovery.zookeeper.path.namespace");

	/**
	 * File system path (URI) where Flink persists metadata in high-availability setups
	 *
	 * Flink 在高可用模式下持久化元数据的文件系统路径(URI)
	 */
	public static final ConfigOption<String> HA_STORAGE_PATH =
			key("high-availability.storageDir")
			.noDefaultValue()
			.withDeprecatedKeys("high-availability.zookeeper.storageDir", "recovery.zookeeper.storageDir");
	

	// ------------------------------------------------------------------------
	//  Recovery Options
	//  恢复配置项
	// ------------------------------------------------------------------------

	/**
	 * Optional port (range) used by the job manager in high-availability mode.
	 *
	 * JobManager 在高可用性模式下使用的可选端口(范围)。
	 */
	public static final ConfigOption<String> HA_JOB_MANAGER_PORT_RANGE = 
			key("high-availability.jobmanager.port")
			.defaultValue("0")
			.withDeprecatedKeys("recovery.jobmanager.port");

	/**
	 * The time before a JobManager after a fail over recovers the current jobs.
	 *
	 * 在失败后， JobManager 恢复当前的 jobs 的延迟。
	 */
	public static final ConfigOption<String> HA_JOB_DELAY = 
			key("high-availability.job.delay")
			.noDefaultValue()
			.withDeprecatedKeys("recovery.job.delay");

	// ------------------------------------------------------------------------
	//  ZooKeeper Options
	// ------------------------------------------------------------------------

	/**
	 * The ZooKeeper quorum to use, when running Flink in a high-availability mode with ZooKeeper.
	 */
	public static final ConfigOption<String> HA_ZOOKEEPER_QUORUM =
			key("high-availability.zookeeper.quorum")
			.noDefaultValue()
			.withDeprecatedKeys("recovery.zookeeper.quorum");

	/**
	 * The root path under which Flink stores its entries in ZooKeeper
	 *
	 * Flink 在 Zookeeper 中存储的根路径，默认 /flink
	 */
	public static final ConfigOption<String> HA_ZOOKEEPER_ROOT =
			key("high-availability.zookeeper.path.root")
			.defaultValue("/flink")
			.withDeprecatedKeys("recovery.zookeeper.path.root");

	// Flink job 在 Zookeeper 中的命名空间 默认 “/default_ns”
	public static final ConfigOption<String> HA_ZOOKEEPER_NAMESPACE =
			key("high-availability.zookeeper.path.namespace")
			.noDefaultValue()
			.withDeprecatedKeys("recovery.zookeeper.path.namespace");

	// 用于选举 leader 锁的 znode 默认 /leaderlatch
	public static final ConfigOption<String> HA_ZOOKEEPER_LATCH_PATH =
			key("high-availability.zookeeper.path.latch")
			.defaultValue("/leaderlatch")
			.withDeprecatedKeys("recovery.zookeeper.path.latch");

	/** ZooKeeper root path (ZNode) for job graphs. */
	// Job graphs 存放的根路径，默认 /jobgraphs
	public static final ConfigOption<String> HA_ZOOKEEPER_JOBGRAPHS_PATH =
			key("high-availability.zookeeper.path.jobgraphs")
			.defaultValue("/jobgraphs")
			.withDeprecatedKeys("recovery.zookeeper.path.jobgraphs");

	// 定义 leader 的znode （默认 /leader），其中包含指向 leader 的 URL 和当前的 leader 的会话ID
	public static final ConfigOption<String> HA_ZOOKEEPER_LEADER_PATH =
			key("high-availability.zookeeper.path.leader")
			.defaultValue("/leader")
			.withDeprecatedKeys("recovery.zookeeper.path.leader");

	/** ZooKeeper root path (ZNode) for completed checkpoints. */
	// 用于存储完成的 checkpoints 的znode 根路径，默认 /checkpoints
	public static final ConfigOption<String> HA_ZOOKEEPER_CHECKPOINTS_PATH =
			key("high-availability.zookeeper.path.checkpoints")
			.defaultValue("/checkpoints")
			.withDeprecatedKeys("recovery.zookeeper.path.checkpoints");

	/** ZooKeeper root path (ZNode) for checkpoint counters. */
	// 用于 checkpoint 计数器的 znode 默认 /checkpoint-counter
	public static final ConfigOption<String> HA_ZOOKEEPER_CHECKPOINT_COUNTER_PATH =
			key("high-availability.zookeeper.path.checkpoint-counter")
			.defaultValue("/checkpoint-counter")
			.withDeprecatedKeys("recovery.zookeeper.path.checkpoint-counter");

	/** ZooKeeper root path (ZNode) for Mesos workers. */
	// Mesos workers 的根路径 默认 /mesos-workers
	@PublicEvolving
	public static final ConfigOption<String> HA_ZOOKEEPER_MESOS_WORKERS_PATH =
			key("high-availability.zookeeper.path.mesos-workers")
			.defaultValue("/mesos-workers")
			.withDeprecatedKeys("recovery.zookeeper.path.mesos-workers");

	// ------------------------------------------------------------------------
	//  ZooKeeper Client Settings
	//  Zookeeper 客户端设置
	// ------------------------------------------------------------------------

	// 默认会话超时时间 60s
	public static final ConfigOption<Integer> ZOOKEEPER_SESSION_TIMEOUT = 
			key("high-availability.zookeeper.client.session-timeout")
			.defaultValue(60000)
			.withDeprecatedKeys("recovery.zookeeper.client.session-timeout");

	// 默认连接超时时间 15s
	public static final ConfigOption<Integer> ZOOKEEPER_CONNECTION_TIMEOUT =
			key("high-availability.zookeeper.client.connection-timeout")
			.defaultValue(15000)
			.withDeprecatedKeys("recovery.zookeeper.client.connection-timeout");

	// 默认重试等待时长 5s
	public static final ConfigOption<Integer> ZOOKEEPER_RETRY_WAIT = 
			key("high-availability.zookeeper.client.retry-wait")
			.defaultValue(5000)
			.withDeprecatedKeys("recovery.zookeeper.client.retry-wait");

	// 默认最大重试次数 3次
	public static final ConfigOption<Integer> ZOOKEEPER_MAX_RETRY_ATTEMPTS = 
			key("high-availability.zookeeper.client.max-retry-attempts")
			.defaultValue(3)
			.withDeprecatedKeys("recovery.zookeeper.client.max-retry-attempts");

	// Zookeeper 中 Job 的默认注册路径 /running_job_registry/
	public static final ConfigOption<String> ZOOKEEPER_RUNNING_JOB_REGISTRY_PATH = 
			key("high-availability.zookeeper.path.running-registry")
			.defaultValue("/running_job_registry/");

	// 客户端默认 ACL 权限：打开
	public static final ConfigOption<String> ZOOKEEPER_CLIENT_ACL =
			key("high-availability.zookeeper.client.acl")
			.defaultValue("open");

	// ------------------------------------------------------------------------

	/** Not intended to be instantiated */
	private HighAvailabilityOptions() {}
}
