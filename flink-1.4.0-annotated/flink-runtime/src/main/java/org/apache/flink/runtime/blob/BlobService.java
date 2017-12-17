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

package org.apache.flink.runtime.blob;

import java.io.Closeable;

/**
 * A simple store and retrieve binary large objects (BLOBs).
 * 一个简单存储和检索二进制大对象(blob)的服务接口。
 */
public interface BlobService extends Closeable {

	/**
	 * Returns a BLOB service for accessing permanent BLOBs.
	 * 返回一个 BLOB 服务，用于访问永久的 BLOB。
	 *
	 * @return BLOB service
	 */
	PermanentBlobService getPermanentBlobService();

	/**
	 * Returns a BLOB service for accessing transient BLOBs.
	 * 返回一个 BLOB 服务，用于访问临时 BLOB。
	 *
	 * @return BLOB service
	 */
	TransientBlobService getTransientBlobService();

	/**
	 * Returns the port of the BLOB server that this BLOB service is working with.
	 * 返回 BLOB 服务的服务器端口。
	 *
	 * @return the port the blob server.
	 */
	int getPort();
}
