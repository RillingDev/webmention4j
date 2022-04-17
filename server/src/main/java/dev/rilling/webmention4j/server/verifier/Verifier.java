package dev.rilling.webmention4j.server.verifier;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

/**
 * Interface for classes which can verify that a resource mentions another.
 */
// TODO: add plain-text and JSON impl
public interface Verifier {
	/**
	 * Returns the MIME type that is supported by this verifier.
	 */
	@NotNull String getSupportedMimeType();

	/**
	 * Checks if the response mentions the target URI.
	 *
	 * @param httpResponse Response to check.
	 * @param target       Target URI to look for.
	 * @return if the target URI is mentioned by the response.
	 * @throws IOException if IO fails.
	 */
	boolean isValid(@NotNull ClassicHttpResponse httpResponse, @NotNull URI target) throws IOException;
}
