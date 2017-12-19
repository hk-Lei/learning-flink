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

package org.apache.flink.runtime.heartbeat;

import org.apache.flink.runtime.clusterframework.types.ResourceID;

/**
 * A heartbeat manager has to be able to start/stop monitoring a {@link HeartbeatTarget}, and report heartbeat timeouts
 * for this target.
 *
 * heartbeat 管理器必须能够 启动/停止 监控 HeartbeatTarget，并报告心跳超时时间。
 *
 * @param <I> Type of the incoming payload
 * @param <O> Type of the outgoing payload
 */
public interface HeartbeatManager<I, O> extends HeartbeatTarget<I> {

	/**
	 * Start monitoring a {@link HeartbeatTarget}. Heartbeat timeouts for this target are reported
	 * to the {@link HeartbeatListener} associated with this heartbeat manager.
	 *
	 * 开始监控 HeartbeatTarget。此目标的心跳超时被报告给与此 Heartbeat 管理器关联的 HeartbeatListener。
	 *
	 * @param resourceID Resource ID identifying the heartbeat target
	 * @param heartbeatTarget Interface to send heartbeat requests and responses to the heartbeat
	 *                        target
	 */
	void monitorTarget(ResourceID resourceID, HeartbeatTarget<O> heartbeatTarget);

	/**
	 * Stops monitoring the heartbeat target with the associated resource ID.
	 *
	 * 停止监控关联资源 ID 的心跳目标。
	 *
	 * @param resourceID Resource ID of the heartbeat target which shall no longer be monitored
	 */
	void unmonitorTarget(ResourceID resourceID);

	/**
	 * Stops the heartbeat manager.
	 *
	 * 停止 heartbeat 管理器
	 */
	void stop();

	/**
	 * Returns the last received heartbeat from the given target.
	 *
	 * 返回给定目标的最后一个接收到的心跳。
	 *
	 * @param resourceId for which to return the last heartbeat
	 * @return Last heartbeat received from the given target or -1 if the target is not being monitored.
	 */
	long getLastHeartbeatFrom(ResourceID resourceId);
}
