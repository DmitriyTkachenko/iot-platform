package com.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.http.AuthenticationHttpHandler;
import com.iot.http.DataHttpHandler;
import com.iot.token.JwtTokenService;
import io.undertow.Undertow;

import static io.undertow.Handlers.path;

public class HttpServer {
	public static void main(final String[] args) {
		JwtTokenService tokenService = new JwtTokenService();
		ObjectMapper objectMapper = new ObjectMapper();
		AuthenticationService authenticationService = new AuthenticationServiceImpl();
		DataHttpHandler dataHttpHandler = new DataHttpHandler(new KafkaSender(), tokenService, objectMapper);
		AuthenticationHttpHandler authenticationHttpHandler =
				new AuthenticationHttpHandler(authenticationService, tokenService, objectMapper);

		Undertow server = Undertow.builder()
				.addHttpListener(8080, "localhost")
				.setHandler(path().addExactPath("/api/data", dataHttpHandler)
						.addExactPath("/api/auth", authenticationHttpHandler))
				.build();

		server.start();
	}
}
