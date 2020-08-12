package com.founder;

import java.util.Map;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;

public class KafkaReceiver {
	KafkaConsumer<String, String> consumer;
	int timeIntervalDefault;

	KafkaReceiver() {
		Properties props = new Properties();
		props.put("bootstrap.servers", "127.0.0.1:9092");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("max.block.ms", "1000");

		consumer = new KafkaConsumer<String, String>(props);
	}

	public Map<String, List<PartitionInfo>> getTopics() {
		return consumer.listTopics();
	}

	public void close() {
		consumer.close();
	}

}
