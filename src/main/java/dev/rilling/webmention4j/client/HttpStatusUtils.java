package dev.rilling.webmention4j.client;

import org.apache.hc.core5.http.HttpStatus;

public final class HttpStatusUtils {
	private HttpStatusUtils() {
	}

	public static boolean isSuccessful(int statusCode) {
		return statusCode >= HttpStatus.SC_OK && statusCode <= HttpStatus.SC_REDIRECTION;
	}
}
