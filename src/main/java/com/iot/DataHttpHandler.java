package com.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.io.IOException;

import static com.iot.DataJsonSchema.DATA;
import static com.iot.DataJsonSchema.DEVICE_ID;
import static com.iot.KafkaTopics.DATA_TOPIC;

public class DataHttpHandler implements HttpHandler {
	private final MessageQueueSender sender;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public DataHttpHandler(MessageQueueSender sender) {
		this.sender = sender;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if (!exchange.getRequestMethod().equals(Methods.POST)) {
			exchange.getResponseHeaders().put(Headers.ALLOW, "POST");
			exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
			exchange.setPersistent(false);
			exchange.getResponseSender().close();
			return;
		}

		System.out.println(exchange.getRequestHeaders().get(Headers.AUTHORIZATION).getFirst());
		exchange.getRequestReceiver().receiveFullBytes(this::processMessage,
				(exch, e) -> sendServerError(exchange));
	}

	private void processMessage(HttpServerExchange exchange, byte[] message) {
		exchange.dispatch(); // do not end exchange when this method returns, because saving to kafka is async


		// TODO: validate token

		JsonNode json;
		try {
			json = objectMapper.readTree(message);
		} catch (IOException e) {
			sendRequestError(exchange);
			return;
		}

		JsonNode deviceId = json.get(DEVICE_ID);
		if (deviceId.isNull()) {
			sendRequestError(exchange);
			return;
		}

		JsonNode data = json.get(DATA);
		sender.send(DATA_TOPIC, deviceId.asText(), data.toString())
				.thenRun(() -> exchange.getResponseSender().close())
				.exceptionally((throwable -> sendServerError(exchange)));
	}

	private Void sendRequestError(HttpServerExchange exchange) {
		return sendError(exchange, StatusCodes.BAD_REQUEST);
	}

	private Void sendServerError(HttpServerExchange exchange) {
		return sendError(exchange, StatusCodes.INTERNAL_SERVER_ERROR);
	}

	private Void sendError(HttpServerExchange exchange, int code) {
		exchange.setStatusCode(code);
		exchange.getResponseSender().close();
		return null;
	}
}
