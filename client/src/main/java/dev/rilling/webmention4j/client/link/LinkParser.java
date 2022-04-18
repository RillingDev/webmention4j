package dev.rilling.webmention4j.client.link;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Handles extraction of {@link Link} elements from a HTTP response.
 *
 * @see Link
 */
public interface LinkParser {
	/**
	 * Parses link elements from the HTTP response.
	 * All links will have any relative URIs resolved against the location.
	 *
	 * @param location     The location of the response.
	 * @param httpResponse The response.
	 * @return A ordered list of link elements.
	 * @throws IOException if parsing fails.
	 */
	@NotNull List<Link> parse(@NotNull URI location, @NotNull ClassicHttpResponse httpResponse) throws IOException;

}
