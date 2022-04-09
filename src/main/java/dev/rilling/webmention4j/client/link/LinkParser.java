package dev.rilling.webmention4j.client.link;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.util.List;

/**
 * Handles extraction of {@link Link} elements from a HTTP response.
 */
public interface LinkParser {
	/**
	 * Parses Link elements from the HTTP response.
	 *
	 * @param location     Location of the response.
	 * @param httpResponse The response.
	 * @return A ordered list of Link elements.
	 * @throws LinkParsingException if parsing fails.
	 */
	@NotNull List<Link> parse(@NotNull URI location, @NotNull ClassicHttpResponse httpResponse)
		throws LinkParsingException;

	class LinkParsingException extends IOException {
		@Serial
		private static final long serialVersionUID = 1638650087206262157L;

		public LinkParsingException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
