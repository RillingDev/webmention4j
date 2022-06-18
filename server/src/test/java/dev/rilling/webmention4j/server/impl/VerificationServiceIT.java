package dev.rilling.webmention4j.server.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import dev.rilling.webmention4j.server.impl.verifier.HtmlVerifier;
import dev.rilling.webmention4j.server.impl.verifier.JsonVerifier;
import dev.rilling.webmention4j.server.impl.verifier.TextVerifier;
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
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationServiceIT {

	@RegisterExtension
	static final WireMockExtension SOURCE_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@RegisterExtension
	static final AutoClosableExtension<CloseableHttpClient> HTTP_CLIENT_EXTENSION = new AutoClosableExtension<>(
		HttpClients::createDefault);

	final VerificationService verificationService = new VerificationService(List.of(new HtmlVerifier(),
		new TextVerifier(),
		new JsonVerifier()));

	@Test
	@DisplayName("#isWebmentionValid throws on non-success response")
	void isWebmentionValidThrowsOnError() {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(notFound()));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");

		assertThatThrownBy(() -> verificationService.isWebmentionValid(HTTP_CLIENT_EXTENSION.get(),
			new Webmention(source, target))).isNotNull().isInstanceOf(IOException.class);
	}

	@Test
	@DisplayName("#isWebmentionValid throws on 'Not Acceptable' response")
	void isWebmentionValidThrowsOnNotAcceptable() {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(aResponse().withStatus(HttpStatus.SC_NOT_ACCEPTABLE)));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");

		assertThatThrownBy(() -> verificationService.isWebmentionValid(HTTP_CLIENT_EXTENSION.get(),
			new Webmention(source, target))).isNotNull()
			.isInstanceOf(VerificationService.UnsupportedContentTypeException.class);
	}

	@Test
	@DisplayName("#isWebmentionValid throws on unspecified content type")
	void isWebmentionValidThrowsOnUnknownContentType() {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE, "text/weird")));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");

		assertThatThrownBy(() -> verificationService.isWebmentionValid(HTTP_CLIENT_EXTENSION.get(),
			new Webmention(source, target))).isNotNull()
			.isInstanceOf(VerificationService.UnsupportedContentTypeException.class);
	}

	@Test
	@DisplayName("#isWebmentionValid returns true if response contains link")
	void isWebmentionValidChecksContentTrue() throws Exception {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="https://example.com">cool site</a>
			</body>
			</html>""")));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");
		assertThat(verificationService.isWebmentionValid(HTTP_CLIENT_EXTENSION.get(),
			new Webmention(source, target))).isTrue();
	}

	@Test
	@DisplayName("#isWebmentionValid returns false if response does not contain link")
	void isWebmentionValidChecksContentFalse() throws Exception {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="https://example.com">cool site</a>
			</body>
			</html>""")));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://foo.example.org");
		assertThat(verificationService.isWebmentionValid(HTTP_CLIENT_EXTENSION.get(),
			new Webmention(source, target))).isFalse();
	}
}
