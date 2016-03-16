package com.iot;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class KafkaSender implements MessageQueueSender {
	private final Producer<String, String> producer;

	public KafkaSender() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", "localhost:9092");
		properties.put("acks", 1);
		producer = new KafkaProducer<>(properties, new StringSerializer(), new StringSerializer());
	}

	public CompletableFuture<Void> send(String topic, String key, String data) {
		ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, data);
		CompletableFuture<Void> future = new CompletableFuture<>();
		producer.send(record, (metadata, exception) -> {
			if (exception != null) {
				future.completeExceptionally(exception);
			} else {
				future.complete(null);
			}
		});
		return future;
	}
}
