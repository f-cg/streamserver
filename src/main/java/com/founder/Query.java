package com.founder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.json.JSONObject;

enum QueryType {
	FlinkSQL, FrequentPattern, Predict;
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
	JSONObject frontInterest = null;
	/* final ChartType defaultctype; */
	TreeMap<String, ArrayList<String>> eventKeysValues;
	LogStream ls;
	EventPredictor epr;

	Query(LogStream ls, String qsql, TableSchema schema, int qid, String qname, StreamTableEnvironment tEnv,
			JSONObject frontObject) {
		this.ls = ls;
		qtype = QueryType.FlinkSQL;
		this.qsql = qsql;
		this.fieldNames = Arrays.asList(schema.getFieldNames());
		this.qid = qid;
		this.qname = qname;
		this.tEnv = tEnv;
		this.frontInterest = frontObject;
	}

	Query(LogStream ls, String qsql, String caseKey, String[] eventsKeys, String timeField, int qid, String qname,
			QueryType qtype, JSONObject frontObject) {
		this.ls = ls;
		this.qtype = qtype;
		this.frontInterest = frontObject;
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

	void refresh() {
		if (this.qtype == QueryType.FrequentPattern) {
			refreshFrequentPatterns();
		} else {
			refreshPredict();
		}
	}

	private void refreshFrequentPatterns() {
		if (this.qtype != QueryType.FrequentPattern) {
			return;
		}
		String eventsSeqSql = this.ls.dm2kafka.newFreqPattSql(this.caseField, this.eventsFields,
				this.timeField);
		try {
			ConnectDM dm = new ConnectDM();
			dm.connect();
			SqlResultData result = dm.querySql(eventsSeqSql);
			dm.disConnect();
			PrefixSpan<String> pfs = new PrefixSpan<String>(0.02, 0, 1);
			ArrayList<ArrayList<String>> seqs = new ArrayList<ArrayList<String>>();
			for (String[] row : result.dataMatrix) {
				if (Utils.isGoodStringArray(row))
					seqs.add(new ArrayList<>(Arrays.asList(row[0].split("->"))));
			}
			ArrayList<FrequentPattern<String>> freqpatt = pfs.run(seqs);
			ArrayList<Object> resultFreq = new ArrayList<Object>();
			for (FrequentPattern<String> p : freqpatt) {
				ArrayList<Object> row = new ArrayList<Object>();
				row.add(String.join("->", p.pattern));
				row.add(p.frequence);
				resultFreq.add(row);
			}
			this.result = resultFreq;
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	private void refreshPredictEventKeyValues() {
		this.eventKeysValues = new TreeMap<String, ArrayList<String>>();
		for (String field : this.eventsFields) {
			ArrayList<String> values = this.ls.dm2kafka.getOrderedFieldValues(field);
			this.eventKeysValues.put(field, values);
		}
	}

	private void refreshPredict() {
		if (this.qtype != QueryType.Predict) {
			return;
		}
		this.refreshPredictEventKeyValues();
		String eventsSeqSql = this.ls.dm2kafka.newPredSql(this.caseField, this.eventsFields, this.timeField);
		try {
			ConnectDM dm = new ConnectDM();
			dm.connect();
			SqlResultData result = dm.querySql(eventsSeqSql);
			dm.disConnect();
			List<List<String>> seqs = new ArrayList<>();
			for (String[] row : result.dataMatrix) {
				if (Utils.isGoodStringArray(row))
					seqs.add(new ArrayList<>(Arrays.asList(row)));
			}
			this.epr = new EventPredictor();
			this.epr.train(seqs);
			for (List<String> seq : seqs) {
				seq.remove(0);
			}
			/* List<EventProb> eventProbs = epr.predictBeautifulWithProb(seqs); */
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	public void predictSeqFeedback(ArrayList<ArrayList<String>> seq) {
		List<EventProb> eventProbs = epr.predictSeqWithProbs(seq);
		System.err.println("predicted size:" + eventProbs.size());
		ArrayList<Object> resultPred = new ArrayList<Object>();
		for (EventProb p : eventProbs) {
			ArrayList<Object> row = new ArrayList<Object>();
			row.add(String.join("->", p.happened));
			row.add(p.pred);
			row.add(p.prob);
			resultPred.add(row);
		}
		this.result = resultPred;
	}

	String queryMetaString() {
		JSONObject js = new JSONObject();
		js.put("type", "queryMeta");
		js.put("qtype", qtype);
		js.put("frontInterest", this.frontInterest);
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
			js.put("eventKeysValues", eventKeysValues);
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
			this.tEnv.execute("Streaming Window SQL Job");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
