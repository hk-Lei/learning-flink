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
 * Interface for a character filter function. The filter function is given a string which the filter
 * can transform. The returned string is the transformation result.
 * 字符过滤器的接口。给过滤器传入一个字符串，返回的转换后字符串。
 */
public interface CharacterFilter {

	/**
	 * Filter the given string and generate a resulting string from it.
	 * 过滤给定的字符串并从中生成一个结果字符串。
	 *
	 * <p>For example, one implementation could filter out invalid characters from the input string.
	 * <p>例如，一个实现可以从输入字符串中过滤出无效字符。
	 *
	 * @param input Input string
	 * @return Filtered result string
	 */
	String filterCharacters(String input);
}
