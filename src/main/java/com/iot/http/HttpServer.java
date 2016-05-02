package com.iot.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.mq.KafkaSender;
import com.iot.auth.AuthenticationService;
import com.iot.auth.MongoAuthenticationService;
import com.iot.token.JwtTokenService;
import io.undertow.Undertow;

import static io.undertow.Handlers.path;

public class HttpServer {
	public static void main(final String[] args) {
		JwtTokenService tokenService = new JwtTokenService();
		ObjectMapper objectMapper = new ObjectMapper();
		AuthenticationService authenticationService = new MongoAuthenticationService();
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
