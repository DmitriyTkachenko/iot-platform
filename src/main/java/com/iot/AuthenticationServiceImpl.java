package com.iot;

public class AuthenticationServiceImpl implements AuthenticationService {
	@Override
	public boolean authenticate(String login, String password) {
		return true; // TODO fetch from database
	}
}
