package com.founder;

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
	public static final boolean KFSENDPRINT = false;
	public static final boolean DMLINEPRINT = false;
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
			+ "COUNT(*) operation_count\n" + "FROM BIZLOG\n" + "WHERE OPERATIONNAME='公文起草'\n"
			+ "GROUP BY TUMBLE(OPERATIONTIME, INTERVAL '10' MINUTE)\n";
	public static final String BIZLOG_QUERY1_NAME = "最近10分钟新增次数";

	public static final String[][] LOGS = new String[][] { { BIZLOGNAME, BIZLOGDDL } };
	public static final String[][] QUERIES = new String[][] { { BIZLOGNAME, BIZLOG_QUERY1, BIZLOG_QUERY1_NAME } };
}
