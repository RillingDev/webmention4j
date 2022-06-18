package dev.rilling.webmention4j.client;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import dev.rilling.webmention4j.client.WebmentionClient.Config;
import dev.rilling.webmention4j.common.Webmention;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
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
	static final WireMockExtension TARGET_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	WebmentionClient webmentionClient;

	@BeforeEach
	void setUp() {
		Config config = new Config();
		config.setAllowLocalhostEndpoint(true);
		webmentionClient = new WebmentionClient(config);
	}

	@Test
	@DisplayName("#supportsWebmention returns false if no endpoint is found")
	void supportsWebmentionFalse() throws IOException {
		TARGET_SERVER.stubFor(get("/no-content").willReturn(ok()));

		URI target = URI.create(TARGET_SERVER.url("/no-content"));
		assertThat(webmentionClient.supportsWebmention(target)).isFalse();
	}

	@Test
	@DisplayName("#supportsWebmention returns true if an endpoint is found")
	void supportsWebmentionTrue() throws IOException {
		TARGET_SERVER.stubFor(get("/post").willReturn(ok().withHeader(HttpHeaders.LINK,
			"<http://aaronpk.example/webmention-endpoint>; rel=\"webmention\"")));

		URI target = URI.create(TARGET_SERVER.url("/post"));
		assertThat(webmentionClient.supportsWebmention(target)).isTrue();
	}

	@Test
	@DisplayName("#sendWebmention throws IOException if no endpoint exists")
	void sendWebmentionNoEndpoint() {
		TARGET_SERVER.stubFor(get("/no-content").willReturn(ok()));

		URI target = URI.create(TARGET_SERVER.url("/no-content"));
		Webmention webmention = new Webmention(URI.create("https://example.com"), target);
		assertThatThrownBy(() -> webmentionClient.sendWebmention(webmention)).isNotNull()
			.isInstanceOf(IOException.class)
			.hasMessage("Could not find any webmention endpoint URL in the target resource.");
	}

	@Test
	@DisplayName("#sendWebmention sends webmention")
	void sendWebmentionSends() throws IOException {
		TARGET_SERVER.stubFor(get("/post").willReturn(ok().withHeader(HttpHeaders.LINK,
			"</endpoint>; rel=\"webmention\"")));
		StubMapping stubMapping = TARGET_SERVER.stubFor(post("/endpoint").willReturn(ok()));

		URI target = URI.create(TARGET_SERVER.url("/post"));
		URI source = URI.create("https://example.com");
		webmentionClient.sendWebmention(new Webmention(source, target));

		UrlPattern urlPattern = new UrlPattern(new EqualToPattern("/endpoint", false), false);
		String encodedTarget = URLEncoder.encode(target.toString(), StandardCharsets.UTF_8);
		String encodedSource = URLEncoder.encode(source.toString(), StandardCharsets.UTF_8);
		EqualToPattern bodyPattern = new EqualToPattern("source=%s&target=%s".formatted(encodedSource, encodedTarget));
		TARGET_SERVER.verify(newRequestPattern(RequestMethod.POST, urlPattern).withRequestBody(bodyPattern));
	}
}
