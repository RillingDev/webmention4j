package dev.rilling.webmention4j.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import dev.rilling.webmention4j.client.WebmentionClient.Config;
import dev.rilling.webmention4j.common.Webmention;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("spec")
class WebmentionClientSpecIT {

	@RegisterExtension
	static final WireMockExtension TARGET_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@Test
	@DisplayName(
		"'During the discovery step, if the sender discovers the endpoint is localhost or a loopback IP address (127.0.0.0/8)," +
			" it SHOULD NOT send the Webmention to that endpoint.'")
	void sendWebmentionLocalhost() {
		Config config = new Config();
		config.setAllowLocalhostEndpoint(false);
		WebmentionClient webmentionClient = new WebmentionClient(config);

		TARGET_SERVER.stubFor(get("/post").willReturn(ok().withHeader(HttpHeaders.LINK,
			"</endpoint>; rel=\"webmention\"")));
		StubMapping stubMapping = TARGET_SERVER.stubFor(post("/endpoint").willReturn(ok()));


		URI target = URI.create(TARGET_SERVER.url("/post"));
		Webmention webmention = new Webmention(URI.create("https://example.com"), target);
		assertThatThrownBy(() -> webmentionClient.sendWebmention(webmention)).isNotNull()
			.isInstanceOf(IOException.class)
			.hasMessageMatching(
				"Endpoint 'http://.*/endpoint' is localhost or a loopback IP address, refusing to notify\\.");
	}

	@Test
	@DisplayName(
		"'During the discovery step, if the sender discovers the endpoint is localhost or a loopback IP address (127.0.0.0/8)," +
			" it SHOULD NOT send the Webmention to that endpoint.'")
	void sendWebmentionLocalhostRedirect() {
		Config config = new Config();
		config.setAllowLocalhostEndpoint(false);
		WebmentionClient webmentionClient = new WebmentionClient(config);

		TARGET_SERVER.stubFor(get("/post").willReturn(ok().withHeader(HttpHeaders.LINK,
			"</endpoint>; rel=\"webmention\"")));
		TARGET_SERVER.stubFor(post("/endpoint").willReturn(permanentRedirect(TARGET_SERVER.url("/real-endpoint"))));
		StubMapping stubMapping = TARGET_SERVER.stubFor(post("/real-endpoint").willReturn(ok()));

		URI target = URI.create(TARGET_SERVER.url("/post"));
		Webmention webmention = new Webmention(URI.create("https://example.com"), target);
		assertThatThrownBy(() -> webmentionClient.sendWebmention(webmention)).isNotNull()
			.isInstanceOf(IOException.class)
			.hasMessageMatching(
				"Endpoint 'http://.*/endpoint' is localhost or a loopback IP address, refusing to notify\\.");
	}

	// TODO: add test for localhost found during redirect following for notification.
}
