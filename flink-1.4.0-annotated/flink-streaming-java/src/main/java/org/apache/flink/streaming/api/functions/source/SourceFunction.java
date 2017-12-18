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

package org.apache.flink.streaming.api.functions.source;

import org.apache.flink.annotation.Public;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.Function;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.functions.TimestampAssigner;
import org.apache.flink.streaming.api.watermark.Watermark;

import java.io.Serializable;

/**
 * Base interface for all stream data sources in Flink. The contract of a stream source
 * is the following: When the source should start emitting elements, the {@link #run} method
 * is called with a {@link SourceContext} that can be used for emitting elements.
 * The run method can run for as long as necessary. The source must, however, react to an
 * invocation of {@link #cancel()} by breaking out of its main loop.
 *
 * <h3>Checkpointed Sources</h3>
 *
 * <p>Sources that also implement the {@link org.apache.flink.streaming.api.checkpoint.Checkpointed}
 * interface must ensure that state checkpointing, updating of internal state and emission of
 * elements are not done concurrently. This is achieved by using the provided checkpointing lock
 * object to protect update of state and emission of elements in a synchronized block.
 *
 * <p>This is the basic pattern one should follow when implementing a (checkpointed) source:
 *
 * <pre>{@code
 *  public class ExampleSource<T> implements SourceFunction<T>, Checkpointed<Long> {
 *      private long count = 0L;
 *      private volatile boolean isRunning = true;
 *
 *      public void run(SourceContext<T> ctx) {
 *          while (isRunning && count < 1000) {
 *              synchronized (ctx.getCheckpointLock()) {
 *                  ctx.collect(count);
 *                  count++;
 *              }
 *          }
 *      }
 *
 *      public void cancel() {
 *          isRunning = false;
 *      }
 *
 *      public Long snapshotState(long checkpointId, long checkpointTimestamp) { return count; }
 *
 *      public void restoreState(Long state) { this.count = state; }
 * }
 * }</pre>
 *
 *
 * <h3>Timestamps and watermarks:</h3>
 * Sources may assign timestamps to elements and may manually emit watermarks.
 * However, these are only interpreted if the streaming program runs on
 * {@link TimeCharacteristic#EventTime}. On other time characteristics
 * ({@link TimeCharacteristic#IngestionTime} and {@link TimeCharacteristic#ProcessingTime}),
 * the watermarks from the source function are ignored.
 *
 * <h3>Gracefully Stopping Functions</h3>
 * Functions may additionally implement the {@link org.apache.flink.api.common.functions.StoppableFunction}
 * interface. "Stopping" a function, in contrast to "canceling" means a graceful exit that leaves the
 * state and the emitted elements in a consistent state.
 *
 * <p>When a source is stopped, the executing thread is not interrupted, but expected to leave the
 * {@link #run(SourceContext)} method in reasonable time on its own, preserving the atomicity
 * of state updates and element emission.
 *
 * Flink 中所有流数据源的基本接口。流数据源的规则如下: 当源开始下发元素时，执行带有 SourceContext 参数的 run 方法。
 * 该方法可以在必要时一直运行（有数据就运行）。 但是，源必须能够跳出主循环来响应 cancel() 的调用。
 *
 * <h3> 支持 Checkpoint 的数据源</h3>
 *
 * <p> Source 也实现了 CheckpointedFunction（Checkpointed已过期）接口，必须确保状态检查点、内部状态的更新和元素的释放不能同时执行。
 * 这是通过使用检查点锁对象来实现的，以保护同步块中元素的更新和释放。
 *
 * <p>这是在实现支持检查点源时应该遵循的基本模式:
 *
 * <pre>{@code
 *  public class ExampleSource<T> implements SourceFunction<T>, CheckpointedFunction<Long> {
 *      private long count = 0L;
 *      private volatile boolean isRunning = true;
 *
 *      public void run(SourceContext<T> ctx) {
 *          while (isRunning && count < 1000) {
 *              synchronized (ctx.getCheckpointLock()) {
 *                  ctx.collect(count);
 *                  count++;
 *              }
 *          }
 *      }
 *
 *      public void cancel() {
 *          isRunning = false;
 *      }
 *
 *      public Long snapshotState(long checkpointId, long checkpointTimestamp) { return count; }
 *
 *      public void initializeState(Long state) { this.count = state; }
 *  }
 * }</pre>
 *
 * <h3>Timestamps and watermarks:</h3>
 *
 * Source 可以为元素指定时间戳并手动提交 watermark，但这只有当流处理是基于 EventTime 时才执行，
 * 基于其他时间例如 IngestionTime 和 ProcessingTime，Source 会忽略 watermarks
 *
 * <h3>优雅地停止</h3>
 *
 * 可以实现一个 StoppableFunction 接口。“停止” 与 “取消” 不同，停止是指优雅的退出：能够使状态和元素都保持一致的状态
 *
 * <p>当一个源被停止时，执行线程不会被中断，但期望在合理的时间内离开 run(SourceContext)方法，保持状态更新和元素释放的原子性。
 *
 * @param <T> The type of the elements produced by this source.
 *
 * @see org.apache.flink.api.common.functions.StoppableFunction
 * @see org.apache.flink.streaming.api.TimeCharacteristic
 */
@Public
public interface SourceFunction<T> extends Function, Serializable {

	/**
	 * Starts the source. Implementations can use the {@link SourceContext} emit
	 * elements.
	 *
	 * <p>Sources that implement {@link org.apache.flink.streaming.api.checkpoint.Checkpointed}
	 * must lock on the checkpoint lock (using a synchronized block) before updating internal
	 * state and emitting elements, to make both an atomic operation:
	 *
	 * <pre>{@code
	 *  public class ExampleSource<T> implements SourceFunction<T>, Checkpointed<Long> {
	 *      private long count = 0L;
	 *      private volatile boolean isRunning = true;
	 *
	 *      public void run(SourceContext<T> ctx) {
	 *          while (isRunning && count < 1000) {
	 *              synchronized (ctx.getCheckpointLock()) {
	 *                  ctx.collect(count);
	 *                  count++;
	 *              }
	 *          }
	 *      }
	 *
	 *      public void cancel() {
	 *          isRunning = false;
	 *      }
	 *
	 *      public Long snapshotState(long checkpointId, long checkpointTimestamp) { return count; }
	 *
	 *      public void restoreState(Long state) { this.count = state; }
	 * }
	 * }</pre>
	 *
	 * @param ctx The context to emit elements to and for accessing locks.
	 */
	void run(SourceContext<T> ctx) throws Exception;

	/**
	 * Cancels the source. Most sources will have a while loop inside the
	 * {@link #run(SourceContext)} method. The implementation needs to ensure that the
	 * source will break out of that loop after this method is called.
	 *
	 * <p>A typical pattern is to have an {@code "volatile boolean isRunning"} flag that is set to
	 * {@code false} in this method. That flag is checked in the loop condition.
	 *
	 * <p>When a source is canceled, the executing thread will also be interrupted
	 * (via {@link Thread#interrupt()}). The interruption happens strictly after this
	 * method has been called, so any interruption handler can rely on the fact that
	 * this method has completed. It is good practice to make any flags altered by
	 * this method "volatile", in order to guarantee the visibility of the effects of
	 * this method to any interruption handler.
	 */
	void cancel();

	// ------------------------------------------------------------------------
	//  source context
	// ------------------------------------------------------------------------

	/**
	 * Interface that source functions use to emit elements, and possibly watermarks.
	 * SourceFunction 用来 emit 元素 (可能也有 watermark) 的接口
	 *
	 * @param <T> The type of the elements produced by the source.
	 */
	@Public // Interface might be extended in the future with additional methods.
	interface SourceContext<T> {

		/**
		 * Emits one element from the source, without attaching a timestamp. In most cases,
		 * this is the default way of emitting elements.
		 *
		 * <p>The timestamp that the element will get assigned depends on the time characteristic of
		 * the streaming program:
		 * <ul>
		 *     <li>On {@link TimeCharacteristic#ProcessingTime}, the element has no timestamp.</li>
		 *     <li>On {@link TimeCharacteristic#IngestionTime}, the element gets the system's
		 *         current time as the timestamp.</li>
		 *     <li>On {@link TimeCharacteristic#EventTime}, the element will have no timestamp initially.
		 *         It needs to get a timestamp (via a {@link TimestampAssigner}) before any time-dependent
		 *         operation (like time windows).</li>
		 * </ul>
		 *
		 * 从 Source 下发一个元素，不附加时间戳。在大多数情况下，这是下发元素的默认方式。
		 *
		 * @param element The element to emit
		 */
		void collect(T element);

		/**
		 * Emits one element from the source, and attaches the given timestamp. This method
		 * is relevant for programs using {@link TimeCharacteristic#EventTime}, where the
		 * sources assign timestamps themselves, rather than relying on a {@link TimestampAssigner}
		 * on the stream.
		 *
		 * <p>On certain time characteristics, this timestamp may be ignored or overwritten.
		 * This allows programs to switch between the different time characteristics and behaviors
		 * without changing the code of the source functions.
		 * <ul>
		 *     <li>On {@link TimeCharacteristic#ProcessingTime}, the timestamp will be ignored,
		 *         because processing time never works with element timestamps.</li>
		 *     <li>On {@link TimeCharacteristic#IngestionTime}, the timestamp is overwritten with the
		 *         system's current time, to realize proper ingestion time semantics.</li>
		 *     <li>On {@link TimeCharacteristic#EventTime}, the timestamp will be used.</li>
		 * </ul>
		 *
		 * Source 下发一个元素并附加一个给定的时间戳
		 *
		 * @param element The element to emit
		 * @param timestamp The timestamp in milliseconds since the Epoch
		 */
		@PublicEvolving
		void collectWithTimestamp(T element, long timestamp);

		/**
		 * Emits the given {@link Watermark}. A Watermark of value {@code t} declares that no
		 * elements with a timestamp {@code t' <= t} will occur any more. If further such
		 * elements will be emitted, those elements are considered <i>late</i>.
		 *
		 * <p>This method is only relevant when running on {@link TimeCharacteristic#EventTime}.
		 * On {@link TimeCharacteristic#ProcessingTime},Watermarks will be ignored. On
		 * {@link TimeCharacteristic#IngestionTime}, the Watermarks will be replaced by the
		 * automatic ingestion time watermarks.
		 *
		 * 发出给定的 Watermark 。
		 * 一个值为 t 的 watermark 表示：任何带有 timestamp t' <= t的元素都不应该发生。
		 * 如果将来这样的元素被下发，这些元素会被认为是 <i> 迟到了 </i>。
		 *
		 * 这个方法只在运行在 EventTime 的情况下有效
		 * 在 ProcessingTime 下，Watermark 是被忽略的
		 * 在 IngestionTime 下，Watermark 将被自动摄入时间的 Watermark 所取代。
		 *
		 * @param mark The Watermark to emit
		 */
		@PublicEvolving
		void emitWatermark(Watermark mark);

		/**
		 * Marks the source to be temporarily idle. This tells the system that this source will
		 * temporarily stop emitting records and watermarks for an indefinite amount of time. This
		 * is only relevant when running on {@link TimeCharacteristic#IngestionTime} and
		 * {@link TimeCharacteristic#EventTime}, allowing downstream tasks to advance their
		 * watermarks without the need to wait for watermarks from this source while it is idle.
		 *
		 * <p>Source functions should make a best effort to call this method as soon as they
		 * acknowledge themselves to be idle. The system will consider the source to resume activity
		 * again once {@link SourceContext#collect(T)}, {@link SourceContext#collectWithTimestamp(T, long)},
		 * or {@link SourceContext#emitWatermark(Watermark)} is called to emit elements or watermarks from the source.
		 *
		 * 标记 Source 暂时空闲，这就相当于告诉系统，这个源将暂时停止下发记录和 watermark 的时间是不确定的。
		 * 只是对于 EventTime 或 IngestionTime 运行时有效，
		 * 允许下游任务提前完成 Watermark ，无需在空闲时等待来自此 Source 的 Watermark 。
		 *
		 * Source 应该尽可能快的在他们处于空闲时就调用该方法，
		 * 一旦 collect(T)、collectWithTimestamp(T,long) 或 emitWatermark(Watermark)} 方法被调用从源发出元素或 Watermark，
		 * 系统将考虑重新激活的这个 Source。
		 */
		@PublicEvolving
		void markAsTemporarilyIdle();

		/**
		 * Returns the checkpoint lock. Please refer to the class-level comment in
		 * {@link SourceFunction} for details about how to write a consistent checkpointed
		 * source.
		 *
		 * 返回检查点的锁。请参阅 SourceFunction 中的注释，了解如何编写支持一致性检查点的 Source 代码。
		 *
		 * @return The object to use as the lock
		 */
		Object getCheckpointLock();

		/**
		 * This method is called by the system to shut down the context.
		 *
		 * 该方法被系统调用以关闭上下文。
		 */
		void close();
	}
}
