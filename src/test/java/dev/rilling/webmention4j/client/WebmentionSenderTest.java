package dev.rilling.webmention4j.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class WebmentionSenderTest {

	@RegisterExtension
	static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@Test
	@DisplayName("uses link header URI")
		// https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
	void usesLinkHeader() throws IOException, InterruptedException {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok().withHeader("Link",
			"<http://aaronpk.example/webmention-endpoint>; rel=\"webmention\"")));

		URI targetUri = URI.create(WIREMOCK.url("/post-by-aaron"));
		WebmentionSender webmentionSender = WebmentionSender.forUri(targetUri);

		assertThat(webmentionSender.getEndpoint()).isEqualTo(URI.create("http://aaronpk.example/webmention-endpoint"));
	}

	@Test
	@DisplayName("uses link HTML element attribute")
		// https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
	void usesLinkHtmlElement() throws IOException, InterruptedException {
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
		WebmentionSender webmentionSender = WebmentionSender.forUri(targetUri);

		assertThat(webmentionSender.getEndpoint()).isEqualTo(URI.create("http://aaronpk.example/webmention-endpoint"));
	}

	@Test
	@DisplayName("uses anchor HTML element attribute")
		// https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
	void usesAnchorHtmlElement() throws IOException, InterruptedException {
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
		WebmentionSender webmentionSender = WebmentionSender.forUri(targetUri);

		assertThat(webmentionSender.getEndpoint()).isEqualTo(URI.create("http://aaronpk.example/webmention-endpoint"));
	}


	@Test
	@DisplayName("prioritizes link header")
		// https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
	void prioritizesHeader() throws IOException, InterruptedException {
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
		WebmentionSender webmentionSender = WebmentionSender.forUri(targetUri);

		assertThat(webmentionSender.getEndpoint()).isEqualTo(URI.create("http://aaronpk.example/webmention-endpoint1"));
	}

	@Test
	@DisplayName("prioritizes first element if no header exists")
		// https://www.w3.org/TR/webmention/#h-sender-discovers-receiver-webmention-endpoint
	void prioritizesFirstElement() throws IOException, InterruptedException {
		WIREMOCK.stubFor(get("/post-by-aaron").willReturn(ok()
			.withHeader("Content-Type", "text/html")
			.withBody("""
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
		WebmentionSender webmentionSender = WebmentionSender.forUri(targetUri);

		assertThat(webmentionSender.getEndpoint()).isEqualTo(URI.create("http://aaronpk.example/webmention-endpoint1"));
	}
}
