package com.founder;

import io.javalin.websocket.WsContext;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.json.JSONObject;

class Query extends Thread {
	String qsql;
	int qid;
	String qname;
	List<String> fieldNames;
	ArrayDeque<Object> result = new ArrayDeque<>();
	StreamTableEnvironment tEnv;

	Query(String qsql, TableSchema schema, int qid, String qname, StreamTableEnvironment tEnv) {
		this.qsql = qsql;
		this.fieldNames = Arrays.asList(schema.getFieldNames());
		this.qid = qid;
		this.qname = qname;
		this.tEnv = tEnv;
	}

	String queryMetaString() {
		JSONObject js = new JSONObject();
		js.put("type", "queryMeta");
		/* js.put("logid", logid); */
		js.put("queryId", qid);
		js.put("queryName", qname);
		js.put("fieldNames", fieldNames);
		js.put("querySql", qsql);
		return js.toString();
	}

	String queryDataString() {
		JSONObject js = new JSONObject();
		js.put("type", "queryData");
		js.put("queryId", qid);
		js.put("data", result);
		return js.toString();
	}

	@Override
	public void run() {
		try {
			this.tEnv.execute("Streaming Window SQL Job");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

public class LogStream {
	/*
	 * 每个日志流里可能包含多个查询，每个查询都应该对应一个SocketSink
	 */
	String name;
	final String initddl;
	String createdTime;
	String executedTime;
	boolean ddlExecuted = false;
	StreamExecutionEnvironment env;
	StreamTableEnvironment tEnv;
	EnvironmentSettings settings;
	/* Map<Integer, SocketSink> sinks = new ConcurrentHashMap<>(); */
	List<WsContext> wss = new LinkedList<>();
	Integer queryinc = 1;
	List<Query> queries = new LinkedList<>();

	LogStream() {
		this(null, null);
	}

	LogStream(String name, String initddl) {
		this.name = name;
		this.initddl = initddl;

		this.createdTime = this.currentDateString();
		this.executedTime = "未执行";

		env = StreamExecutionEnvironment.getExecutionEnvironment().setParallelism(Constants.parallel);
		settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();

		tEnv = StreamTableEnvironment.create(env, settings);
	}

	String currentDateString() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date date = new Date(System.currentTimeMillis());
		return formatter.format(date);
	}

	void add_query(String querySql, String queryName) {
		// sql query
		// addSink
		// execute
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
		Query query = new Query(querySql, resultSchema, queryid, queryName, tEnv);
		DataStream<Row> resultDs = tEnv.toAppendStream(result, Row.class);
		queries.add(query);
		broadcast(queriesListString());
		broadcast(query.queryMetaString());
		SocketSink sink = new SocketSink(name, queryid);
		resultDs.addSink(sink);
		/* sinks.put(queryid, sink); */
		query.start();
	}

	void register(String ddl) {
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

	String queriesListString() {
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
}
