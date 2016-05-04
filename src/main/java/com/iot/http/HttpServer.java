package com.iot.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.auth.AuthenticationService;
import com.iot.auth.MongoAuthenticationService;
import com.iot.mq.KafkaSender;
import com.iot.token.JwtTokenService;
import com.iot.weather.http.ReportHttpHandler;
import io.undertow.Undertow;

import static io.undertow.Handlers.routing;

public class HttpServer {
	public static void main(final String[] args) {
		JwtTokenService tokenService = new JwtTokenService();
		ObjectMapper objectMapper = new ObjectMapper();
		AuthenticationService authenticationService = new MongoAuthenticationService();
		DataHttpHandler dataHttpHandler = new DataHttpHandler(new KafkaSender(), tokenService, objectMapper);
		AuthenticationHttpHandler authenticationHttpHandler =
				new AuthenticationHttpHandler(authenticationService, tokenService, objectMapper);
		ReportHttpHandler reportHttpHandler = new ReportHttpHandler();

		Undertow server = Undertow.builder()
				.addHttpListener(8080, "localhost")
				.setHandler(routing()
						.get("/api/report/device/{id}", reportHttpHandler)
						.post("/api/data", dataHttpHandler)
						.get("/api/auth/login/{login}/password/{password}", authenticationHttpHandler))
				.build();

		server.start();
	}
}
