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

package org.apache.flink.metrics.reporter;

import org.apache.flink.metrics.Metric;
import org.apache.flink.metrics.MetricConfig;
import org.apache.flink.metrics.MetricGroup;

/**
 * Reporters are used to export {@link Metric Metrics} to an external backend.
 * Reporters 被用来输出度量指标到外部端
 *
 * <p>Reporters are instantiated via reflection and must be public, non-abstract, and have a
 * public no-argument constructor.
 * <p>Reporters 通过反射进行实例化，必须是公开的、非抽象的，并且有一个公共的无参构造函数。
 */
public interface MetricReporter {

	// ------------------------------------------------------------------------
	//  life cycle 生命周期
	// ------------------------------------------------------------------------

	/**
	 * Configures this reporter. Since reporters are instantiated generically and hence parameter-less,
	 * this method is the place where the reporters set their basic fields based on configuration values.
	 * 配置这个 Reporter。由于 Reporter 是被实例化而非参数化的，所以这个方法是 Reporter 根据配置值设置基本属性的地方。
	 *
	 * <p>This method is always called first on a newly instantiated reporter.
	 *
	 * @param config A properties object that contains all parameters set for this reporter.
	 */
	void open(MetricConfig config);

	/**
	 * Closes this reporter. Should be used to close channels, streams and release resources.
	 * 关闭这个 Reporter。应该用于关闭通道、流和释放资源。
	 */
	void close();

	// ------------------------------------------------------------------------
	//  adding / removing metrics 添加、删除指标
	// ------------------------------------------------------------------------

	/**
	 * Called when a new {@link Metric} was added.
	 * 当添加一个新的指标时调用。
	 *
	 * @param metric      the metric that was added
	 * @param metricName  the name of the metric
	 * @param group       the group that contains the metric
	 */
	void notifyOfAddedMetric(Metric metric, String metricName, MetricGroup group);

	/**
	 * Called when a {@link Metric} was should be removed.
	 * 当应该删除一个指标时调用。
	 *
	 * @param metric      the metric that should be removed
	 * @param metricName  the name of the metric
	 * @param group       the group that contains the metric
	 */
	void notifyOfRemovedMetric(Metric metric, String metricName, MetricGroup group);
}
