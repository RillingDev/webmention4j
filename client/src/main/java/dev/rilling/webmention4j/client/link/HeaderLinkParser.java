package dev.rilling.webmention4j.client.link;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * {@link LinkParser} checking HTTP headers for {@link Link}s.
 */
public final class HeaderLinkParser implements LinkParser {

	public @NotNull List<Link> parse(@NotNull URI location, @NotNull ClassicHttpResponse httpResponse)
		throws LinkParsingException {
		try {
			return Arrays.stream(httpResponse.getHeaders(HttpHeaders.LINK))
				.map(header -> RuntimeDelegate.getInstance()
					.createLinkBuilder()
					.baseUri(location)
					.link(header.getValue())
					.build())
				.map(Link::convert)
				.toList();
		} catch (Exception e) {
			throw new LinkParsingException("Could not parse link(s) in header.", e);
		}
	}

}
