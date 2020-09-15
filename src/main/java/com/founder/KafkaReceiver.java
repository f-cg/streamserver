package com.founder;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;

public class KafkaReceiver {
	private KafkaConsumer<String, String> consumer;

	KafkaReceiver() {
		Properties props = new Properties();
		props.put("bootstrap.servers", "127.0.0.1:9092");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("max.block.ms", "1000");

		consumer = new KafkaConsumer<String, String>(props);
	}

	public String[] getTopics() {
		return Arrays.stream(consumer.listTopics().keySet().toArray()).toArray(String[]::new);
	}

	public void close() {
		consumer.close();
	}

}
