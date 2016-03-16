package com.iot;

import io.undertow.Handlers;
import io.undertow.Undertow;

public class HttpServer {
	public static void main(final String[] args) {
		Undertow server = Undertow.builder()
				.addHttpListener(8080, "localhost")
				.setHandler(Handlers.path()
						.addExactPath("/api/data", new DataHttpHandler(new KafkaSender())))
				.build();
		server.start();
	}
}
