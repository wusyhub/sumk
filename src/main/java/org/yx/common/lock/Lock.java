/**
 * Copyright (C) 2016 - 2030 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.common.lock;

/**
 * 分布式锁的钥匙
 * 
 * @author 游夏
 *
 */
public interface Lock extends AutoCloseable {
	/**
	 * @return 钥匙的Id，肯定不为空
	 */
	String getId();

	void unlock();

	@Override
	default void close() {
		this.unlock();
	}

}
