package com.founder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ExecKafka {
	private static final String workingDir = System.getProperty("user.dir");
	private static final String kafkaDir = workingDir + "/kafka_2.12-2.5.0";
	private static final String zkArg = String.format("%s/config/zookeeper.properties", kafkaDir);
	private static final String kafkaArg = String.format("%s/config/server.properties", kafkaDir);

	private static boolean execCmd(String[] binArgs, String sucessId) throws IOException {
		String line;
		Process process = new ProcessBuilder(binArgs).start();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			while ((line = br.readLine()) != null) {
				println(line);
				if (line.contains(sucessId)) {
					System.out.println(binArgs[0] + " executed successfully");
					return true;
				}
			}
		} catch (Exception e) {
			throw e;
		}
		System.out.println(binArgs[0] + " executing failed");
		return false;
	}

	private static void println(String line) {
		if (Constants.EXECPRINT)
			System.out.println(line);
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
		return true;
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
