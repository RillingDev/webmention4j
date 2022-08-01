package dev.rilling.webmention4j.client.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.client.internal.link.HeaderLinkParser;
import dev.rilling.webmention4j.client.internal.link.HtmlLinkParser;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndpointDiscoveryServiceIT {

	@RegisterExtension
	static final WireMockExtension TARGET_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@RegisterExtension
	static final AutoClosableExtension<CloseableHttpClient> HTTP_CLIENT_EXTENSION = new AutoClosableExtension<>(
		HttpClients::createDefault);

	final EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService(new HeaderLinkParser(),
		new HtmlLinkParser());


	@Test
	@DisplayName("#discoverEndpoint throws on IO error")
	void throwsOnError() {
		TARGET_SERVER.stubFor(get("/client-error").willReturn(aResponse().withStatus(HttpStatus.SC_CLIENT_ERROR)));
		TARGET_SERVER.stubFor(get("/not-found").willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		TARGET_SERVER.stubFor(get("/unauthorized").willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));
		TARGET_SERVER.stubFor(get("/server-error").willReturn(aResponse().withStatus(HttpStatus.SC_SERVER_ERROR)));

		assertThatThrownBy(() -> endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(TARGET_SERVER.url("/client-error")))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(TARGET_SERVER.url("/not-found")))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(TARGET_SERVER.url("/unauthorized")))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(TARGET_SERVER.url("/server-error")))).isInstanceOf(IOException.class);

	}

}
