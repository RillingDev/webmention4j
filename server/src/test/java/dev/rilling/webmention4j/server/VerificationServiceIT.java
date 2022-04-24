package dev.rilling.webmention4j.server;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import dev.rilling.webmention4j.common.AutoClosableExtension;
import dev.rilling.webmention4j.server.verifier.HtmlVerifier;
import dev.rilling.webmention4j.server.verifier.JsonVerifier;
import dev.rilling.webmention4j.server.verifier.TextVerifier;
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
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
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
	@DisplayName("#isSubmissionValid throws on non-success response")
	void isSubmissionValidThrowsOnError() {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(notFound()));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");

		assertThatThrownBy(() -> verificationService.isSubmissionValid(HTTP_CLIENT_EXTENSION.get(),
			source,
			target)).isNotNull().isInstanceOf(IOException.class);
	}

	@Test
	@DisplayName("#isSubmissionValid sets 'Accept' header based on supported formats")
	void isSubmissionValidSetsAccept() throws Exception {
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
		verificationService.isSubmissionValid(HTTP_CLIENT_EXTENSION.get(), source, target);

		UrlPattern urlPattern = new UrlPattern(new EqualToPattern("/blog/post", false), false);
		SOURCE_SERVER.verify(newRequestPattern(RequestMethod.GET, urlPattern).withHeader(HttpHeaders.ACCEPT,
			new EqualToPattern("text/html, text/plain, application/json")));
	}

	@Test
	@DisplayName("#isSubmissionValid throws on 'Not Acceptable' response")
	void isSubmissionValidThrowsOnNotAcceptable() {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(aResponse().withStatus(HttpStatus.SC_NOT_ACCEPTABLE)));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");

		assertThatThrownBy(() -> verificationService.isSubmissionValid(HTTP_CLIENT_EXTENSION.get(),
			source,
			target)).isNotNull().isInstanceOf(VerificationService.UnsupportedContentTypeException.class);
	}

	@Test
	@DisplayName("#isSubmissionValid throws on unspecified content type")
	void isSubmissionValidThrowsOnUnknownContentType() {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE, "text/weird")));

		URI source = URI.create(SOURCE_SERVER.url("/blog/post"));
		URI target = URI.create("https://example.com");

		assertThatThrownBy(() -> verificationService.isSubmissionValid(HTTP_CLIENT_EXTENSION.get(),
			source,
			target)).isNotNull().isInstanceOf(VerificationService.UnsupportedContentTypeException.class);
	}

	@Test
	@DisplayName("#isSubmissionValid returns true if response contains link")
	void isSubmissionValidChecksContentTrue() throws Exception {
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
		assertThat(verificationService.isSubmissionValid(HTTP_CLIENT_EXTENSION.get(), source, target)).isTrue();
	}

	@Test
	@DisplayName("#isSubmissionValid returns false if response does not contain link")
	void isSubmissionValidChecksContentFalse() throws Exception {
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
		assertThat(verificationService.isSubmissionValid(HTTP_CLIENT_EXTENSION.get(), source, target)).isFalse();
	}
}
