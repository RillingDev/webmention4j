package dev.rilling.webmention4j.client;

import dev.rilling.webmention4j.client.link.Link;
import dev.rilling.webmention4j.client.link.LinkParser;
import dev.rilling.webmention4j.common.HttpUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Service handling Webmention endpoint detection.
 */
// Spec: https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
final class EndpointDiscoveryService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointDiscoveryService.class);

	private final @NotNull LinkParser headerLinkParser;
	private final @NotNull LinkParser htmlLinkParser;

	/**
	 * Constructor.
	 *
	 * @param headerLinkParser A {@link LinkParser} capable of header parsing.
	 * @param htmlLinkParser   A {@link LinkParser} capable of HTML parsing.
	 */
	EndpointDiscoveryService(@NotNull LinkParser headerLinkParser, @NotNull LinkParser htmlLinkParser) {
		this.headerLinkParser = headerLinkParser;
		this.htmlLinkParser = htmlLinkParser;
	}

	/**
	 * Attempts to discover the Webmention endpoint that is used for this target URL.
	 *
	 * @param httpClient HTTP client.
	 *                   Must be configured to follow redirects.
	 *                   Should be configured to use a fitting UA string.
	 * @param target     Target URL (e.g. the referenced website).
	 * @return The Webmention endpoint URI if one is found, or empty.
	 * @throws IOException If IO fails.
	 */
	@NotNull
	public Optional<URI> discoverEndpoint(@NotNull CloseableHttpClient httpClient, @NotNull URI target)
		throws IOException {
		// Spec: 'The sender MUST fetch the target URL'
		ClassicHttpRequest request = ClassicRequestBuilder.get(target).build();

		LOGGER.debug("Requesting endpoint information from '{}'.", target);
		try (ClassicHttpResponse response = httpClient.execute(request)) {
			return discoverEndpoint(target, response);
		}
	}

	@NotNull
	private Optional<URI> discoverEndpoint(@NotNull URI target, @NotNull ClassicHttpResponse response)
		throws IOException {
		LOGGER.trace("Received response '{}' from '{}'.", response, target);

		// TODO: make HEAD request.

		if (!HttpUtils.isSuccessful(response.getCode())) {
			EntityUtils.consume(response.getEntity());
			throw new IOException("Request failed: %d - '%s'.".formatted(response.getCode(),
				response.getReasonPhrase()));
		}

		/*
		 * Spec:
		 * 'Check for an HTTP Link header with a rel value of webmention.
		 * If the content type of the document is HTML,
		 * then the sender MUST look for an HTML <link> and <a> element with a rel value of webmention.
		 * If more than one of these is present, the first HTTP Link header takes precedence,
		 * followed by the first <link> or <a> element in document order.
		 * Senders MUST support all three options and fall back in this order.'
		 *
		 * 'The endpoint MAY contain query string parameters, which MUST be preserved as query string parameters'
		 */
		Optional<URI> fromHeader = findWebmentionEndpoint(headerLinkParser, target, response);
		if (fromHeader.isPresent()) {
			LOGGER.debug("Found endpoint '{}' in header.", fromHeader.get());
			return fromHeader;
		}

		Optional<URI> fromBody = findWebmentionEndpoint(htmlLinkParser, target, response);
		if (fromBody.isPresent()) {
			LOGGER.debug("Found endpoint '{}' in body.", fromBody.get());
			return fromBody;
		}

		LOGGER.debug("Found no endpoint for '{}'.", target);
		return Optional.empty();
	}

	@NotNull
	private Optional<URI> findWebmentionEndpoint(@NotNull LinkParser linkParser,
												 @NotNull URI target,
												 @NotNull ClassicHttpResponse httpResponse) throws IOException {
		/*
		 * Spec:
		 * 'The endpoint MAY be a relative URL, in which case the sender MUST resolve it relative to the target
		 * URL according to [URL].'
		 */
		return linkParser.parse(target, httpResponse)
			.stream()
			.filter(link -> link.rel().contains("webmention"))
			.findFirst()
			.map(Link::uri);
	}
}
