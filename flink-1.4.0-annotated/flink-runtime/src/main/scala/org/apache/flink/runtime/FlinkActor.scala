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

package org.apache.flink.runtime

import _root_.akka.actor.Actor
import grizzled.slf4j.Logger

/** Base trait for Flink's actors.
  *
  * The message handling logic is defined in the handleMessage method. This allows to mixin
  * stackable traits which change the message receiving behaviour.
  *
  * Flink 的 Actors 的基本特征（trait）
  *
  * 消息处理逻辑是在 handleMessage 方法中定义的。这允许混合叠加那些不同消息处理逻辑的特征。
  */
trait FlinkActor extends Actor {
  val log: Logger

  override def receive: Receive = handleMessage

  /** Handle incoming messages
    * 处理传入的消息
    *
    * @return
    */
  def handleMessage: Receive

  /** Factory method for messages. This method can be used by mixins to decorate messages
    * 工厂方法。这种方法可以用于混合装饰消息
    *
    * @param message The message to decorate
    * @return The decorated message
    */
  def decorateMessage(message: Any): Any = {
    message
  }
}
