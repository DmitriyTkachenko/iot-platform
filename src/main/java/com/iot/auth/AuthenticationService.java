package com.iot.auth;

import java.util.concurrent.CompletionStage;

public interface AuthenticationService {
	CompletionStage<Boolean> authenticate(String login, String password);
}
