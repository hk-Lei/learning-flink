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

import java.util.Map;

/**
 * A MetricGroup is a named container for {@link Metric Metrics} and further metric subgroups.
 * MetricGroup 是 Flink 指标及指标子分组的容器
 *
 * <p>Instances of this class can be used to register new metrics with Flink and to create a nested
 * hierarchy based on the group names.
 * 该类的实例可用于用 Flink 注册新指标，并基于组名称创建嵌套层次结构。
 *
 * <p>A MetricGroup is uniquely identified by it's place in the hierarchy and name.
 * 一个 MetricGroup 在其层次结构的位置和名称中的是被唯一标识的。
 */
public interface MetricGroup {

	// ------------------------------------------------------------------------
	//  Metrics
	// ------------------------------------------------------------------------

	/**
	 * Creates and registers a new {@link org.apache.flink.metrics.Counter} with Flink.
	 *
	 * @param name name of the counter
	 * @return the created counter
	 */
	Counter counter(int name);

	/**
	 * Creates and registers a new {@link org.apache.flink.metrics.Counter} with Flink.
	 *
	 * @param name name of the counter
	 * @return the created counter
	 */
	Counter counter(String name);

	/**
	 * Registers a {@link org.apache.flink.metrics.Counter} with Flink.
	 *
	 * @param name    name of the counter
	 * @param counter counter to register
	 * @param <C>     counter type
	 * @return the given counter
	 */
	<C extends Counter> C counter(int name, C counter);

	/**
	 * Registers a {@link org.apache.flink.metrics.Counter} with Flink.
	 *
	 * @param name    name of the counter
	 * @param counter counter to register
	 * @param <C>     counter type
	 * @return the given counter
	 */
	<C extends Counter> C counter(String name, C counter);

	/**
	 * Registers a new {@link org.apache.flink.metrics.Gauge} with Flink.
	 *
	 * @param name  name of the gauge
	 * @param gauge gauge to register
	 * @param <T>   return type of the gauge
	 * @return the given gauge
	 */
	<T, G extends Gauge<T>> G gauge(int name, G gauge);

	/**
	 * Registers a new {@link org.apache.flink.metrics.Gauge} with Flink.
	 *
	 * @param name  name of the gauge
	 * @param gauge gauge to register
	 * @param <T>   return type of the gauge
	 * @return the given gauge
	 */
	<T, G extends Gauge<T>> G gauge(String name, G gauge);

	/**
	 * Registers a new {@link Histogram} with Flink.
	 *
	 * @param name name of the histogram
	 * @param histogram histogram to register
	 * @param <H> histogram type
	 * @return the registered histogram
	 */
	<H extends Histogram> H histogram(String name, H histogram);

	/**
	 * Registers a new {@link Histogram} with Flink.
	 *
	 * @param name name of the histogram
	 * @param histogram histogram to register
	 * @param <H> histogram type
	 * @return the registered histogram
	 */
	<H extends Histogram> H histogram(int name, H histogram);

	/**
	 * Registers a new {@link Meter} with Flink.
	 *
	 * @param name name of the meter
	 * @param meter meter to register
	 * @param <M> meter type
	 * @return the registered meter
	 */
	<M extends Meter> M meter(String name, M meter);

	/**
	 * Registers a new {@link Meter} with Flink.
	 *
	 * @param name name of the meter
	 * @param meter meter to register
	 * @param <M> meter type
	 * @return the registered meter
	 */
	<M extends Meter> M meter(int name, M meter);

	// ------------------------------------------------------------------------
	// Groups
	// ------------------------------------------------------------------------

	/**
	 * Creates a new MetricGroup and adds it to this groups sub-groups.
	 * 创建一个新的 MetricGroup 并将其添加到这个组的子组中。
	 *
	 * @param name name of the group
	 * @return the created group
	 */
	MetricGroup addGroup(int name);

	/**
	 * Creates a new MetricGroup and adds it to this groups sub-groups.
	 *
	 * @param name name of the group
	 * @return the created group
	 */
	MetricGroup addGroup(String name);

	// ------------------------------------------------------------------------
	// Scope
	// ------------------------------------------------------------------------

	/**
	 * Gets the scope as an array of the scope components, for example
	 * {@code ["host-7", "taskmanager-2", "window_word_count", "my-mapper"]}.
	 * 获取作为范围组件的范围数组，例如 {@code ["host-7", "taskmanager-2", "window_word_count", "my-mapper"]}
	 *
	 * @see #getMetricIdentifier(String)
	 * @see #getMetricIdentifier(String, CharacterFilter)
	 */
	String[] getScopeComponents();

	/**
	 * Returns a map of all variables and their associated value, for example
	 * {@code {"<host>"="host-7", "<tm_id>"="taskmanager-2"}}.
	 *
	 * @return map of all variables and their associated value
     */
	Map<String, String> getAllVariables();

	/**
	 * Returns the fully qualified metric name, for example
	 * {@code "host-7.taskmanager-2.window_word_count.my-mapper.metricName"}.
	 *
	 * @param metricName metric name
	 * @return fully qualified metric name
	 */
	String getMetricIdentifier(String metricName);

	/**
	 * Returns the fully qualified metric name, for example
	 * {@code "host-7.taskmanager-2.window_word_count.my-mapper.metricName"}.
	 *
	 * @param metricName metric name
	 * @param filter character filter which is applied to the scope components if not null.
	 * @return fully qualified metric name
	 */
	String getMetricIdentifier(String metricName, CharacterFilter filter);
}
