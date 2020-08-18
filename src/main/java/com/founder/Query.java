package com.founder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.json.JSONObject;

enum QueryType {
	FlinkSQL, FrequentPattern;
}

public class Query extends Thread {
	String qsql;
	int qid;
	String qname;
	List<String> fieldNames;
	ArrayList<Object> result = new ArrayList<>();
	StreamTableEnvironment tEnv;
	String caseField = null;
	String[] eventsFields = null;
	String timeField = null;
	final QueryType qtype;

	Query(String qsql, TableSchema schema, int qid, String qname, StreamTableEnvironment tEnv) {
		qtype = QueryType.FlinkSQL;
		this.qsql = qsql;
		this.fieldNames = Arrays.asList(schema.getFieldNames());
		this.qid = qid;
		this.qname = qname;
		this.tEnv = tEnv;
	}

	Query(String caseKey, String[] eventsKeys, String timeField, int qid, String qname) {
		qtype = QueryType.FrequentPattern;
		this.caseField = caseKey;
		this.eventsFields = eventsKeys;
		this.timeField = timeField;
		this.qid = qid;
		this.qname = qname;
	}

	String queryMetaString() {
		JSONObject js = new JSONObject();
		js.put("type", "queryMeta");
		js.put("qtype", qtype);
		js.put("queryId", qid);
		js.put("queryName", qname);
		if (qtype == QueryType.FlinkSQL) {
			js.put("fieldNames", fieldNames);
			js.put("querySql", qsql);
		} else if (qtype == QueryType.FrequentPattern) {
			js.put("caseField", caseField);
			js.put("eventsFields", eventsFields);
		}
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
