package dev.rilling.webmention4j.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndpointDiscoveryServiceTest {

	@RegisterExtension
	static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	final EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService(HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.ALWAYS)
		.build());

	@Test
	@DisplayName("Spec: 'Follow redirects'")
	void enforcesRedirects() {
		assertThatThrownBy(() -> new EndpointDiscoveryService(HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NEVER)
			.build())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Spec: 'Check for an HTTP Link header with a rel value of webmention'")
	void usesLinkHeader() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Link",
			"<http://aaronpk.example/webmention-endpoint>; rel=\"webmention\"")));

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));
	}

	@Test
	@DisplayName("Spec: 'If the content type of the document is HTML, then the sender MUST look for an HTML <link> " +
		"[...] element with a rel value of webmention'")
	void usesLinkHtmlElement() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Content-Type", "text/html").withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
				<link href="http://aaronpk.example/webmention-endpoint" rel="webmention" />
			</head>
			<body>
			</body>
			</html>""")));

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));
	}

	@Test
	@DisplayName(
		"Spec: 'If the content type of the document is HTML, then the sender MUST look for an HTML [...] <a> " +
			"element with a rel value of webmention'")
	void usesAnchorHtmlElement() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Content-Type", "text/html").withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="http://aaronpk.example/webmention-endpoint" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint"));
	}


	@Test
	@DisplayName("Spec: 'If more than one of these is present, the first HTTP Link header takes precedence'")
	void prioritizesHeader() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Link",
				"<http://aaronpk.example/webmention-endpoint1>; rel=\"webmention\"")
			.withHeader("Content-Type", "text/html")
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

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint1"));
	}

	@Test
	@DisplayName("Spec: 'If more than one of these is present, [...] takes precedence, followed by the first <link> " +
		"or <a> element in document order'")
	void prioritizesFirstElement() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Content-Type", "text/html").withBody("""
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

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint1"));
	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY be a relative URL, in which case the sender MUST resolve it relative to the target URL'")
	void adaptsRelativeUriForHeader() throws Exception {
		WIREMOCK.stubFor(get("/blog/post-by-aaron").willReturn(ok().withHeader("Link",
			"<../webmention-endpoint>; rel=\"webmention\"")));

		URI targetUri = URI.create(WIREMOCK.url("/blog/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		URI endpointUri = URI.create(WIREMOCK.url("/webmention-endpoint"));
		assertThat(endpoint).contains(endpointUri);
	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY be a relative URL, in which case the sender MUST resolve it relative to the target URL'")
	void adaptsRelativeUriForElement() throws Exception {
		WIREMOCK.stubFor(get("/blog/post-by-aaron").willReturn(ok().withHeader("Content-Type", "text/html").withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="../webmention-endpoint" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(WIREMOCK.url("/blog/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		URI endpointUri = URI.create(WIREMOCK.url("/webmention-endpoint"));
		assertThat(endpoint).contains(endpointUri);
	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY contain query string parameters, which MUST be preserved as query string parameters'")
	void preservesQueryParamsForHeader() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Link",
			"<http://aaronpk.example/webmention-endpoint?version=1>; rel=\"webmention\"")));

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint?version=1"));
	}

	@Test
	@DisplayName("Spec: 'The endpoint MAY contain query string parameters, which MUST be preserved as query string parameters'")
	void preservesQueryParamsForElement() throws Exception {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Content-Type", "text/html").withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="http://aaronpk.example/webmention-endpoint?version=1" rel="webmention">webmention</a>
			</body>
			</html>""")));

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		Optional<URI> endpoint = endpointDiscoveryService.discover(targetUri);

		assertThat(endpoint).contains(URI.create("http://aaronpk.example/webmention-endpoint?version=1"));
	}
}
