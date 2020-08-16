package com.founder;

import java.util.regex.Pattern;

/**
 * Constants
 */
public class Constants {

	public static final int UNISERVERPORT = 8848;
	public static final int JAVALINWEBPORT = 7776;
	public static final String UNISERVERHOST = "127.0.0.1";
	public static final int parallel = 1;
	public static final String dmUrl = "jdbc:dm://162.105.146.37:5326";
	public static final String dmUserName = "FOUNDER";
	public static final String dmPassword = "fdOAondameng";
	public static final String dmJDBC = "dm.jdbc.driver.DmDriver";
	public static final boolean EXECPRINT = false;
	public static final boolean DMRESULTPRINT = false;
	public static final boolean KFSENDPRINT = true;
	public static final boolean DMLINEPRINT = true;
	public static final boolean HTTPPATHPRINT = true;
	public static final String BIZLOGDDL = "CREATE TABLE BIZLOG (\n" + "BIZID INT,\n" + "DATAID STRING,\n"
			+ "OPERATIONNAME STRING,\n" + "OPERATIONDESC STRING,\n" + "OPERATIONUSERID STRING,\n"
			+ "OPERATIONTIME TIMESTAMP(3),\n" + "OPERATIONUSERNAME STRING,\n" + "DUTY STRING,\n"
			+ "WATERMARK FOR OPERATIONTIME AS OPERATIONTIME\n" + ") WITH (\n"
			+ "'connector.type' = 'kafka',\n" + "'connector.version' = 'universal',\n"
			+ "'connector.topic' = 'BIZLOG',\n"
			+ "'connector.properties.zookeeper.connect' = 'localhost:2181',\n"
			+ "'connector.properties.bootstrap.servers' = 'localhost:9092',\n" + "'format.type' = 'csv'\n"
			+ ")\n";
	public static final String BIZLOGNAME = "BIZLOG";
	public static final String BIZLOG_QUERY1 = "SELECT CAST(TUMBLE_START(OPERATIONTIME, INTERVAL '10' MINUTE) AS STRING) window_start,\n"
			+ "COUNT(*) operation_count\n" + "FROM BIZLOG\n" + "WHERE OPERATIONNAME='新增'\n"
			+ "GROUP BY TUMBLE(OPERATIONTIME, INTERVAL '10' MINUTE)\n";
	public static final String BIZLOG_QUERY1_NAME = "最近10分钟新增次数";
	public static final String BIZLOG_QUERY2 = "PATTERN\nDATAID\nOPERATIONNAME\nOPERATIONTIME";
	public static final String BIZLOG_QUERY2_NAME = "Frequent Patterns";

	public static final String DMSQL1 = "select BIZID, DATAID, OPERATIONNAME, OPERATIONDESC, OPERATIONUSERID, OPERATIONTIME, OPERATIONUSERNAME, DUTY\n"
			+ "from OA.ZT_BIZOBJECTLOG, UIM.APP_USER\n"
			+ "where OPERATIONUSERID=LOGINNAME and OPERATIONNAME='新增'\n" + "order by OPERATIONTIME";
	public static final String[][] LOGS = new String[][] { { BIZLOGNAME, DMSQL1 } };
	public static final String[][] QUERIES = new String[][] { { BIZLOGNAME, BIZLOG_QUERY1, BIZLOG_QUERY1_NAME },
			{ BIZLOGNAME, BIZLOG_QUERY2, BIZLOG_QUERY2_NAME } };

	public static void main(String[] args) {
		System.out.println("LOGS:");
		for (String[] log : LOGS) {
			for (String litem : log) {
				System.out.println(litem);
			}
		}
		System.out.println("\n");
		System.out.println("QUERIES:");
		for (String[] query : QUERIES) {
			for (String qitem : query) {
				System.out.println(qitem);
			}
		}
		System.out.println("\n");
	}
}
