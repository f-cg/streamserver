package com.founder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DM2Kafka extends Thread {
	private String sql;
	/* int whereStartIdx; */
	/* int whereEndIdx; */
	private String orderBy;
	private int orderByColIdx;
	private String topic;
	private String name;
	private KafkaSender kf;
	private String fieldNames[];
	private String typeNames[];
	private String wrapSelect;
	private SqlResultData result;
	private Pattern orderByPatt = Pattern.compile("order[\\s]+by[\\s]+([\\w]+)[\\W]*", Pattern.CASE_INSENSITIVE);
	/*
	 * Pattern wherePatt = Pattern.compile("[\\W]+where[\\s]",
	 * Pattern.CASE_INSENSITIVE);
	 */

	private boolean parseSql() {
		Matcher m;
		m = orderByPatt.matcher(sql);
		m.find();
		this.orderBy = m.group(m.groupCount());
		/* whereStartIdx = m.start() - 1; */
		/* whereEndIdx = whereStartIdx; */
		/* } */
		/* m = wherePatt.matcher(sql); */
		/* while (m.find()) { */
		/* whereIdx = m.end(); */
		/* } */

		return true;
	}

	private void genWrapSelect() {
		this.wrapSelect = "select " + String.join(",", fieldNames);
	}

	private String newSql(String whereCond) {
		if (whereCond != null)
			return this.wrapSelect + " from (\n" + this.sql + "\n) where " + orderBy + " > '" + whereCond
					+ "' order by " + orderBy;
		else
			return this.sql;
	}

	public String newFreqPattSql(String caseID, String[] eventsFields, String timeField) {
		String eventsFieldsAsOne = String.join(" ||',' || ", eventsFields);
		String eventSeq = "listagg(" + eventsFieldsAsOne + ",'->') WITHIN GROUP (ORDER BY " + timeField
				+ ") EVENTSSEQ";
		String selectFreqPattSql = "select " + eventSeq + "\nfrom (\n" + sql + "\n)\ngroup by " + caseID;
		return selectFreqPattSql;
	}

	public String newPredSql(String caseID, String[] eventsFields, String timeField) {
		String eventsFieldsAsOne = String.join(" ||',' || ", eventsFields);
		String selectFreqPattSql = "select " + caseID + "," + eventsFieldsAsOne + "\nfrom (\n" + sql
				+ "\n)\n order by " + timeField;
		System.err.println(selectFreqPattSql);
		return selectFreqPattSql;
	}

	public ArrayList<String> getOrderedFieldValues(String field) {
		ArrayList<String> values = new ArrayList<>();
		try {
			String selectSql = "select " + field + ", count(*) as cnt\nfrom (\n" + sql + "\n)\n group by "
					+ field + " order by cnt DESC";
			System.err.println("selectSql:\n\n");
			System.err.println(selectSql);
			System.err.println("selectSql\n\n");
			ConnectDM dm = new ConnectDM();
			dm.connect();
			SqlResultData result;
			result = dm.querySql(selectSql);
			dm.disConnect();
			for (String[] row : result.dataMatrix) {
				if (Utils.isGoodStringArray(row))
					values.add(row[0]);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return values;
	}

	private void firstPullInit() {
		this.fieldNames = result.fieldNames;
		this.typeNames = result.typeNames;
		this.orderByColIdx = Utils.findIndex(result.fieldNames, orderBy);
		if (this.orderByColIdx == -1) {
			System.err.println("Cannot find " + orderBy + " in the field names");
		}
		this.genWrapSelect();
	}

	DM2Kafka(String sql, String name) {
		this.sql = sql;
		this.name = name;
		this.topic = Utils.utf2hex(name);
		this.kf = new KafkaSender(topic);
		this.parseSql();
	}

	private void send2Kafka(ArrayList<String[]> dataMatrix) {
		this.kf.sendMatrix(dataMatrix);
	}

	public String genCreateSql() {
		String createSql = "CREATE TABLE " + name + " (\n";
		for (int i = 0; i < fieldNames.length; i++) {
			String f = fieldNames[i];
			String t = typeNames[i];
			if (t.equals("VARCHAR") || t.equals("CLOB")) {
				t = "STRING";
				if (f.equals(orderBy)) {
					t = "TIMESTAMP(3)";
				}
			}
			createSql += f + " " + t + ",\n";
		}
		createSql += "WATERMARK FOR " + orderBy + " AS " + orderBy + "\n";

		createSql += ") WITH (\n" + "'connector.type' = 'kafka',\n" + "'connector.version' = 'universal',\n";
		createSql += "'connector.topic' = '" + topic + "',\n";

		createSql += "'connector.properties.zookeeper.connect' = 'localhost:2181',\n"
				+ "'connector.properties.bootstrap.servers' = 'localhost:9092',\n"
				+ "'format.type' = 'csv'\n" + ")";
		return createSql;
	}

	private void getResult(String exeSql) throws SQLException {
		ConnectDM dm;
		dm = new ConnectDM();
		dm.connect();
		result = dm.querySql(exeSql);
		dm.disConnect();
		result.print();
	}

	public void firstRun() {
		try {
			getResult(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.firstPullInit();
	}

	@Override
	public void run() {
		String lastTime = null;
		try {
			while (true) {
				try {
					sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				getResult(newSql(lastTime));
				if (result.dataMatrix.size() > 0) {
					lastTime = result.dataMatrix
							.get(result.dataMatrix.size() - 1)[this.orderByColIdx];
					this.send2Kafka(result.dataMatrix);
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			return;
		}
	}

	public static void startExamples() {
		DM2Kafka dmk = new DM2Kafka(Constants.LOGS[0][1], Constants.LOGS[0][0]);
		dmk.firstRun();
		dmk.start();
	}

	public static void main(String[] args) {
		startExamples();
	}
}
