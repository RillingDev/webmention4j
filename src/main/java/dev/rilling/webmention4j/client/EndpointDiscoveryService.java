package dev.rilling.webmention4j.client;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service handling Webmention endpoint detection.
 */
// Spec: https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
final class EndpointDiscoveryService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointDiscoveryService.class);

	// Very primitive parser for checking that 'rel="webmention"' is present.
	// Spec: https://datatracker.ietf.org/doc/html/rfc5988#section-5
	private static final Pattern HEADER_LINK_WEBMENTION = Pattern.compile(
		"^<(?<url>.*)>.*;\\s*rel\\s*=\\s*\"?webmention\"?.*$");

	private final @NotNull Supplier<CloseableHttpClient> httpClientFactory;

	/**
	 * Constructor.
	 *
	 * @param httpClientFactory Factory to create {@link CloseableHttpClient}s from.
	 *                          Must be configured to follow redirects.
	 *                          May be configured to present an UA that references Webmention.
	 */
	/*
	 * Spec:
	 * 'follow redirects'
	 * 'Senders MAY customize the HTTP User Agent used when fetching the target URL
	 * in order to indicate to the recipient that this request is made as part of Webmention discovery.'
	 */
	EndpointDiscoveryService(@NotNull Supplier<CloseableHttpClient> httpClientFactory) {
		this.httpClientFactory = httpClientFactory;
	}

	/**
	 * Attempts to discover the Webmention endpoint that is used for this target URL.
	 *
	 * @param target Target URL (e.g. the referenced website).
	 * @return The Webmention endpoint URI if one is found, or empty.
	 * @throws IOException If IO fails.
	 */
	@NotNull
	public Optional<URI> discoverEndpoint(@NotNull URI target) throws IOException {
		// Spec: 'The sender MUST fetch the target URL'
		ClassicHttpRequest request = ClassicRequestBuilder.get(target).build();

		LOGGER.debug("Requesting endpoint information from '{}'.", target);
		try (CloseableHttpClient httpClient = httpClientFactory.get(); ClassicHttpResponse response = httpClient.execute(
			request)) {
			return discoverEndpoint(target, response);
		}
	}

	@NotNull
	private Optional<URI> discoverEndpoint(@NotNull URI target, @NotNull ClassicHttpResponse response)
		throws IOException {
		LOGGER.trace("Received response '{}' from '{}'.", response, target);

		if (!HttpStatusUtils.isSuccessful(response.getCode())) {
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
		Optional<URI> fromHeader = extractEndpointFromHeader(response).map(endpoint -> postProcessEndpointUri(target,
			endpoint));
		if (fromHeader.isPresent()) {
			LOGGER.debug("Found endpoint '{}' in header.", fromHeader.get());
			return fromHeader;
		}

		if (isHtml(response)) {
			String body;
			try {
				body = EntityUtils.toString(response.getEntity());
			} catch (ParseException e) {
				throw new IOException("Could not parse body.", e);
			}
			Optional<URI> fromBody = extractEndpointFromHtml(target,
				body).map(endpoint -> postProcessEndpointUri(target, endpoint));
			fromBody.ifPresent(value -> LOGGER.debug("Found endpoint '{}' in body.", value));
			return fromBody;
		}

		LOGGER.debug("Found no endpoint for '{}'.", target);
		return Optional.empty();
	}

	@NotNull
	private Optional<URI> extractEndpointFromHeader(@NotNull HttpResponse httpResponse) {
		for (Header link : httpResponse.getHeaders("Link")) {
			Matcher matcher = HEADER_LINK_WEBMENTION.matcher(link.getValue());
			if (matcher.matches()) {
				URI endpoint = URI.create(matcher.group("url"));
				// Spec: 'The first HTTP Link header takes precedence'
				return Optional.of(endpoint);
			}
		}
		return Optional.empty();
	}

	@NotNull
	private Optional<URI> extractEndpointFromHtml(@NotNull URI baseUri, @NotNull String body) {
		Document document = Jsoup.parse(body, baseUri.toString());
		Element firstWebmentionElement = document.selectFirst(new Evaluator() {
			@Override
			public boolean matches(@NotNull Element root, @NotNull Element element) {
				return ("link".equals(element.normalName()) || "a".equals(element.normalName())) &&
					"webmention".equals(element.attr("rel"));
			}
		});
		if (firstWebmentionElement != null) {
			URI endpoint = URI.create(firstWebmentionElement.attr("href"));
			return Optional.of(endpoint);
		}
		return Optional.empty();
	}

	@NotNull
	private URI postProcessEndpointUri(@NotNull URI base, @NotNull URI endpoint) {
		/*
		 * Spec:
		 * 'The endpoint MAY be a relative URL,
		 * in which case the sender MUST resolve it relative to the target URL'.
		 */
		return base.resolve(endpoint);
	}

	private boolean isHtml(@NotNull HttpResponse httpResponse) {
		Header contentType = httpResponse.getFirstHeader("Content-Type");
		return contentType != null && ContentType.parse(contentType.getValue()).isSameMimeType(ContentType.TEXT_HTML);
	}
}
