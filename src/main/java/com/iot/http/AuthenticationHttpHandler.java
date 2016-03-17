package com.iot.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.AuthenticationService;
import com.iot.token.JwtTokenService;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.iot.DataSchema.DEVICE_ID;
import static com.iot.DataSchema.LOGIN;
import static com.iot.DataSchema.PASSWORD;
import static com.iot.http.HttpUtils.sendError;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;

public class AuthenticationHttpHandler implements HttpHandler {
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
		if (!exchange.getRequestMethod().equals(Methods.GET)) {
			exchange.getResponseHeaders().put(Headers.ALLOW, "GET");
			exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
			exchange.setPersistent(false);
			exchange.getResponseSender().close();
			return;
		}

		Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
		Optional<String> login = extractQueryParameter(LOGIN, queryParameters);
		Optional<String> password = extractQueryParameter(PASSWORD, queryParameters);

		if (!login.isPresent() || !password.isPresent()) {
			sendError(exchange, "Login credentials are not present", UNAUTHORIZED);
			return;
		}

		boolean authenticated = authenticationService.authenticate(login.get(), password.get());

		if (!authenticated) {
			sendError(exchange, "Authentication failed", UNAUTHORIZED);
			return;
		}

		Map<String, Object> tokenClaims = new HashMap<>();
		tokenClaims.put(DEVICE_ID, login.get());
		String token = tokenService.createToken(tokenClaims);
		String json = objectMapper.createObjectNode().put("token", token).toString();

		exchange.getResponseSender().send(json);
	}

	private static Optional<String> extractQueryParameter(String parameter, Map<String, Deque<String>> queryParameters) {
		Deque<String> values = queryParameters.get(parameter);

		if (values == null || values.isEmpty())
			return Optional.empty();

		return Optional.of(values.getFirst());
	}
}
