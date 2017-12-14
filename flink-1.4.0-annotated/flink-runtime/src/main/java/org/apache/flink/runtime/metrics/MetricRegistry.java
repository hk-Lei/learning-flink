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

package org.apache.flink.runtime.metrics;

import org.apache.flink.metrics.Metric;
import org.apache.flink.runtime.metrics.dump.MetricQueryService;
import org.apache.flink.runtime.metrics.groups.AbstractMetricGroup;
import org.apache.flink.runtime.metrics.scope.ScopeFormats;

import javax.annotation.Nullable;

/**
 * Interface for a metric registry.
 * metrics 注册表接口
 */
public interface MetricRegistry {

	/**
	 * Returns the global delimiter.
	 * 返回全局分隔符
	 *
	 * @return global delimiter
	 */
	char getDelimiter();

	/**
	 * Returns the configured delimiter for the reporter with the given index.
	 * 返回指定索引位置的已配置的分隔符。
	 *
	 * @param index index of the reporter whose delimiter should be used
	 * @return configured reporter delimiter, or global delimiter if index is invalid
	 */
	char getDelimiter(int index);

	/**
	 * Returns the number of registered reporters.
	 * 返回注册的 reporters 数
	 */
	int getNumberReporters();

	/**
	 * Registers a new {@link Metric} with this registry.
	 * 注册一个新的 Metric
	 *
	 * @param metric      the metric that was added
	 * @param metricName  the name of the metric
	 * @param group       the group that contains the metric
	 */
	void register(Metric metric, String metricName, AbstractMetricGroup group);

	/**
	 * Un-registers the given {@link Metric} with this registry.
	 * 解除给的的 metric 的注册信息
	 *
	 * @param metric      the metric that should be removed
	 * @param metricName  the name of the metric
	 * @param group       the group that contains the metric
	 */
	void unregister(Metric metric, String metricName, AbstractMetricGroup group);

	/**
	 * Returns the scope formats.
	 * 返回格式化的域
	 *
	 * @return scope formats
	 */
	ScopeFormats getScopeFormats();

	/**
	 * Returns the path of the {@link MetricQueryService} or null, if none is started.
	 * 返回 MetricQueryService 的路径，如果服务没有启动返回 null
	 *
	 * @return Path of the MetricQueryService or null, if none is started
	 */
	@Nullable
	String getMetricQueryServicePath();
}
