package com.iot.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.auth.AuthenticationService;
import com.iot.token.JwtTokenService;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.iot.http.HttpUtils.extractQueryParameter;
import static com.iot.http.HttpUtils.sendError;
import static com.iot.http.HttpUtils.sendServerError;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

public class AuthenticationHttpHandler implements HttpHandler {
	public static final String DEVICE_ID = "deviceId";
	public static final String LOGIN = "login";
	public static final String PASSWORD = "password";

	private final AuthenticationService authenticationService;
	private final JwtTokenService tokenService;
	private final ObjectMapper objectMapper;

	public AuthenticationHttpHandler(AuthenticationService authenticationService, JwtTokenService tokenService,
	                                 ObjectMapper objectMapper) {
		this.authenticationService = authenticationService;
		this.tokenService = tokenService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		String login = extractQueryParameter(LOGIN, queryParameters);
		String password = extractQueryParameter(PASSWORD, queryParameters);

		if (login == null || password == null) {
			sendError(exchange, "Login credentials are not present", UNAUTHORIZED);
			return;
		}

		exchange.dispatch();
		CompletionStage<Boolean> authenticationFuture = authenticationService.authenticate(login, password);
		authenticationFuture
				.thenAccept((authenticated) -> {
					if (authenticated) {
						createAndSendToken(login, exchange);
						return;
					}

					sendError(exchange, "Authentication credentials are invalid", UNAUTHORIZED);
				})
				.exceptionally(throwable -> sendServerError(exchange));
	}

	private void createAndSendToken(String login, HttpServerExchange exchange) {
		Map<String, Object> tokenClaims = new HashMap<>();
		tokenClaims.put(DEVICE_ID, login);
		String token = tokenService.createToken(tokenClaims);
		String json = objectMapper.createObjectNode().put("token", token).toString();
		exchange.getResponseSender().send(json);
	}
}
