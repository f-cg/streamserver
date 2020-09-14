package com.founder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.json.JSONObject;

import io.javalin.websocket.WsContext;

enum LogStreamType {
	DMKF, KF, FS;
}

public class LogStream {
	/*
	 * 每个日志流里可能包含多个查询，每个查询都应该对应一个SocketSink
	 */
	private LogStreamType lsType;
	public String name;
	private final String initddl;
	private String DMSql;
	private String createdTime;
	private String executedTime;
	private boolean ddlExecuted = false;
	private StreamExecutionEnvironment env;
	private StreamTableEnvironment tEnv;
	private EnvironmentSettings settings;
	/* Map<Integer, SocketSink> sinks = new ConcurrentHashMap<>(); */
	private List<WsContext> wss = new LinkedList<>();
	private Integer queryinc = 1;
	private List<Query> queries = new LinkedList<>();
	DM2Kafka dm2kafka;
	private static final Pattern PP = Pattern.compile("^PATTERN");
	private static final Pattern PR = Pattern.compile("^PREDICT");

	LogStream(String name, String initddl) {
		this.name = name;
		this.lsType = checkLogStreamType(initddl);
		if (lsType == LogStreamType.DMKF) {
			this.DMSql = initddl;
			dm2kafka = new DM2Kafka(initddl, name);
			dm2kafka.firstRun();
			String createSql = this.dm2kafka.genCreateSql();
			this.initddl = createSql;
			dm2kafka.start();
		} else {
			this.initddl = initddl;
		}
		this.createdTime = this.currentDateString();
		this.executedTime = "未执行";

		env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(Constants.parallel);
		settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();

		tEnv = StreamTableEnvironment.create(env, settings);
		System.err.println("LogStream constructed!");
	}

	LogStreamType checkLogStreamType(String ddl) {
		Pattern FS = Pattern.compile("'connector.type'[\\s]*=[\\s]*'filesystem'", Pattern.CASE_INSENSITIVE);
		Pattern KF = Pattern.compile("'connector.type'[\\s]*=[\\s]*'kafka'", Pattern.CASE_INSENSITIVE);
		Pattern DMKF = Pattern.compile("[\\s]*select[\\s]+", Pattern.CASE_INSENSITIVE);
		if (FS.matcher(ddl).find()) {
			return LogStreamType.FS;
		} else if (KF.matcher(ddl).find()) {
			return LogStreamType.KF;
		} else if (DMKF.matcher(ddl).find()) {
			return LogStreamType.DMKF;
		} else {
			return LogStreamType.DMKF;
		}
	}

	private String currentDateString() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date date = new Date(System.currentTimeMillis());
		return formatter.format(date);
	}

	/**
	 * @param querySql PATTERN\ncaseKey\nvalueKey1,valueKey2
	 */
	private void addQueryFreqPred(String querySql, String queryName, QueryType qtype, JSONObject frontInterest) {
		String[] lines = querySql.split("\n");
		String caseKey = lines[1];
		String[] eventsKeys = lines[2].split(",");
		String timeField = lines[3];
		int queryid = queryinc++;
		Query query = new Query(this, querySql, caseKey, eventsKeys, timeField, queryid, queryName, qtype,
				frontInterest);
		queries.add(query);
		System.err.println("before broadcast");
		broadcast(queriesListString());
		query.refresh();
		broadcast(query.queryMetaString());
	}

	void addQuery(String querySql, String queryName, JSONObject frontInterest) {
		// sql query
		// addSink
		// execute
		if (frontInterest == null) {
			frontInterest = new JSONObject();
		}
		String defaultctype = frontInterest.optString("defaultctype");
		if (PP.matcher(querySql).find()) {
			if (defaultctype == null || defaultctype.isEmpty()) {
				frontInterest.put("defaultctype", "table");
			}
			addQueryFreqPred(querySql, queryName, QueryType.FrequentPattern, frontInterest);
			return;
		} else if (PR.matcher(querySql).find()) {
			if (defaultctype == null || defaultctype.isEmpty()) {
				frontInterest.put("defaultctype", "table");
			}
			addQueryFreqPred(querySql, queryName, QueryType.Predict, frontInterest);
			return;
		}
		if (defaultctype == null || defaultctype.isEmpty()) {
			frontInterest.put("defaultctype", "graph");
		}
		if (!ddlExecuted) {
			tEnv.sqlUpdate(initddl);
			ddlExecuted = true;
			this.executedTime = this.currentDateString();
		}
		System.out.println("add_query sqlQuery");
		Table result = tEnv.sqlQuery(querySql);
		result.printSchema();
		TableSchema resultSchema = result.getSchema();
		/* Optional<DataType> f0type = resultSchema.getFieldDataType(0); */
		/* System.out.println(resultSchema); */
		/* System.out.println(f0type); */
		int queryid = queryinc++;
		Query query = new Query(this, querySql, resultSchema, queryid, queryName, tEnv, frontInterest);
		DataStream<Row> resultDs = tEnv.toAppendStream(result, Row.class);
		queries.add(query);
		broadcast(queriesListString());
		broadcast(query.queryMetaString());
		SocketSink sink = new SocketSink(name, queryid);
		resultDs.addSink(sink);
		/* sinks.put(queryid, sink); */
		query.start();
	}

	void broadcast(String msg) {
		wss.stream().filter(ct -> ct.session.isOpen()).forEach(session -> {
			System.out.println("session_send: " + msg);
			session.send(msg);
		});
	}

	Query getquery(int qid) {
		for (Query q : queries) {
			if (q.qid == qid) {
				return q;
			}
		}
		return null;
	}

	void delquery(int qid) {
		Query q = getquery(qid);
		if (q != null) {
			queries.remove(q);
		}
	}

	private String queriesListString() {
		JSONObject js = new JSONObject();
		js.put("type", "queriesList");
		js.put("logId", name);
		List<Integer> qids = new ArrayList<>();
		for (Query q : queries) {
			qids.add(q.qid);
		}
		js.put("queriesId", qids);
		return js.toString();
	}

	void addWss(WsContext ctx) {
		this.wss.add(ctx);

	}

	Map<String, String> getLogdetailModel() {
		Map<String, String> model = new HashMap<String, String>();
		model.put("logId", this.name);
		model.put("ddl", this.initddl);
		model.put("createdTime", this.createdTime);
		model.put("executedTime", this.executedTime);
		return model;
	}

	void sendQueriesList(WsContext ctx) {
		ctx.send(this.queriesListString());
	}

	void sendQueriesMetas(WsContext ctx) {
		for (Query q : this.queries) {
			ctx.send(q.queryMetaString());
		}
	}

	void sendQueriesData(WsContext ctx) {
		for (Query q : this.queries) {
			ctx.send(q.queryDataString());
		}
	}

	/*
	 * 日志流对应的所有查询的id列表 Metas 结果 分别发给该连接
	 *
	 */
	void sendLogdetails(WsContext ctx) {
		this.sendQueriesList(ctx);
		this.sendQueriesMetas(ctx);
		this.sendQueriesData(ctx);
	}
}
