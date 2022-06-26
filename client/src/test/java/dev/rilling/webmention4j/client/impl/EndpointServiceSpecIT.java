package dev.rilling.webmention4j.client.impl;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("spec")
class EndpointServiceSpecIT {

	@RegisterExtension
	static final WireMockExtension ENDPOINT_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@RegisterExtension
	static final AutoClosableExtension<CloseableHttpClient> HTTP_CLIENT_EXTENSION = new AutoClosableExtension<>(
		HttpClients::createDefault);

	final EndpointService endpointService = new EndpointService();

	@Test
	@DisplayName("'The sender MUST post x-www-form-urlencoded source and target parameters to the Webmention " +
		"endpoint, where source is the URL of the sender's page containing a link, and target is the URL of the page being linked to'")
	void sendsEncodedData() throws IOException {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint").willReturn(ok()));

		URI endpoint = URI.create(ENDPOINT_SERVER.url("/webmention-endpoint"));
		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(), endpoint, new Webmention(source, target));

		UrlPattern urlPattern = new UrlPattern(new EqualToPattern("/webmention-endpoint", false), false);
		EqualToPattern contentTypePattern = new EqualToPattern("application/x-www-form-urlencoded; charset=UTF-8");
		EqualToPattern bodyPattern = new EqualToPattern("source=https%3A%2F%2Fwaterpigs.example%2Fpost-by-barnaby" +
			"&target=https%3A%2F%2Faaronpk.example%2Fpost-by-aaron");
		ENDPOINT_SERVER.verify(newRequestPattern(RequestMethod.POST, urlPattern).withHeader(HttpHeaders.CONTENT_TYPE,
			contentTypePattern).withRequestBody(bodyPattern));
	}

	@Test
	@DisplayName("'Note that if the Webmention endpoint URL contains query string parameters," +
		"the query string parameters MUST be preserved, and MUST NOT be sent in the POST body'")
	void keepsQueryParams() throws IOException {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint?version=1").willReturn(ok()));

		URI endpoint = URI.create(ENDPOINT_SERVER.url("/webmention-endpoint?version=1"));
		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(), endpoint, new Webmention(source, target));

		UrlPattern urlPattern = new UrlPattern(new EqualToPattern("/webmention-endpoint?version=1", false), false);
		EqualToPattern bodyPattern = new EqualToPattern("source=https%3A%2F%2Fwaterpigs.example%2Fpost-by-barnaby" +
			"&target=https%3A%2F%2Faaronpk.example%2Fpost-by-aaron");
		ENDPOINT_SERVER.verify(newRequestPattern(RequestMethod.POST, urlPattern).withRequestBody(bodyPattern));
	}

	@Test
	@DisplayName("'If the response code is 201, the Location header will include a URL that can be used to " +
		"monitor the status of the request.' (201)")
	void returnsMonitoringUrlFor201() throws IOException {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint").willReturn(aResponse().withStatus(HttpStatus.SC_CREATED)
			.withHeader(HttpHeaders.LOCATION, "https://example.com/monitoring")));

		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		assertThat(endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint")),
			new Webmention(source, target))).contains(URI.create("https://example.com/monitoring"));
	}

	@Test
	@DisplayName("'If the response code is 201, the Location header will include a URL that can be used to " +
		"monitor the status of the request.' (non-201)")
	void returnsNoMonitoringUrlForOthers() throws IOException {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint").willReturn(aResponse().withStatus(HttpStatus.SC_OK)
			.withHeader(HttpHeaders.LOCATION, "https://example.com/monitoring")));

		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		assertThat(endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint")),
			new Webmention(source, target))).isEmpty();
	}

	@Test
	@DisplayName("'Any 2xx response code MUST be considered a success' (success)")
	void allows2XXStatus() throws IOException {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-ok").willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-created").willReturn(aResponse().withStatus(HttpStatus.SC_CREATED)));
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-accepted").willReturn(aResponse().withStatus(HttpStatus.SC_ACCEPTED)));

		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-ok")),
			new Webmention(source, target));
		endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-created")),
			new Webmention(source, target));
		endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-accepted")),
			new Webmention(source, target));
	}

	@Test
	@DisplayName("'Any 2xx response code MUST be considered a success' (error)")
	void throwsForNon2XXStatus() {
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-client").willReturn(aResponse().withStatus(HttpStatus.SC_CLIENT_ERROR)));
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-unauthorized").willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-not-found").willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));
		ENDPOINT_SERVER.stubFor(post("/webmention-endpoint-server-error").willReturn(aResponse().withStatus(HttpStatus.SC_SERVER_ERROR)));

		URI source = URI.create("https://waterpigs.example/post-by-barnaby");
		URI target = URI.create("https://aaronpk.example/post-by-aaron");

		assertThatThrownBy(() -> endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-client")),
			new Webmention(source, target))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-unauthorized")),
			new Webmention(source, target))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-not-found")),
			new Webmention(source, target))).isInstanceOf(IOException.class);
		assertThatThrownBy(() -> endpointService.notifyEndpoint(HTTP_CLIENT_EXTENSION.get(),
			URI.create(ENDPOINT_SERVER.url("/webmention-endpoint-server-error")),
			new Webmention(source, target))).isInstanceOf(IOException.class);
	}


}
