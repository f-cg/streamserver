package com.founder;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.table.api.Table;


import org.apache.flink.types.Row;

import org.apache.flink.util.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * 
 * Simple example for demonstrating the use of SQL in Java.
 *
 * 
 * 
 * <p>
 * Usage: {@code ./bin/flink run ./examples/table/StreamWindowSQLExample.jar}
 *
 * 
 * 
 * <p>
 * This example shows how to:
 * 
 * - Register a table via DDL
 * 
 * - Declare an event time attribute in the DDL
 * 
 * - Run a streaming window aggregate on the registered table
 * 
 */

public class CEPtest {
	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("/tmp/print.txt")), true));

		// set up execution environment

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		EnvironmentSettings settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();

		StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);

		// write source data into temporary file and get the absolute path

		String path = "/tmp/datahub_cards.csv";

		// register table via DDL with watermark,

		// the events are out of order, hence, we use 3 seconds to wait the late events

		String ddl = "CREATE TABLE datahub_stream (\n" +

				" ts TIMESTAMP(3),\n" +

				" card_id VARCHAR,\n" +

				" location VARCHAR,\n" +

				" `action` VARCHAR,\n" +

				"WATERMARK FOR ts AS ts - INTERVAL '1' SECOND\n"+

				") WITH (\n" +

				"  'connector.type' = 'filesystem',\n" +

				"  'connector.path' = '" + path + "',\n" +

				"  'format.type' = 'csv'\n" +

				")";
		System.out.println(ddl);

		tEnv.sqlUpdate(ddl);

		String query = 	"select `start_timestamp`, `end_timestamp`, card_id, `event`\n"+
			"from datahub_stream\n"+
			"MATCH_RECOGNIZE (\n"+
			"PARTITION BY card_id\n"+
			"ORDER BY ts\n"+
			"MEASURES\n"+
			"e2.`action` as `event`,\n"+
			"e1.ts as `start_timestamp`,\n"+
			"LAST(e2.ts) as `end_timestamp`\n"+
			"ONE ROW PER MATCH\n"+
			"AFTER MATCH SKIP TO NEXT ROW\n"+
			"PATTERN (e1 e2) WITHIN INTERVAL '10' MINUTE\n"+
			"DEFINE\n"+
			"e1 as e1.action = 'Consumption',\n"+
			"e2 as e2.action = 'Consumption' and e2.location <> e1.location\n"+
			")";

		/* String query="select * from datahub_stream"; */

		Table result = tEnv.sqlQuery(query);

		tEnv.toAppendStream(result, Row.class).print();

		env.execute("Streaming Window SQL Job");

	}

}
