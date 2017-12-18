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

package org.apache.flink.streaming.runtime.streamstatus;

import org.apache.flink.annotation.Internal;

/**
 * Interface that allows toggling the current {@link StreamStatus} as well as retrieving it.
 *
 * 允许切换以及检索当前 StreamStatus 的接口。
 */
@Internal
public interface StreamStatusMaintainer extends StreamStatusProvider {

	/**
	 * Toggles the current stream status. This method should only have effect
	 * if the supplied stream status is different from the current status.
	 *
	 * 切换当前流状态。只有当提供的流状态与当前状态不同时，此方法才会起作用。
	 *
	 * @param streamStatus the new status to toggle to
	 */
	void toggleStreamStatus(StreamStatus streamStatus);

}
