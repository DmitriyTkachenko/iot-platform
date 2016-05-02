package com.iot.mq;

import java.util.concurrent.CompletionStage;

public interface MessageQueueSender {
	CompletionStage<Void> send(String topic, String key, String data);
}
