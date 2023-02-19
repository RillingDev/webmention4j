package dev.rilling.webmention4j.client.internal;

import org.apache.hc.client5.http.RedirectException;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalhostRejectingRedirectStrategyTest {

	LocalhostRejectingRedirectStrategy localhostRejectingRedirectStrategy = new LocalhostRejectingRedirectStrategy();

	@Test
	@DisplayName("#isRedirected rejects redirect locations that are localhost")
	void isRedirectedIgnoresLocalhost() throws IOException {
		HttpRequest request = new BasicClassicHttpRequest(Method.GET, URI.create("https://example.com"));
		try (BasicClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY)) {
			response.setHeader(HttpHeaders.LOCATION, "https://localhost");

			assertThatThrownBy(() -> localhostRejectingRedirectStrategy.isRedirected(request,
				response,
				new HttpClientContext())).isInstanceOf(RedirectException.class);
		}
	}

	@Test
	@DisplayName("#isRedirected keeps redirect locations that are not localhost")
	void isRedirectedKeepsNonLocalhost() throws Exception {
		HttpRequest request = new BasicClassicHttpRequest(Method.GET, URI.create("https://example.com"));
		try (BasicClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_MOVED_PERMANENTLY)) {
			response.setHeader(HttpHeaders.LOCATION, "https://example.org");

			assertThat(localhostRejectingRedirectStrategy.isRedirected(request,
				response,
				new HttpClientContext())).isTrue();
		}
	}

	@Test
	@DisplayName("#isRedirected keeps non-redirect responses")
	void isRedirectedKeepsNonRedirect() throws Exception {
		HttpRequest request = new BasicClassicHttpRequest(Method.GET, URI.create("https://example.com"));
		try (BasicClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			assertThat(localhostRejectingRedirectStrategy.isRedirected(request,
				response,
				new HttpClientContext())).isFalse();
		}
	}
}
