package dev.rilling.webmention4j.client.impl;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class LocalhostIgnoringRedirectStrategyTest {

	LocalhostIgnoringRedirectStrategy localhostIgnoringRedirectStrategy = new LocalhostIgnoringRedirectStrategy();

	@Test
	@DisplayName("#isRedirected ignores redirect locations that are localhost")
	void isRedirectedIgnoresLocalhost() throws ProtocolException {
		HttpRequest request = new BasicClassicHttpRequest(Method.GET, URI.create("https://example.com"));
		HttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY);
		response.setHeader(HttpHeaders.LOCATION, "https://localhost");

		assertThat(localhostIgnoringRedirectStrategy.isRedirected(request,
			response,
			new HttpClientContext())).isFalse();
	}

	@Test
	@DisplayName("#isRedirected keeps redirect locations that are not localhost")
	void isRedirectedKeepsNonLocalhost() throws ProtocolException {
		HttpRequest request = new BasicClassicHttpRequest(Method.GET, URI.create("https://example.com"));
		HttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY);
		response.setHeader(HttpHeaders.LOCATION, "https://example.org");

		assertThat(localhostIgnoringRedirectStrategy.isRedirected(request, response, new HttpClientContext())).isTrue();
	}
}
