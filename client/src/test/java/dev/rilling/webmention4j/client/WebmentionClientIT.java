package dev.rilling.webmention4j.client;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebmentionClientIT {

	@RegisterExtension
	static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	final WebmentionClient webmentionClient = new WebmentionClient();

	@Test
	@DisplayName("#supportsWebmention returns false if no endpoint is found")
	void supportsWebmentionFalse() throws IOException {
		WIREMOCK.stubFor(get("/no-content").willReturn(ok()));

		URI target = URI.create(WIREMOCK.url("/no-content"));
		assertThat(webmentionClient.supportsWebmention(target)).isFalse();
	}

	@Test
	@DisplayName("#supportsWebmention returns true if an endpoint is found")
	void supportsWebmentionTrue() throws IOException {
		WIREMOCK.stubFor(get("/post").willReturn(ok().withHeader(HttpHeaders.LINK,
			"<http://aaronpk.example/webmention-endpoint>; rel=\"webmention\"")));

		URI target = URI.create(WIREMOCK.url("/post"));
		assertThat(webmentionClient.supportsWebmention(target)).isTrue();
	}

	@Test
	@DisplayName("#sendWebmention throws IOException if no endpoint exists")
	void sendWebmentionNoEndpoint() {
		WIREMOCK.stubFor(get("/no-content").willReturn(ok()));

		URI target = URI.create(WIREMOCK.url("/no-content"));
		assertThatThrownBy(() -> webmentionClient.sendWebmention(URI.create("http://example.com"), target)).isNotNull()
			.isInstanceOf(IOException.class);
	}

	@Test
	@DisplayName("#sendWebmention sends webmention")
	void sendWebmentionSends() throws IOException {
		WIREMOCK.stubFor(get("/post").willReturn(ok().withHeader(HttpHeaders.LINK, "</endpoint>; rel=\"webmention\"")));
		StubMapping stubMapping = WIREMOCK.stubFor(post("/endpoint").willReturn(ok()));

		URI target = URI.create(WIREMOCK.url("/post"));
		URI source = URI.create("http://example.com");
		webmentionClient.sendWebmention(source, target);

		UrlPattern urlPattern = new UrlPattern(new EqualToPattern("/endpoint", false), false);
		String encodedTarget = URLEncoder.encode(target.toString(), StandardCharsets.UTF_8);
		String encodedSource = URLEncoder.encode(source.toString(), StandardCharsets.UTF_8);
		EqualToPattern bodyPattern = new EqualToPattern("source=%s&target=%s".formatted(encodedSource, encodedTarget));
		WIREMOCK.verify(newRequestPattern(RequestMethod.POST, urlPattern).withRequestBody(bodyPattern));
	}
}
