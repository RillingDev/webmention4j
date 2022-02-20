package dev.rilling.webmention4j.client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebmentionSender {
	// TODO: check against https://datatracker.ietf.org/doc/html/rfc5988#section-5
	private static final Pattern HEADER_LINK_WEBMENTION = Pattern.compile("<(?<url>.*)>; rel=\"webmention\"");

	private final URI endpoint;

	private WebmentionSender(URI endpoint) {
		this.endpoint = endpoint;
	}

	public URI getEndpoint() {
		return endpoint;
	}

	public static WebmentionSender forUri(URI uri) throws IOException, InterruptedException {
		// 3.1.2: Follow all redirects.
		HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

		URI endpoint = discoverEndpoint(httpClient, uri).orElseThrow(() -> new IOException(
			"Could not find any webmention endpoint URI in the target resource."));

		return new WebmentionSender(endpoint);
	}

	private static Optional<URI> discoverEndpoint(HttpClient httpClient, URI uri)
		throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		Optional<URI> fromHeader = extractEndpointFromHeader(response.headers());
		if (fromHeader.isPresent()) {
			return fromHeader;
		}

		if (response.headers().firstValue("Content-Type")
			// Charset can be ignored, HttpClient already handled it
			.map(contentType -> contentType.startsWith("text/html")).orElse(false)) {
			return extractEndpointFromHtml(uri, response.body());
		}

		return Optional.empty();
	}

	private static Optional<URI> extractEndpointFromHeader(HttpHeaders httpHeaders) {
		// https://datatracker.ietf.org/doc/html/rfc5988
		for (String link : httpHeaders.allValues("Link")) {
			Matcher matcher = HEADER_LINK_WEBMENTION.matcher(link);
			if (matcher.matches()) {
				URI endpoint = URI.create(matcher.group("url"));
				// Return first match, ignore others.
				return Optional.of(endpoint);
			}
		}
		return Optional.empty();
	}

	private static Optional<URI> extractEndpointFromHtml(URI baseUri, String body) {
		Document document = Jsoup.parse(body, baseUri.toString());

		Element firstLinkLikeElement = document.selectFirst(new Evaluator() {
			@Override
			public boolean matches(Element root, Element element) {
				return ("link".equals(element.normalName()) || "a".equals(element.normalName())) &&
					"webmention".equals(element.attr("rel"));
			}
		});

		if (firstLinkLikeElement != null) {
			URI endpoint = URI.create(firstLinkLikeElement.absUrl("href"));
			return Optional.of(endpoint);
		}
		return Optional.empty();
	}
}
