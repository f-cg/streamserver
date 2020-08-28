package com.founder;

import java.util.ArrayList;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaSender {
	Producer<String, String> producer;
	String topic;
	int timeIntervalDefault;

	KafkaSender(String topic, int timeIntervalDefault) {

		Properties props = new Properties();

		props.put("bootstrap.servers", "127.0.0.1:9092");

		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		props.put("max.block.ms", "1000");

		this.producer = new KafkaProducer<>(props);
		this.topic = topic;
		this.timeIntervalDefault = timeIntervalDefault;
	}

	KafkaSender(String topic) {
		this(topic, Constants.KAFKA_SENDER_TIME_INTERVAL);
	}

	public void send(ArrayList<String> arrayList, int timeInterval) {
		for (String line : arrayList) {
			try {
				Thread.sleep(timeInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			println(line);

			// 创建 ProducerRecord 可以指定 topic、partition、key、value，其中 partition 和 key 是可选的

			ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, line);

			// 只管发送消息，不管是否发送成功
			producer.send(record);
		}
	}

	public void send(ArrayList<String> arrayList) {
		send(arrayList, timeIntervalDefault);
	}

	public void sendMatrix(ArrayList<String[]> dataMatrix, int timeInterval) {
		for (String[] row : dataMatrix) {
			try {
				Thread.sleep(timeInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String line = String.join(",", row);
			println(line);
			ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, line);
			producer.send(record);
		}
	}

	private void println(String line) {
		if (Constants.KFSENDPRINT)
			System.out.println("KafkaSender send: " + line);
	}

	public void sendMatrix(ArrayList<String[]> dataMatrix) {
		sendMatrix(dataMatrix, timeIntervalDefault);
	}

	public void close() {
		producer.close();
	}

}
