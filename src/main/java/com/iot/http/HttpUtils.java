package com.iot.http;

import io.undertow.server.HttpServerExchange;

import static io.undertow.util.StatusCodes.BAD_REQUEST;
import static io.undertow.util.StatusCodes.INTERNAL_SERVER_ERROR;

public class HttpUtils {
	public static Void sendRequestError(HttpServerExchange exchange) {
		return sendError(exchange, BAD_REQUEST);
	}

	public static Void sendServerError(HttpServerExchange exchange) {
		return sendError(exchange, INTERNAL_SERVER_ERROR);
	}

	public static Void sendError(HttpServerExchange exchange, int code) {
		return sendError(exchange, "", code);
	}

	public static Void sendError(HttpServerExchange exchange, String body, int code) {
		exchange.setStatusCode(code);
		exchange.getResponseSender().send(body);
		return null;
	}
}
