package com.iot.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.mq.MessageQueueSender;
import com.iot.token.JwtTokenService;
import com.iot.token.TokenParseException;
import com.iot.token.TokenVerificationException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.util.Map;

import static com.iot.http.AuthenticationHttpHandler.DEVICE_ID;
import static com.iot.http.HttpUtils.*;
import static com.iot.mq.KafkaTopics.DATA_TOPIC;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

public class DataHttpHandler implements HttpHandler {
	public static final String DATA = "data";

	private final MessageQueueSender sender;
	private final JwtTokenService tokenService;
	private final ObjectMapper objectMapper;

	public DataHttpHandler(MessageQueueSender sender, JwtTokenService tokenService, ObjectMapper objectMapper) {
		this.sender = sender;
		this.tokenService = tokenService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		exchange.getRequestReceiver().receiveFullBytes(this::processMessage, (exch, e) -> sendServerError(exchange));
	}

	private void processMessage(HttpServerExchange exchange, byte[] message) {
		String authorizationHeader = getAuthorizationHeader(exchange);
		if (authorizationHeader == null) {
			sendError(exchange, "No 'Authorization' header", UNAUTHORIZED);
			return;
		}

		Map<String, Object> tokenPayload;
		try {
			tokenPayload = tokenService.verifyToken(authorizationHeader);
		} catch (TokenParseException e) {
			sendError(exchange, "Token could not be parsed", UNAUTHORIZED);
			return;
		} catch (TokenVerificationException e) {
			sendError(exchange, "Token could not be verified", UNAUTHORIZED);
			return;
		}

		JsonNode json;
		try {
			json = objectMapper.readTree(message);
		} catch (IOException e) {
			sendRequestError(exchange);
			return;
		}

		JsonNode data = json.get(DATA);
		String deviceId = getDeviceId(tokenPayload);
		if (deviceId == null || data == null) {
			sendRequestError(exchange);
			return;
		}

		exchange.dispatch(); // do not end exchange when this method returns, because saving to kafka is async
		sender.send(DATA_TOPIC, deviceId, data.toString())
				.thenRun(() -> exchange.getResponseSender().close())
				.exceptionally((throwable -> sendServerError(exchange)));
	}

	private static String getAuthorizationHeader(HttpServerExchange exchange) {
		HeaderValues authorizationHeaderValues = exchange.getRequestHeaders().get(Headers.AUTHORIZATION);
		return authorizationHeaderValues == null ? null : authorizationHeaderValues.getFirst();
	}

	private static String getDeviceId(Map<String, Object> tokenPayload) {
		if (tokenPayload == null)
			return null;

		Object o = tokenPayload.get(DEVICE_ID);

		if (o instanceof String) {
			String deviceId = (String) o;
			return deviceId.isEmpty() ? null : deviceId;
		}

		return null;
	}
}
