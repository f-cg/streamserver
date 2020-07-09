
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

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;

import org.apache.flink.table.api.Table;

import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.descriptors.Csv;
import org.apache.flink.table.descriptors.Kafka;
import org.apache.flink.table.descriptors.Rowtime;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.table.types.DataType;
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

public class KafkaConnectorConsumerWindowApp{

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

		final Schema schema = new Schema()

				.field("user_id", DataTypes.INT())

				.field("product", DataTypes.STRING())

				.field("amount", DataTypes.INT())
				
			.field("ts", Types.SQL_TIMESTAMP).rowtime(new Rowtime().timestampsFromField("ts"));

		// register table via DDL with watermark,

		// the events are out of order, hence, we use 3 seconds to wait the late events
		tEnv.connect(new Kafka().version("universal") // required: valid connector versions are

				// "0.8", "0.9", "0.10", "0.11", and "universal"

				.topic("test")// required: topic name from which the table is read

				// optional: connector specific properties

				.property("zookeeper.connect", "localhost:2181")

				.property("bootstrap.servers", "localhost:9092")

				.property("group.id", "testGroup")

				// optional: output partitioning from Flink's partitions into Kafka's partitions

				.sinkPartitionerFixed() // each Flink partition ends up in at-most one Kafka partition
							// (default)

				.sinkPartitionerRoundRobin() // a Flink partition is distributed to Kafka partitions
								// round-robin

		).withFormat( // required: Kafka connector requires to specify a format,
				new Csv().fieldDelimiter(',').deriveSchema()

		).withSchema(schema).createTemporaryTable("MykfkSource");
		;// Please refer to Table Formats section for more details.

		String query = "SELECT\n" +

		"  CAST(TUMBLE_START(ts, INTERVAL '5' SECOND) AS STRING) window_start,\n" +

		"  COUNT(*) order_num,\n" +

				"  SUM(amount) total_amount,\n" +

				"  COUNT(DISTINCT product) unique_products\n" +

				"FROM MykfkSource\n" +

				"GROUP BY TUMBLE(ts, INTERVAL '5' SECOND)";

		/* query = "select DATE_FORMAT(ts, 'yyyy-MM-dd HH:00') dt, COUNT(*) AS cnt from MykfkSource GROUP BY DATE_FORMAT(ts, 'yyyy-MM-dd HH:00')"; */

		System.out.println(query);

		Table result = tEnv.sqlQuery(query);

		tEnv.toRetractStream(result, Row.class).print();

		// submit the job

		tEnv.execute("Streaming Window SQL Job");

		// should output:

		// 2019-12-12 00:00:00.000,3,10,3

		// 2019-12-12 00:00:05.000,3,6,2

	}
}
