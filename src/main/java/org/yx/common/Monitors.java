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
package org.yx.common;

import static org.yx.conf.AppInfo.LN;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.yx.bean.InnerIOC;
import org.yx.bean.NameSlot;
import org.yx.conf.AppInfo;
import org.yx.conf.Const;
import org.yx.db.conn.DataSourceManager;
import org.yx.db.conn.DataSources;
import org.yx.db.mapper.NamedExecutor;
import org.yx.db.sql.PojoMeta;
import org.yx.db.sql.PojoMetaHolder;
import org.yx.db.sql.VisitCounter;
import org.yx.db.visit.SumkStatement;
import org.yx.log.LogLevel;
import org.yx.log.Loggers;
import org.yx.main.SumkServer;
import org.yx.rpc.client.route.RpcRoutes;
import org.yx.rpc.data.RouteInfo;
import org.yx.util.StringUtil;
import org.yx.util.SumkDate;

public class Monitors {

	private static final String BLANK = "  ";

	public static String serverInfo() {
		long startTime = SumkServer.startTime();
		long now = System.currentTimeMillis();
		long ms = now - startTime;
		StringBuilder sb = new StringBuilder();
		sb.append("start").append(BLANK).append(SumkDate.of(startTime).to_yyyy_MM_dd_HH_mm_ss_SSS().replace(" ", "T"))
				.append(BLANK).append("run(ms)").append(BLANK).append(ms).append(LN).append("localip").append(BLANK)
				.append(AppInfo.getLocalIp()).append(BLANK).append("pid").append(BLANK).append(AppInfo.pid()).append(LN)
				.append("framework").append(BLANK).append(Const.sumkVersion());
		String v = AppInfo.appId(null);
		if (v != null) {
			sb.append(BLANK).append("appId").append(BLANK).append(v);
		}
		return sb.toString();
	}

	public static String systemInfo() {
		Map<Object, Object> map = new HashMap<>(System.getProperties());
		StringBuilder sb = new StringBuilder();
		map.forEach((k, v) -> {
			if (v != null) {
				v = v.toString().replace("\r", "\\r").replace("\n", "\\n");
			}
			sb.append(k).append(" : ").append(v).append(LN);
		});
		return sb.toString();
	}

	public static String jvmInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("## name   init   max   commited    used").append(LN);
		DecimalFormat f = new DecimalFormat("#,###");
		List<MemoryPoolMXBean> mpmxbs = ManagementFactory.getMemoryPoolMXBeans();
		for (MemoryPoolMXBean mpmxb : mpmxbs) {
			if (mpmxb == null || mpmxb.getUsage() == null) {
				continue;
			}
			String name = mpmxb.getName();
			if (name == null || name.isEmpty()) {
				continue;
			}
			sb.append(name).append(BLANK).append(f.format(mpmxb.getUsage().getInit())).append(BLANK)
					.append(f.format(mpmxb.getUsage().getMax())).append(BLANK)
					.append(f.format(mpmxb.getUsage().getCommitted())).append(BLANK)
					.append(f.format(mpmxb.getUsage().getUsed())).append(LN);
		}
		return sb.toString();
	}

	public static String allTrack() {
		Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
		StringBuilder sb = new StringBuilder();
		map.forEach((t, st) -> {
			sb.append(t.getName() + "  [id:" + t.getId() + "]").append(LN);
			for (StackTraceElement e : st) {
				if (sb.length() > 0) {
					sb.append(BLANK);
				}
				sb.append(e.getClassName() + "." + e.getMethodName() + "() ");
				if (e.getLineNumber() > 0) {
					sb.append(e.getLineNumber());
				}
				sb.append(LN);
			}
			sb.append(LN);
		});
		return sb.toString();
	}

	public static String threadPoolInfo(ThreadPoolExecutor pool) {
		StringBuilder sb = new StringBuilder();
		sb.append("active").append(BLANK).append(pool.getActiveCount()).append(BLANK).append("size").append(BLANK)
				.append(pool.getPoolSize()).append(BLANK).append("queue").append(BLANK).append(pool.getQueue().size())
				.append(BLANK).append(LN).append("max").append(BLANK).append(pool.getMaximumPoolSize()).append(BLANK)
				.append("keepAlive(ms)").append(BLANK).append(pool.getKeepAliveTime(TimeUnit.MILLISECONDS))
				.append(BLANK)

				.append("completed*").append(BLANK).append(pool.getCompletedTaskCount());
		return sb.toString();
	}

	public static List<String> beans() {
		Collection<Object> beans = InnerIOC.beans();
		List<String> names = new ArrayList<>();
		for (Object obj : beans) {
			names.add(obj.getClass().getName());
		}
		Collections.sort(names);
		return names;
	}

	public static String beansName() {
		StringBuilder sb = new StringBuilder();
		List<String> names = InnerIOC.beanNames();
		Collections.sort(names);
		for (String name : names) {
			NameSlot slot = InnerIOC.getSlot(name);
			sb.append(slot).append(Const.LN);
		}
		return sb.toString();
	}

	public static String logLevels() {
		Map<String, LogLevel> map = new TreeMap<>(Loggers.currentLevels());
		StringBuilder sb = new StringBuilder("#logLevels:").append(LN);
		char[] black = new char[7];
		Arrays.fill(black, ' ');
		map.forEach((k, v) -> {
			sb.append(v).append(black, 0, black.length - v.name().length()).append(k).append(LN);
		});
		return sb.toString();
	}

	public static String dbVisitInfo() {
		List<PojoMeta> list = PojoMetaHolder.allPojoMeta();
		StringBuilder sb = new StringBuilder(128);
		sb.append("##sql总执行次数").append(BLANK).append(SumkStatement.getExecuteCount()).append(BLANK).append(BLANK)
				.append("SDB执行次数").append(BLANK).append(NamedExecutor.getExecuteCount()).append(LN);
		sb.append("#tableName").append(BLANK).append("queryCount").append(BLANK).append("modifyCount").append(BLANK)
				.append("cacheKeyVisits").append(BLANK).append("cacheKeyHits").append(AppInfo.LN);
		long modify, query, cacheVisit, cacheHit;
		long totalModify = 0, totalQuery = 0, totalCacheVisit = 0, totalCacheHit = 0;
		for (PojoMeta p : list) {
			VisitCounter c = p.getCounter();
			query = c.getQueryCount();
			modify = c.getModifyCount();
			sb.append(p.getTableName()).append(BLANK).append(query).append(BLANK).append(modify).append(BLANK);
			totalModify += modify;
			totalQuery += query;
			if (p.isNoCache()) {
				sb.append("-").append(BLANK).append("-").append(AppInfo.LN);
				continue;
			}
			cacheVisit = c.getCacheKeyVisits();
			cacheHit = c.getCacheKeyHits();
			sb.append(cacheVisit).append(BLANK).append(cacheHit).append(AppInfo.LN);
			totalCacheVisit += cacheVisit;
			totalCacheHit += cacheHit;
		}
		sb.append("*total*").append(BLANK).append(totalQuery).append(BLANK).append(totalModify).append(BLANK)
				.append(totalCacheVisit).append(BLANK).append(totalCacheHit).append(AppInfo.LN);
		return sb.toString();
	}

	public static String dataSourceStatus(String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}
		StringBuilder sb = new StringBuilder(200);
		if (!DataSources.getManagerSelector().dbNames().contains(name)) {
			sb.append(DataSources.getManagerSelector().dbNames());
			return sb.toString();
		}
		DataSourceManager manager = DataSources.getManager(name);
		if (manager == null) {
			return sb.toString();
		}
		sb.append(manager.status());
		return sb.toString();
	}

	public static String sumkDateCacheChangeCount() {
		StringBuilder sb = new StringBuilder();
		sb.append("##sumkDateCached").append(BLANK).append(SumkDate.cacheChangeCount());
		return sb.toString();
	}

	public static String gcInfo() {
		StringBuilder sb = new StringBuilder(64).append("##name").append(BLANK).append("count").append(BLANK)
				.append("time(ms)");
		for (GarbageCollectorMXBean mxBean : ManagementFactory.getGarbageCollectorMXBeans()) {
			sb.append(LN).append(getGcName(mxBean.getName())).append(BLANK).append(mxBean.getCollectionCount())
					.append(BLANK).append(mxBean.getCollectionTime());
		}
		return sb.toString();
	}

	private static String getGcName(String name) {
		if ("PS Scavenge".equals(name) || "ParNew".equals(name) || "G1 Young Generation".equals(name)
				|| "Copy".equals(name)) {
			return "Young";
		}
		if ("PS MarkSweep".equals(name) || "ConcurrentMarkSweep".equals(name) || "G1 Old Generation".equals(name)
				|| "MarkSweepCompact".equals(name)) {
			return "Old";
		}
		return name;
	}

	public static String rpcDatas() {
		RpcRoutes route = RpcRoutes.current();
		StringBuilder sb = new StringBuilder(64).append("##rpcData").append(BLANK)
				.append(SumkDate.of(route.createTime()));
		for (RouteInfo info : route.zkDatas()) {
			sb.append(LN).append(info.path()).append(BLANK).append(info.host()).append(BLANK).append(info.apis());
		}
		return sb.toString();
	}

}
