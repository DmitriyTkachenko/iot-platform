package com.iot.auth;

import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.mongodb.client.model.Filters.eq;

public class MongoAuthenticationService implements AuthenticationService {
	public static final String DB_NAME = "iot";
	public static final String COLLECTION_NAME = "device_info";

	public static final String DEVICE_ID = "device_id";
	public static final String PASSWORD = "password";

	private final MongoCollection<Document> deviceInfoCollection;

	public MongoAuthenticationService() {
		MongoClient mongoClient = MongoClients.create();
		MongoDatabase db = mongoClient.getDatabase(DB_NAME);
		deviceInfoCollection = db.getCollection(COLLECTION_NAME);
	}

	@Override
	public CompletionStage<Boolean> authenticate(String login, String password) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		deviceInfoCollection.find(eq(DEVICE_ID, login)).first((result, exception) -> {
			if (exception != null) {
				future.completeExceptionally(exception);
				return;
			}

			if (result == null) {
				future.complete(false);
				return;
			}

			Object pwd = result.get(PASSWORD);
			if (pwd instanceof String && pwd.equals(password))
				future.complete(true);
			else
				future.complete(false);
		});
		return future;
	}
}
