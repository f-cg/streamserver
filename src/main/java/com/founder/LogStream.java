package com.founder;
import io.javalin.websocket.WsContext;
import java.util.LinkedList;
import java.util.List;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

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

	void add_query(String query) {
		// sql query
		// addSink
		// execute
		if (initddl != null) {
			System.out.println("add_query sqlUpdate");
			tEnv.sqlUpdate(initddl);
			initddl = null;
		}
		System.out.println("add_query sqlQuery");
		Table result = tEnv.sqlQuery(query);
		DataStream<Row> resultDs = tEnv.toAppendStream(result, Row.class);
		int queryid = queryinc++;
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

