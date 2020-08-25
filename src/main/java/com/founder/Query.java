package com.founder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableSchema;
/* import org.apache.flink.table.api.bridge.java.StreamTableEnvironment; */
import org.json.JSONObject;

enum ChartType {
	graph, table;
}

enum QueryType {
	FlinkSQL, FrequentPattern, Predict;
}

public class Query extends Thread {
	String qsql;
	int qid;
	String qname;
	List<String> fieldNames;
	ArrayList<Object> result = new ArrayList<>();
	StreamExecutionEnvironment env;
	String caseField = null;
	String[] eventsFields = null;
	String timeField = null;
	final QueryType qtype;
	final ChartType defaultctype;

	Query(String qsql, TableSchema schema, int qid, String qname, StreamExecutionEnvironment env,
			ChartType defaultctype) {
		qtype = QueryType.FlinkSQL;
		this.qsql = qsql;
		this.fieldNames = Arrays.asList(schema.getFieldNames());
		this.qid = qid;
		this.qname = qname;
		this.env = env;
		if (defaultctype != null)
			this.defaultctype = defaultctype;
		else
			this.defaultctype = ChartType.graph;
	}

	Query(String qsql, String caseKey, String[] eventsKeys, String timeField, int qid, String qname,
			QueryType qtype, ChartType defaultctype) {
		this.qtype = qtype;
		this.defaultctype = defaultctype;
		this.qsql = qsql;
		this.caseField = caseKey;
		this.eventsFields = eventsKeys;
		this.timeField = timeField;
		this.qid = qid;
		this.qname = qname;
		if (qtype == QueryType.FrequentPattern) {
			this.fieldNames = Arrays.asList(new String[] { "事件序列", "频次" });
		} else if (qtype == QueryType.Predict) {
			this.fieldNames = Arrays.asList(new String[] { "事件序列", "预测", "概率" });
		} else {
			System.err.println("no such query type" + qtype);
			System.exit(1);
		}
	}

	String queryMetaString() {
		JSONObject js = new JSONObject();
		js.put("type", "queryMeta");
		js.put("qtype", qtype);
		js.put("defaultctype", defaultctype);
		js.put("queryId", qid);
		js.put("queryName", qname);
		js.put("fieldNames", fieldNames);
		js.put("querySql", qsql);
		if (qtype == QueryType.FlinkSQL) {
		} else if (qtype == QueryType.FrequentPattern) {
			js.put("caseField", caseField);
			js.put("eventsFields", eventsFields);
		} else if (qtype == QueryType.Predict) {
			js.put("caseField", caseField);
			js.put("eventsFields", eventsFields);
		} else {
			System.err.println("no such query type" + qtype);
			System.exit(1);
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
			this.env.execute("Streaming Window SQL Job");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
