package dev.rilling.webmention4j.common;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.MessageHeaders;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class HttpUtils {
	private HttpUtils() {
	}

	public static boolean isSuccessful(int statusCode) {
		return statusCode >= HttpStatus.SC_OK && statusCode <= HttpStatus.SC_REDIRECTION;
	}

	@NotNull
	public static Optional<ContentType> extractContentType(@NotNull MessageHeaders httpResponse) {
		return Optional.ofNullable(httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE))
			.map(contentTypeHeader -> ContentType.parse(contentTypeHeader.getValue()));
	}

}
