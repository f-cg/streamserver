/*

 * Licensed to the Apache Software Foundation (ASF) under one

 * or more contributor license agreements.  See the NOTICE file

 * distributed with this work for additional information

 * regarding copyright ownership.  The ASF licenses this file

 * to you under the Apache License, Version 2.0 (the

 * "License"); you may not use this file except in compliance

 * with the License.  You may obtain a copy of the License at

 *

 *     http://www.apache.org/licenses/LICENSE-2.0

 *

 * Unless required by applicable law or agreed to in writing, software

 * distributed under the License is distributed on an "AS IS" BASIS,

 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 * See the License for the specific language governing permissions and

 * limitations under the License.

 */

package com.founder;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.table.api.EnvironmentSettings;

import org.apache.flink.table.api.Table;

import org.apache.flink.table.api.java.StreamTableEnvironment;

import org.apache.flink.types.Row;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * 
 * Simple example for demonstrating the use of SQL in Java.
 *
 * 
 * 
 * <p>
 * Usage: {@code ./bin/flink run ./examples/table/StreamWindowSQLExample.java}
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

public class KafkaConnectorConsumerTableSQLApp{

	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("/tmp/print.txt")), true));

		// set up execution environment

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// use blink planner in streaming mode,

		// because watermark statement is only available in blink planner.

		EnvironmentSettings settings = EnvironmentSettings.newInstance()

				.useBlinkPlanner()

				.inStreamingMode()

				.build();

		StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);

		// register table via DDL with watermark,

		// the events are out of order, hence, we use 3 seconds to wait the late events
		String ddl = "CREATE TABLE MyUserTable (\n" + "wordc STRING\n" + ") WITH (\n"
				+ "'connector.type' = 'kafka',\n" + "'connector.version' = 'universal',\n"
				+ "'connector.topic' = 'test',\n"
				+ "'connector.properties.zookeeper.connect' = 'localhost:2181',\n"
				+ "'connector.properties.bootstrap.servers' = 'localhost:9092',\n"
				+ "'connector.properties.group.id' = 'testGroup',\n" + "'format.type' = 'csv'\n" + ")";
		tEnv.sqlUpdate(ddl);

		// run a SQL query on the table and retrieve the result as a new Table
		String query = "select * from MyUserTable";

		/* String query = "SELECT\n" + */
                /*  */
		/*                 "  CAST(TUMBLE_START(ts, INTERVAL '5' SECOND) AS STRING) window_start,\n" + */
                /*  */
		/*                 "  COUNT(*) order_num,\n" + */
                /*  */
		/*                 "  SUM(amount) total_amount,\n" + */
                /*  */
		/*                 "  COUNT(DISTINCT product) unique_products\n" + */
                /*  */
		/*                 "FROM orders\n" + */
                /*  */
		/*                 "GROUP BY TUMBLE(ts, INTERVAL '5' SECOND)"; */

		Table result = tEnv.sqlQuery(query);

		tEnv.toAppendStream(result, Row.class).print();

		// submit the job

		tEnv.execute("Streaming Window SQL Job");

		// should output:

		// 2019-12-12 00:00:00.000,3,10,3

		// 2019-12-12 00:00:05.000,3,6,2

	}
}
