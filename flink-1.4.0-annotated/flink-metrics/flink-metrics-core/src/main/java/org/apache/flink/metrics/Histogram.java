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

package org.apache.flink.metrics;

/**
 * Histogram interface to be used with Flink's metrics system.
 * Flink的 Metrics 系统的直方图接口
 *
 * <p>The histogram allows to record values, get the current count of recorded values and create
 * histogram statistics for the currently seen elements.
 * 直方图允许记录值，获取当前记录值的当前计数，并为当前所看到的元素创建直方图统计数据。
 */
public interface Histogram extends Metric {

	/**
	 * Update the histogram with the given value.
	 * 用给定值更新直方图。
	 *
	 * @param value Value to update the histogram with
	 */
	void update(long value);

	/**
	 * Get the count of seen elements.
	 * 获取所见元素的计数。
	 *
	 * @return Count of seen elements
	 */
	long getCount();

	/**
	 * Create statistics for the currently recorded elements.
	 * 为当前记录的元素创建统计信息。
	 *
	 * @return Statistics about the currently recorded elements
	 */
	HistogramStatistics getStatistics();
}
