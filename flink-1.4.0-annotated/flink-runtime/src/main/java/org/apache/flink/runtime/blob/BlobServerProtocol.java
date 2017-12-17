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

public class BlobServerProtocol {

	// --------------------------------------------------------------------------------------------
	//  Constants used in the protocol of the BLOB store
	//  在 BLOB 存储协议中使用的常量
	// --------------------------------------------------------------------------------------------

	/** The buffer size in bytes for network transfers. */
	/** 用于网络传输的缓冲区大小。 64K */
	static final int BUFFER_SIZE = 65536; // 64 K

	/**
	 * Internal code to identify a PUT operation.
	 *
	 * <p>Note: previously, there was also <tt>DELETE_OPERATION</tt> (code <tt>2</tt>).
	 *
	 * 用于标识 PUT 操作的内部代码。
	 *
	 * <p>注意: 以前，这里也有 DELETE_OPERATION （值为 2）
	 */
	static final byte PUT_OPERATION = 0;

	/**
	 * Internal code to identify a GET operation.
	 *
	 * <p>Note: previously, there was also <tt>DELETE_OPERATION</tt> (code <tt>2</tt>).
	 *
	 * 用于标识 GET 操作的内部代码。
	 *
	 * <p>注意: 以前，这里也有 DELETE_OPERATION （值为 2）
	 */
	static final byte GET_OPERATION = 1;

	/** Internal code to identify a successful operation. */
	/** 确定一个成功操作的内部代码。*/
	static final byte RETURN_OKAY = 0;

	/** Internal code to identify an erroneous operation. */
	/** 确定一个错误操作的内部代码。*/
	static final byte RETURN_ERROR = 1;

	/**
	 * Internal code to identify a job-unrelated BLOBs (only for transient BLOBs!).
	 *
	 * <p>Note: previously, there was also <tt>NAME_ADDRESSABLE</tt> (code <tt>1</tt>).
	 *
	 * 标识一个与 Job 无关的 blobs (临时的 blobs) 的内部代码
	 *
	 * <p>注意: 以前，这里也有 NAME_ADDRESSABLE (值为 1)。
	 */
	static final byte JOB_UNRELATED_CONTENT = 0;

	/**
	 * Internal code to identify a job-related (permanent or transient) BLOBs.
	 *
	 * <p>Note: This is equal to the previous <tt>JOB_ID_SCOPE</tt> (code <tt>2</tt>).
	 *
	 * 标识一个与 Job 有关的 blobs (临时的 blobs) 的内部代码
	 *
	 * <p>注意: 这个与以前的 <tt>JOB_ID_SCOPE</tt> 是一致的
	 */
	static final byte JOB_RELATED_CONTENT = 2;

	// --------------------------------------------------------------------------------------------

	private BlobServerProtocol() {}
}
