package dev.rilling.webmention4j.server.internal.verifier;

import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * Interface for classes which can verify that a resource mentions another.
 */
public interface Verifier {
	/**
	 * Returns the MIME type supported by this verifier.
	 */
	String getSupportedMimeType();

	/**
	 * Checks if the response mentions the target URL.
	 *
	 * @param response Response to check. Will be a successful response
	 *                 with a declared content type compatible with {@link #getSupportedMimeType()}.
	 * @param target   Target URL to look for.
	 * @return if the target URL is mentioned by the response.
	 * @throws IOException if I/O fails.
	 */
	boolean isValid(ClassicHttpResponse response, URI target) throws IOException;
}
