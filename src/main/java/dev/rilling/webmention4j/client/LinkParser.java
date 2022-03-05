package dev.rilling.webmention4j.client;

import jakarta.ws.rs.core.Link;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Handles extraction of {@link Link} elements from a HTTP response.
 */
interface LinkParser {
	/**
	 * Parses Link elements from the HTTP response.
	 *
	 * @param uri          Location of the response.
	 * @param httpResponse The response.
	 * @return A ordered list of Link elements.
	 * @throws IOException if parsing fails.
	 */
	@NotNull List<Link> parse(@NotNull URI uri, @NotNull ClassicHttpResponse httpResponse) throws IOException;
}
