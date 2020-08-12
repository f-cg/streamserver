package com.founder;

import org.apache.kafka.common.PartitionInfo;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ExecKafka {
	static final String workingDir = System.getProperty("user.dir");
	static final String kafkaDir = workingDir + "/kafka_2.12-2.5.0";
	static final String zkArg = String.format("%s/config/zookeeper.properties", kafkaDir);
	static final String kafkaArg = String.format("%s/config/server.properties", kafkaDir);

	public static boolean execCmd(String[] binArgs, String sucessId) throws IOException {
		String line;
		Process process = new ProcessBuilder(binArgs).start();
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while ((line = br.readLine()) != null) {
			System.out.println(line);
			if (line.contains(sucessId)) {
				System.out.println(binArgs[0] + " executed successfully");
				return true;
			}
		}
		System.out.println(binArgs[0] + " executing failed");
		return false;
	}

	public static boolean createTopics(String... topics) throws IOException {
		String sucessId = "Created topic";
		String topicBin = String.format("%s/bin/kafka-topics.sh", kafkaDir);
		String[] topicBinArgs = { topicBin, "--create", "--bootstrap-server", "localhost:9092",
				"--replication-factor", "1", "--partitions", "1", "--topic", "topic_here" };
		int topicIdx = topicBinArgs.length - 1;

		KafkaReceiver consumer = new KafkaReceiver();
		Map<String, List<PartitionInfo>> oldTopics = consumer.getTopics();
		for (String topic : topics) {
			if (oldTopics.containsKey(topic)) {
				continue;
			}
			topicBinArgs[topicIdx] = topic;
			System.out.println(String.join(" ", topicBinArgs));
			if (!execCmd(topicBinArgs, sucessId)) {
				return false;
			}
		}
		return true;
	}

	public static boolean execKafka() throws IOException {
		String zkStartBin = String.format("%s/bin/zookeeper-server-start.sh", kafkaDir);
		String sucessId = "org.apache.zookeeper.server.ContainerManager";
		if (!execCmd(new String[] { zkStartBin, zkArg }, sucessId)) {
			return false;
		}

		String kfStartBin = String.format("%s/bin/kafka-server-start.sh", kafkaDir);
		sucessId = "started (kafka.server.KafkaServer)";
		if (!execCmd(new String[] { kfStartBin, kafkaArg }, sucessId)) {
			return false;
		}

		return createTopics("BIZLOG");
	}

	public static void stopKafka() {
		String zkStopBin = String.format("%s/bin/zookeeper-server-stop.sh", kafkaDir);
		try {
			new ProcessBuilder(zkStopBin, zkArg).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String kfStopBin = String.format("%s/bin/kafka-server-stop.sh", kafkaDir);
		try {
			new ProcessBuilder(kfStopBin, kafkaArg).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	ExecKafka() {
	}
}
