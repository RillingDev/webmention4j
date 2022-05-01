package dev.rilling.webmention4j.client.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.client.impl.link.HeaderLinkParser;
import dev.rilling.webmention4j.client.impl.link.HtmlLinkParser;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
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
	@DisplayName("Misc: 'Throws on IO error'")
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

	@Test
	@DisplayName("Spec: 'Follow redirects'")
	void followsRedirects() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron-redirect").willReturn(permanentRedirect("/post-by-aaron")));

		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.LINK,
			"<http://aaronpk.example/webmention-endpoint>; rel=\"webmention\"")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron-redirect"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));

	}

	@Test
	@DisplayName("Spec: 'Check for an HTTP Link header with a rel value of webmention'")
	void usesLinkHeader() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.LINK,
			"<http://aaronpk.example/webmention-endpoint>; rel=\"webmention\"")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));

	}

	@Test
	@DisplayName("Spec: 'If the content type of the document is HTML, then the sender MUST look for an HTML <link> " +
		"[...] element with a rel value of webmention'")
	void usesLinkHtmlElement() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
				<link href="http://aaronpk.example/webmention-endpoint" rel="webmention" />
			</head>
			<body>
			</body>
			</html>""")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));

	}

	@Test
	@DisplayName(
		"Spec: 'If the content type of the document is HTML, then the sender MUST look for an HTML [...] <a> " +
			"element with a rel value of webmention'")
	void usesAnchorHtmlElement() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="http://aaronpk.example/webmention-endpoint" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));

	}


	@Test
	@DisplayName("Spec: 'If more than one of these is present, the first HTTP Link header takes precedence'")
	void prioritizesHeader() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.LINK,
				"<http://aaronpk.example/webmention-endpoint1>; rel=\"webmention\"")
			.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString())
			.withBody("""
				<html lang="en">
				<head>
					<title>Foo</title>
					<link href="http://aaronpk.example/webmention-endpoint2" rel="webmention" />
					<link href="http://aaronpk.example/webmention-endpoint2b" rel="webmention" />
				</head>
				<body>
					<a href="http://aaronpk.example/webmention-endpoint3" rel="webmention">webmention</a>
					<a href="http://aaronpk.example/webmention-endpoint3b" rel="webmention">webmention</a>
				</body>
				</html>""")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint1"));

	}

	@Test
	@DisplayName("Spec: 'If more than one of these is present, [...] takes precedence, followed by the first <link> " +
		"or <a> element in document order'")
	void prioritizesFirstElement() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
				<link href="http://aaronpk.example/webmention-endpoint1" rel="webmention" />
				<link href="http://aaronpk.example/webmention-endpoint1b" rel="webmention" />
			</head>
			<body>
				<a href="http://aaronpk.example/webmention-endpoint2" rel="webmention">webmention</a>
				<a href="http://aaronpk.example/webmention-endpoint2b" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint1"));

	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY be a relative URL, in which case the sender MUST resolve it relative to the target URL'")
	void adaptsRelativeUriForHeader() throws IOException {
		TARGET_SERVER.stubFor(get("/blog/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.LINK,
			"<../webmention-endpoint>; rel=\"webmention\"")));

		URI targetUri = URI.create(TARGET_SERVER.url("/blog/post-by-aaron"));

		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create(TARGET_SERVER.url("/webmention-endpoint")));

	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY be a relative URL, in which case the sender MUST resolve it relative to the target URL'")
	void adaptsRelativeUriForElement() throws IOException {
		TARGET_SERVER.stubFor(get("/blog/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="../webmention-endpoint" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(TARGET_SERVER.url("/blog/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create(TARGET_SERVER.url("/webmention-endpoint")));

	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY contain query string parameters, which MUST be preserved as query string parameters'")
	void preservesQueryParamsForHeader() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.LINK,
			"<http://aaronpk.example/webmention-endpoint?version=1>; rel=\"webmention\"")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint?version=1"));

	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY contain query string parameters, which MUST be preserved as query string parameters'")
	void preservesQueryParamsForElement() throws IOException {
		TARGET_SERVER.stubFor(get("/post-by-aaron").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="http://aaronpk.example/webmention-endpoint?version=1" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(TARGET_SERVER.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discoverEndpoint(HTTP_CLIENT_EXTENSION.get(), targetUri);
		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint?version=1"));
	}
}
