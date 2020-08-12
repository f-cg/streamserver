package com.founder;

import java.sql.SQLException;
import java.util.ArrayList;

public class DM2Kafka extends Thread {
	String selectSql;
	String whereCond;
	String orderBy;
	String topic;
	KafkaSender kf;

	DM2Kafka(String selectSql, String whereCond, String orderBy, String topic) {
		this.selectSql = selectSql;
		this.whereCond = whereCond;
		this.orderBy = orderBy;
		this.topic = topic;
		this.kf = new KafkaSender(topic);
	}

	private void send2Kafka(ArrayList<String[]> dataMatrix) {
		this.kf.sendMatrix(dataMatrix);
	}

	@Override
	public void run() {
		String lastTime = null;
		while (true) {
			String whereSql = "";
			if (lastTime != null) {
				if (whereCond != "") {
					whereSql = "where " + this.whereCond + " and " + orderBy + ">'" + lastTime
							+ "'";
				} else {
					whereSql = "where " + orderBy + "=" + lastTime;
				}
			} else {// 尚未得到过数据
				if (whereCond != "") {
					whereSql = "where " + this.whereCond;
				}
			}
			String orderBySql = "order by " + orderBy;
			String sql = String.join("\n", new String[] { selectSql, whereSql, orderBySql });
			System.out.println(sql);
			ConnectDM dm;
			try {
				dm = new ConnectDM();
				dm.connect();
				SqlResultData result = dm.querySql(sql);
				dm.disConnect();
				result.print();
				int idx = Utils.findIndex(result.fieldNames, orderBy);
				if (idx == -1) {
					System.err.println("Cannot find " + orderBy + " in the field names");
				}
				if (result.dataMatrix.size() > 0) {
					lastTime = result.dataMatrix.get(result.dataMatrix.size() - 1)[idx];
					this.send2Kafka(result.dataMatrix);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void startExamples() {
		String selectSql = "select BIZID, DATAID, OPERATIONNAME, OPERATIONDESC, OPERATIONUSERID, OPERATIONTIME, OPERATIONUSERNAME, DUTY\n"
				+ "from OA.ZT_BIZOBJECTLOG, UIM.APP_USER";
		String whereCond = "OPERATIONUSERID=LOGINNAME";
		String orderBy = "OPERATIONTIME";
		String topic = "BIZLOG";
		DM2Kafka dmk = new DM2Kafka(selectSql, whereCond, orderBy, topic);
		dmk.start();
	}

	public static void main(String[] args) {
		startExamples();
	}
}
