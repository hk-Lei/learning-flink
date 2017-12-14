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
 *
 */

package org.apache.flink.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation for marking classes as public, stable interfaces.
 * 将类标记为公共、稳定接口的注释
 *
 * <p>Classes, methods and fields with this annotation are stable across minor releases (1.0, 1.1, 1.2). In other words,
 * applications using @Public annotated classes will compile against newer versions of the same major release.
 * <p >用此注释标记的类、方法和字段在小版本(1.0,1.1,1.2)之间是稳定的。换句话说，编译同一个 major 版本时对 @Public 注释的类会使用更新的版本进行编译。
 *
 * <p>Only major releases (1.0, 2.0, 3.0) can break interfaces with this annotation.
 * <p>只有主要的版本(如 1.0,2.0,3.0 等)可以用这个注释打破接口。
 */
@Documented
@Target(ElementType.TYPE)
@Public
public @interface Public {}
