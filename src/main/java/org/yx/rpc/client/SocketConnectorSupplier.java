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
package org.yx.rpc.client;

import java.util.function.Supplier;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.yx.bean.IOC;
import org.yx.bean.Loader;
import org.yx.conf.AppInfo;
import org.yx.exception.SumkException;
import org.yx.log.Logs;
import org.yx.rpc.InnerRpcUtil;
import org.yx.rpc.codec.SumkCodecFactory;
import org.yx.util.ExceptionUtil;

public class SocketConnectorSupplier implements Supplier<SocketConnector> {

	private volatile SocketConnector connector;

	@Override
	public SocketConnector get() {
		SocketConnector con = this.connector;
		if (con != null && !con.isDisposing() && !con.isDisposed()) {
			return con;
		}
		return this.create();
	}

	private synchronized SocketConnector create() {
		if (connector != null && !connector.isDisposing() && !connector.isDisposed()) {
			return connector;
		}
		try {
			NioSocketConnector con = new NioSocketConnector(
					AppInfo.getInt("sumk.rpc.client.poolsize", Runtime.getRuntime().availableProcessors() + 1));
			con.setConnectTimeoutMillis(AppInfo.getInt("sumk.rpc.connect.timeout", 5000));
			InnerRpcUtil.config(con.getSessionConfig(), false);
			con.setHandler(createClientHandler());
			con.getFilterChain().addLast("codec", new ProtocolCodecFilter(IOC.get(SumkCodecFactory.class)));
			if (AppInfo.getBoolean("sumk.rpc.client.threadpool.enable", true)) {
				con.getFilterChain().addLast("threadpool", new ExecutorFilter(Rpc.clientExecutor()));
			}
			this.connector = con;
			return con;
		} catch (Exception e) {
			Logs.rpc().error(e.getMessage(), e);
			throw new SumkException(5423654, "create connector error", e);
		}
	}

	private IoHandler createClientHandler() {
		String clzName = AppInfo.get("sumk.rpc.clientHandler", "org.yx.rpc.client.ClientHandler");
		try {
			return (IoHandler) Loader.newInstance(clzName, null, null);
		} catch (Exception e) {
			Logs.rpc().error("初始化clientHandler失败", e);
			throw ExceptionUtil.toRuntimeException(e);
		}
	}

}
