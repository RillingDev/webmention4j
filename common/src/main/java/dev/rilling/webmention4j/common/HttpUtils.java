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
		return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_REDIRECTION;
	}

	@NotNull
	public static Optional<ContentType> extractContentType(@NotNull MessageHeaders httpResponse) {
		return Optional.ofNullable(httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE))
			.map(contentTypeHeader -> ContentType.parse(contentTypeHeader.getValue()));
	}

	/**
	 * Creates a user-agent string for the given name and the version associated with the given package.
	 *
	 * @param name                  Name to identify the user agent with. Should be lowercase and without spaces/special characters.
	 * @param implementationPackage The package to look up the {@link Package#getImplementationVersion} from. If no
	 *                              version is found, a placeholder is substituted.
	 * @return a user-agent string.
	 */
	@NotNull
	public static String createUserAgentString(@NotNull String name, @NotNull Package implementationPackage) {
		return "%s/%s".formatted(name, getVersionString(implementationPackage));
	}

	@NotNull
	private static String getVersionString(@NotNull Package aPackage) {
		if (aPackage.getImplementationVersion() == null) {
			return "0.0.0-development";
		}
		return aPackage.getImplementationVersion();
	}
}
