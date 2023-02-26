package dev.rilling.webmention4j.common.internal;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public final class HttpUtils {

	private HttpUtils() {
	}


	/**
	 * Validates that the response has a 2xx status code.
	 *
	 * @param response Response to check.
	 * @throws IOException if the response has a non-2xx status code.
	 */
	public static void validateResponse(@NotNull ClassicHttpResponse response) throws IOException {
		// See AbstractHttpClientResponseHandler
		if (response.getCode() >= HttpStatus.SC_REDIRECTION) {
			String body;
			if (response.getEntity() == null) {
				body = "<no body>";
			} else {
				try {
					body = EntityUtils.toString(response.getEntity());
				} catch (ParseException ignored) {
					body = "<parsing of body failed>";
				}
			}
			throw new IOException("Request failed: %d - %s:%n%s".formatted(response.getCode(), response.getReasonPhrase(), body));
		}
	}

	/**
	 * @return The 'Content-Type' header value of the response, if one is defined.
	 */
	@NotNull
	public static Optional<ContentType> extractContentType(@NotNull MessageHeaders messageHeaders) {
		return Optional.ofNullable(messageHeaders.getFirstHeader(HttpHeaders.CONTENT_TYPE)).map(contentTypeHeader -> ContentType.parse(contentTypeHeader.getValue()));
	}

	/**
	 * @return The 'Location' header value of the response, if one is defined.
	 * @throws IOException if location URL cannot be parsed.
	 */
	@NotNull
	public static Optional<URI> extractLocation(@NotNull MessageHeaders messageHeaders) throws IOException {
		Header locationHeader = messageHeaders.getFirstHeader(HttpHeaders.LOCATION);
		if (locationHeader == null || locationHeader.getValue() == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(new URI(locationHeader.getValue()));
		} catch (URISyntaxException e) {
			throw new IOException("Could not parse location header.", e);
		}
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

	private static String getVersionString(Package implementationPackage) {
		if (implementationPackage.getImplementationVersion() == null) {
			return "0.0.0-development";
		}
		return implementationPackage.getImplementationVersion();
	}

}
