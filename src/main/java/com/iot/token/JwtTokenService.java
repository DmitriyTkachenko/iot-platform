package com.iot.token;

import com.auth0.jwt.JWTSigner;
import com.auth0.jwt.JWTVerifier;

import java.util.Map;
import java.util.regex.Pattern;

public class JwtTokenService {
	private static final String SECRET = "feRtfVqoyDJWVVoP4X2m";

	private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile("^Bearer$", Pattern.CASE_INSENSITIVE);

	private final JWTVerifier verifier = new JWTVerifier(SECRET);
	private final JWTSigner signer = new JWTSigner(SECRET);

	public String createToken(Map<String, Object> claims) {
		return signer.sign(claims);
	}

	public Map<String, Object> verifyToken(String authorizationHeader) throws TokenParseException,
			TokenVerificationException {
		String token = extractToken(authorizationHeader);
		try {
			return verifier.verify(token);
		} catch (Exception e) {
			throw new TokenVerificationException(e);
		}
	}

	private static String extractToken(String authorizationHeader) throws TokenParseException {
		if (authorizationHeader == null)
			throw new TokenParseException("Authorization header value is null");

		String[] parts = authorizationHeader.split(" ");
		if (parts.length != 2) {
			throw new TokenParseException("Incorrect format: '" + authorizationHeader +
					"'. Format is Authorization: Bearer [token]");
		}

		String scheme = parts[0];
		String credentials = parts[1];

		if (AUTH_HEADER_PATTERN.matcher(scheme).matches()) {
			return credentials;
		} else {
			throw new TokenParseException("Incorrect scheme: " + scheme);
		}
	}
}
