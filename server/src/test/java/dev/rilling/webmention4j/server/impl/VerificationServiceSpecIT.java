package dev.rilling.webmention4j.server.impl;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import dev.rilling.webmention4j.server.impl.verifier.HtmlVerifier;
import dev.rilling.webmention4j.server.impl.verifier.JsonVerifier;
import dev.rilling.webmention4j.server.impl.verifier.TextVerifier;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;

@Tag("spec")
class VerificationServiceSpecIT {

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
	@DisplayName("'The receiver SHOULD include an HTTP Accept header indicating its preference of content types that are acceptable'")
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
}
