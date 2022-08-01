package dev.rilling.webmention4j.client.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndpointServiceIT {

	@RegisterExtension
	static final WireMockExtension ENDPOINT_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@RegisterExtension
	static final AutoClosableExtension<CloseableHttpClient> HTTP_CLIENT_EXTENSION = new AutoClosableExtension<>(
		HttpClients::createDefault);

	final EndpointService endpointService = new EndpointService();

	@Test
	@DisplayName("#notifyEndpoint throws upon invalid location header")
	void throwsForInvalidLocationHeader() {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint").willReturn(aResponse().withStatus(HttpStatus.SC_CREATED)
			.withHeader(HttpHeaders.LOCATION, "http:/\\//\\")));

		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		assertThatThrownBy(() -> endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint")),
			new Webmention(source, target))).isInstanceOf(IOException.class);
	}

}
