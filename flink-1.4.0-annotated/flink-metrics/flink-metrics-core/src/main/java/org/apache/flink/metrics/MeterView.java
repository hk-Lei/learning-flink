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
 * A MeterView provides an average rate of events per second over a given time period.
 * MeterView提供了给定时间段内每秒事件的平均速率。
 *
 * <p>The primary advantage of this class is that the rate is neither updated by the computing thread nor for every event.
 * Instead, a history of counts is maintained that is updated in regular intervals by a background thread. From this
 * history a rate is derived on demand, which represents the average rate of events over the given time span.
 * 这个类的主要优点是，该速率既不是由计算线程更新的，也不是每一个事件都更新的。
 * 相反，是一个后台线程通过维护计数的历史记录定期更新的。从这段历史中，可以得出一个速率，它表示给定时间跨度内事件的平均速率。
 *
 * <p>Setting the time span to a low value reduces memory-consumption and will more accurately report short-term changes.
 * The minimum value possible is {@link View#UPDATE_INTERVAL_SECONDS}.
 * A high value in turn increases memory-consumption, since a longer history has to be maintained, but will result in
 * smoother transitions between rates.
 * 将时间跨度设置得比较小可以减少内存消耗，并能更准确地报告短期变化，其最小时间跨度可能是 5 秒（View.UPDATE_INTERVAL_SECONDS）。
 * 相反的，大的时间跨度会增加内存消耗，因为要保持较长的历史，但这样会导致速率间的波动更为平稳
 *
 * <p>The events are counted by a {@link Counter}.
 */
public class MeterView implements Meter, View {
	/** The underlying counter maintaining the count. 维护统计值的内部计数器 */
	private final Counter counter;
	/** The time-span over which the average is calculated. 计算平均值的时间跨度 */
	private final int timeSpanInSeconds;
	/** Circular array containing the history of values. 包含值历史的循环数组 */
	private final long[] values;
	/** The index in the array for the current time. 数组中当前时间的索引 */
	private int time = 0;
	/** The last rate we computed. 我们计算的一个最近速率。*/
	private double currentRate = 0;

	public MeterView(int timeSpanInSeconds) {
		this(new SimpleCounter(), timeSpanInSeconds);
	}

	public MeterView(Counter counter, int timeSpanInSeconds) {
		this.counter = counter;
		this.timeSpanInSeconds = timeSpanInSeconds - (timeSpanInSeconds % UPDATE_INTERVAL_SECONDS);
		this.values = new long[this.timeSpanInSeconds / UPDATE_INTERVAL_SECONDS + 1];
	}

	@Override
	public void markEvent() {
		this.counter.inc();
	}

	@Override
	public void markEvent(long n) {
		this.counter.inc(n);
	}

	@Override
	public long getCount() {
		return counter.getCount();
	}

	@Override
	public double getRate() {
		return currentRate;
	}

	@Override
	public void update() {
		time = (time + 1) % values.length;
		values[time] = counter.getCount();
		currentRate =  ((double) (values[time] - values[(time + 1) % values.length]) / timeSpanInSeconds);
	}
}
