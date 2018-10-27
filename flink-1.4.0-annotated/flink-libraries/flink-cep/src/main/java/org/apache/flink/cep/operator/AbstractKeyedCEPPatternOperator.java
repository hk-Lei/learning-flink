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

package org.apache.flink.cep.operator;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.functions.Function;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.ListSerializer;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.cep.EventComparator;
import org.apache.flink.cep.nfa.AfterMatchSkipStrategy;
import org.apache.flink.cep.nfa.NFA;
import org.apache.flink.cep.nfa.compiler.NFACompiler;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.apache.flink.streaming.api.operators.AbstractUdfStreamOperator;
import org.apache.flink.streaming.api.operators.InternalTimer;
import org.apache.flink.streaming.api.operators.InternalTimerService;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.Triggerable;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Abstract CEP pattern operator for a keyed input stream. For each key, the operator creates
 * a {@link NFA} and a priority queue to buffer out of order elements. Both data structures are
 * stored using the managed keyed state. Additionally, the set of all seen keys is kept as part of the
 * operator state. This is necessary to trigger the execution for all keys upon receiving a new
 * watermark.
 *
 * keyed 输入流的抽象 CEP 模式操作符，对于每一个 Key 创建一个 NFA 和一个优先队列来缓存乱序的消息。
 * 这两个数据结构都依托于其管理的 keyed state，此外，所有键的集合都是作为操作符状态的一部分保存的。
 * 这是在接收到新 watermark 时触发所有键执行的必要条件。
 *
 *
 * @param <IN> Type of the input elements
 * @param <KEY> Type of the key on which the input stream is keyed
 * @param <OUT> Type of the output elements
 */
public abstract class AbstractKeyedCEPPatternOperator<IN, KEY, OUT, F extends Function>
	extends AbstractUdfStreamOperator<OUT, F>
	implements OneInputStreamOperator<IN, OUT>, Triggerable<KEY, VoidNamespace> {

	private static final long serialVersionUID = -4166778210774160757L;

	private final boolean isProcessingTime;

	private final TypeSerializer<IN> inputSerializer;

	///////////////			State			//////////////

	private static final String NFA_OPERATOR_STATE_NAME = "nfaOperatorStateName";
	private static final String EVENT_QUEUE_STATE_NAME = "eventQueuesStateName";

	private transient ValueState<NFA<IN>> nfaOperatorState;
	private transient MapState<Long, List<IN>> elementQueueState;

	private final NFACompiler.NFAFactory<IN> nfaFactory;

	private transient InternalTimerService<VoidNamespace> timerService;

	/**
	 * The last seen watermark. This will be used to
	 * decide if an incoming element is late or not.
	 */
	private long lastWatermark;

	private final EventComparator<IN> comparator;

	protected final AfterMatchSkipStrategy afterMatchSkipStrategy;

	public AbstractKeyedCEPPatternOperator(
			final TypeSerializer<IN> inputSerializer,
			final boolean isProcessingTime,
			final NFACompiler.NFAFactory<IN> nfaFactory,
			final EventComparator<IN> comparator,
			final AfterMatchSkipStrategy afterMatchSkipStrategy,
			final F function) {
		super(function);

		this.inputSerializer = Preconditions.checkNotNull(inputSerializer);
		this.isProcessingTime = Preconditions.checkNotNull(isProcessingTime);
		this.nfaFactory = Preconditions.checkNotNull(nfaFactory);
		this.comparator = comparator;

		if (afterMatchSkipStrategy == null) {
			this.afterMatchSkipStrategy = AfterMatchSkipStrategy.noSkip();
		} else {
			this.afterMatchSkipStrategy = afterMatchSkipStrategy;
		}
	}

	@Override
	public void initializeState(StateInitializationContext context) throws Exception {
		super.initializeState(context);

		if (nfaOperatorState == null) {
			nfaOperatorState = getRuntimeContext().getState(
				new ValueStateDescriptor<>(
						NFA_OPERATOR_STATE_NAME,
						new NFA.NFASerializer<>(inputSerializer)));
		}

		if (elementQueueState == null) {
			elementQueueState = getRuntimeContext().getMapState(
					new MapStateDescriptor<>(
							EVENT_QUEUE_STATE_NAME,
							LongSerializer.INSTANCE,
							new ListSerializer<>(inputSerializer)
					)
			);
		}
	}

	@Override
	public void open() throws Exception {
		super.open();

		timerService = getInternalTimerService(
				"watermark-callbacks",
				VoidNamespaceSerializer.INSTANCE,
				this);
	}

	@Override
	public void processElement(StreamRecord<IN> element) throws Exception {
		if (isProcessingTime) {
			if (comparator == null) {
				// there can be no out of order elements in processing time
				// 处于 processing 时间模式下不存在元素乱序的情况
				NFA<IN> nfa = getNFA();
				processEvent(nfa, element.getValue(), getProcessingTimeService().getCurrentProcessingTime());
				updateNFA(nfa);
			} else {
				long currentTime = timerService.currentProcessingTime();
				bufferEvent(element.getValue(), currentTime);

				// register a timer for the next millisecond to sort and emit buffered data
				// 为下一毫秒注册一个 timer ，以排序和释放缓冲数据
				timerService.registerProcessingTimeTimer(VoidNamespace.INSTANCE, currentTime + 1);
			}

		} else {

			long timestamp = element.getTimestamp();
			IN value = element.getValue();

			// In event-time processing we assume correctness of the watermark.
			// Events with timestamp smaller than or equal with the last seen watermark are considered late.
			// Late events are put in a dedicated side output, if the user has specified one.

			// 在 Event-time 处理中，我们假定 watermark 是正确的。
			// 时间戳小于或等于最后一个可见 watermark 的事件被认为是迟到的。
			// 如果用户指定了一个输出，迟到的事件会被放在一个专用的输出端。
			if (timestamp > lastWatermark) {

				// we have an event with a valid timestamp, so
				// we buffer it until we receive the proper watermark.

				//我们有一个具有有效时间戳的事件，因此我们缓冲它直到收到适当的 watermark。

				saveRegisterWatermarkTimer();

				bufferEvent(value, timestamp);
			}
		}
	}

	/**
	 * Registers a timer for {@code current watermark + 1}, this means that we get triggered
	 * whenever the watermark advances, which is what we want for working off the queue of
	 * buffered elements.
	 *
	 * 为当前 watermark + 1 注册一个 timer，意味着每当有 watermark 的时候，我们就触发处理缓存元素的队列的操作
	 */
	private void saveRegisterWatermarkTimer() {
		long currentWatermark = timerService.currentWatermark();
		// protect against overflow
		if (currentWatermark + 1 > currentWatermark) {
			timerService.registerEventTimeTimer(VoidNamespace.INSTANCE, currentWatermark + 1);
		}
	}

	private void bufferEvent(IN event, long currentTime) throws Exception {
		List<IN> elementsForTimestamp =  elementQueueState.get(currentTime);
		if (elementsForTimestamp == null) {
			elementsForTimestamp = new ArrayList<>();
		}

		if (getExecutionConfig().isObjectReuseEnabled()) {
			// copy the StreamRecord so that it cannot be changed
			// 复制流记录，这样它就不能被更改
			elementsForTimestamp.add(inputSerializer.copy(event));
		} else {
			elementsForTimestamp.add(event);
		}
		elementQueueState.put(currentTime, elementsForTimestamp);
	}

	@Override
	public void onEventTime(InternalTimer<KEY, VoidNamespace> timer) throws Exception {

		// 1) get the queue of pending elements for the key and the corresponding NFA,
		// 2) process the pending elements in event time order and custom comparator if exists
		//		by feeding them in the NFA
		// 3) advance the time to the current watermark, so that expired patterns are discarded.
		// 4) update the stored state for the key, by only storing the new NFA and MapState iff they
		//		have state to be used later.
		// 5) update the last seen watermark.

		// 1)获取 key 对应的事件队列及其相应的 NFA
		// 2)基于事件时间和自定义比较器（如果通过 NFA 中能获取到）来排序队列中的事件
		// 3)将时间置为当前的水印，这样过期的 patterns 就会被丢弃
		// 4)更新 key 的存储状态，只存储新的 NFA 和 MapState （如果它们稍后会使用）
		// 5)更新最后一次看到的 watermark

		// STEP 1
		PriorityQueue<Long> sortedTimestamps = getSortedTimestamps();
		NFA<IN> nfa = getNFA();

		// STEP 2
		while (!sortedTimestamps.isEmpty() && sortedTimestamps.peek() <= timerService.currentWatermark()) {
			long timestamp = sortedTimestamps.poll();
			sort(elementQueueState.get(timestamp)).forEachOrdered(
				event -> processEvent(nfa, event, timestamp)
			);
			elementQueueState.remove(timestamp);
		}

		// STEP 3
		advanceTime(nfa, timerService.currentWatermark());

		// STEP 4
		if (sortedTimestamps.isEmpty()) {
			elementQueueState.clear();
		}
		updateNFA(nfa);

		if (!sortedTimestamps.isEmpty() || !nfa.isEmpty()) {
			saveRegisterWatermarkTimer();
		}

		// STEP 5
		updateLastSeenWatermark(timerService.currentWatermark());
	}

	@Override
	public void onProcessingTime(InternalTimer<KEY, VoidNamespace> timer) throws Exception {
		// 1) get the queue of pending elements for the key and the corresponding NFA,
		// 2) process the pending elements in process time order and custom comparator if exists
		//		by feeding them in the NFA
		// 3) update the stored state for the key, by only storing the new NFA and MapState iff they
		//		have state to be used later.

		// 1)获取 key 对应的事件队列及其相应的 NFA
		// 2)基于当前处理时间和自定义比较器（如果通过 NFA 中能获取到）来排序队列中的事件
		// 3)更新 key 的存储状态，只存储新的 NFA 和 MapState （如果它们稍后会使用）

		// STEP 1
		PriorityQueue<Long> sortedTimestamps = getSortedTimestamps();
		NFA<IN> nfa = getNFA();

		// STEP 2
		while (!sortedTimestamps.isEmpty()) {
			long timestamp = sortedTimestamps.poll();
			sort(elementQueueState.get(timestamp)).forEachOrdered(
				event -> processEvent(nfa, event, timestamp)
			);
			elementQueueState.remove(timestamp);
		}

		// STEP 3
		if (sortedTimestamps.isEmpty()) {
			elementQueueState.clear();
		}
		updateNFA(nfa);
	}

	private Stream<IN> sort(Iterable<IN> iter) {
		Stream<IN> stream = StreamSupport.stream(iter.spliterator(), false);
		if (comparator == null) {
			return stream;
		} else {
			return stream.sorted(comparator);
		}
	}

	private void updateLastSeenWatermark(long timestamp) {
		this.lastWatermark = timestamp;
	}

	private NFA<IN> getNFA() throws IOException {
		NFA<IN> nfa = nfaOperatorState.value();
		return nfa != null ? nfa : nfaFactory.createNFA();
	}

	private void updateNFA(NFA<IN> nfa) throws IOException {
		if (nfa.isNFAChanged()) {
			if (nfa.isEmpty()) {
				nfaOperatorState.clear();
			} else {
				nfa.resetNFAChanged();
				nfaOperatorState.update(nfa);
			}
		}
	}

	private PriorityQueue<Long> getSortedTimestamps() throws Exception {
		PriorityQueue<Long> sortedTimestamps = new PriorityQueue<>();
		for (Long timestamp: elementQueueState.keys()) {
			sortedTimestamps.offer(timestamp);
		}
		return sortedTimestamps;
	}

	/**
	 * Process the given event by giving it to the NFA and outputting the produced set of matched
	 * event sequences.
	 *
	 * 通过将给定事件提供给 NFA 并输出生成的匹配事件序列集来处理给定事件。
	 *
	 * @param nfa NFA to be used for the event detection
	 * @param event The current event to be processed
	 * @param timestamp The timestamp of the event
	 */
	private void processEvent(NFA<IN> nfa, IN event, long timestamp)  {
		Tuple2<Collection<Map<String, List<IN>>>, Collection<Tuple2<Map<String, List<IN>>, Long>>> patterns =
			nfa.process(event, timestamp, afterMatchSkipStrategy);

		try {
			processMatchedSequences(patterns.f0, timestamp);
			processTimedOutSequences(patterns.f1, timestamp);
		} catch (Exception e) {
			//rethrow as Runtime, to be able to use processEvent in Stream.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Advances the time for the given NFA to the given timestamp. This can lead to pruning and
	 * timeouts.
	 *
	 * @param nfa to advance the time for
	 * @param timestamp to advance the time to
	 */
	private void advanceTime(NFA<IN> nfa, long timestamp) throws Exception {
		processEvent(nfa, null, timestamp);
	}

	protected abstract void processMatchedSequences(Iterable<Map<String, List<IN>>> matchingSequences, long timestamp) throws Exception;

	protected void processTimedOutSequences(
			Iterable<Tuple2<Map<String, List<IN>>, Long>> timedOutSequences,
			long timestamp) throws Exception {
	}

	//////////////////////			Testing Methods			//////////////////////

	@VisibleForTesting
	public boolean hasNonEmptyNFA(KEY key) throws IOException {
		setCurrentKey(key);
		return nfaOperatorState.value() != null;
	}

	@VisibleForTesting
	public boolean hasNonEmptyPQ(KEY key) throws Exception {
		setCurrentKey(key);
		return elementQueueState.keys().iterator().hasNext();
	}

	@VisibleForTesting
	public int getPQSize(KEY key) throws Exception {
		setCurrentKey(key);
		int counter = 0;
		for (List<IN> elements: elementQueueState.values()) {
			counter += elements.size();
		}
		return counter;
	}
}
