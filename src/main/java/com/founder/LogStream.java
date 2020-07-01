package com.founder;

import io.javalin.websocket.WsContext;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;

class Query {
	String qsql;
	int qid;
	TableSchema resultSchema;
	ArrayDeque<Object> result=new ArrayDeque<>();

	Query(String qsql, TableSchema schema, int qid) {
		this.qsql = qsql;
		this.resultSchema = schema;
		this.qid = qid;
	}
}

public class LogStream {
	/*
	 * 每个日志流里可能包含多个查询，每个查询都应该对应一个SocketSink
	 */
	String name;
	String initddl;
	StreamExecutionEnvironment env;
	StreamTableEnvironment tEnv;
	EnvironmentSettings settings;
	/* Map<Integer, SocketSink> sinks = new ConcurrentHashMap<>(); */
	List<WsContext> wss = new LinkedList<>();
	Integer queryinc = 1;
	Map<Integer, Query> queries = new ConcurrentHashMap<>();

	LogStream() {
		this(null, null);
	}

	LogStream(String name, String initddl) {
		this.name = name;
		this.initddl = initddl;

		env = StreamExecutionEnvironment.getExecutionEnvironment();
		settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();

		tEnv = StreamTableEnvironment.create(env, settings);
	}

	void add_query(String querysql) {
		// sql query
		// addSink
		// execute
		if (initddl != null) {
			System.out.println("add_query sqlUpdate");
			tEnv.sqlUpdate(initddl);
			initddl = null;
		}
		System.out.println("add_query sqlQuery");
		Table result = tEnv.sqlQuery(querysql);
		result.printSchema();
		TableSchema resultSchema = result.getSchema();
		/* Optional<DataType> f0type = resultSchema.getFieldDataType(0); */
		/* System.out.println(resultSchema); */
		/* System.out.println(f0type); */
		int queryid = queryinc++;
		Query query = new Query(querysql, resultSchema, queryid);
		DataStream<Row> resultDs = tEnv.toAppendStream(result, Row.class);
		queries.put(queryid, query);
		SocketSink sink = new SocketSink(name, queryid);
		resultDs.addSink(sink);
		/* sinks.put(queryid, sink); */
		try {
			tEnv.execute("Streaming Window SQL Job");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	void register(String ddl) {
	}
}
