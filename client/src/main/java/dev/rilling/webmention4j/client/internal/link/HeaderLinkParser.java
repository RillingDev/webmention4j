package dev.rilling.webmention4j.client.internal.link;

import jakarta.ws.rs.core.HttpHeaders;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * {@link LinkParser} checking HTTP headers for {@link Link}s.
 */
public final class HeaderLinkParser implements LinkParser {

	public @NotNull List<Link> parse(@NotNull URI location, @NotNull ClassicHttpResponse response)
		throws IOException {
		try {
			return Arrays.stream(response.getHeaders(HttpHeaders.LINK))
				.map(header -> LinkUtils.fromHeaderValue(location, header.getValue()))
				.toList();
		} catch (Exception e) {
			throw new IOException("Could not parse link(s) in header.", e);
		}
	}

}
