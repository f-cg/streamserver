package com.founder;

import io.javalin.websocket.WsContext;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.json.JSONObject;

enum LogStreamType {
	DMKF, KF, FS;
}

public class LogStream {
	/*
	 * 每个日志流里可能包含多个查询，每个查询都应该对应一个SocketSink
	 */
	LogStreamType lsType;
	String name;
	final String initddl;
	String DMSql;
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
	DM2Kafka dm2kafka;
	private static final Pattern PP = Pattern.compile("^PATTERN");

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

	String currentDateString() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date date = new Date(System.currentTimeMillis());
		return formatter.format(date);
	}

	/**
	 * @param querySql PATTERN\ncaseKey\nvalueKey1,valueKey2
	 */
	void addQueryFreq(String querySql, String queryName) {
		String[] lines = querySql.split("\n");
		String caseKey = lines[1];
		String[] eventsKeys = lines[2].split(",");
		String timeField = lines[3];
		int queryid = queryinc++;
		Query query = new Query(caseKey, eventsKeys, timeField, queryid, queryName);
		queries.add(query);
		System.err.println("before broadcast");
		broadcast(queriesListString());
		broadcast(query.queryMetaString());
	}

	void addQuery(String querySql, String queryName) {
		// sql query
		// addSink
		// execute
		if (PP.matcher(querySql).find()) {
			addQueryFreq(querySql, queryName);
			return;
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

	ArrayList<FrequentPattern<String>> getFrequentPatterns(int qid) {
		if (lsType != LogStreamType.DMKF) {
			return null;
		}
		Query query = getquery(qid);
		String eventsSeqSql = dm2kafka.newFreqPattSql(query.caseField, query.eventsFields, query.timeField);
		try {
			ConnectDM dm = new ConnectDM();
			dm.connect();
			SqlResultData result = dm.querySql(eventsSeqSql);
			dm.disConnect();
			PrefixSpan<String> pfs = new PrefixSpan<String>(0.1);
			ArrayList<ArrayList<String>> seqs = new ArrayList<ArrayList<String>>();
			for (String[] row : result.dataMatrix) {
				seqs.add(new ArrayList<>(Arrays.asList(row[0].split("->"))));
			}
			query.freqpatt = pfs.run(seqs);
			query.freqpatt.sort(new Comparator<FrequentPattern<String>>() {
				@Override
				public int compare(FrequentPattern<String> p1, FrequentPattern<String> p2) {
					return p1.frequence - p2.frequence;
				}
			});
			return query.freqpatt;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
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
