package dev.rilling.webmention4j.client.internal.link;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * {@link LinkParser} checking HTTP headers for {@link Link}s.
 */
public final class HeaderLinkParser implements LinkParser {

	public @NonNull List<Link> parse(@NonNull URI location, @NonNull ClassicHttpResponse response)
		throws IOException {
		try {
			return Arrays.stream(response.getHeaders(HttpHeaders.LINK))
				.map(header -> LinkUtils.fromHeaderValue(location, header.getValue()))
				.toList();
		} catch (LinkUtils.LinkException e) {
			throw new IOException("Could not parse link(s) in header.", e);
		}
	}

}
