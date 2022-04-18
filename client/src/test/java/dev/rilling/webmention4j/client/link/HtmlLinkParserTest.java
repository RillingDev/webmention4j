package dev.rilling.webmention4j.client.link;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlLinkParserTest {

	final HtmlLinkParser htmlLinkParser = new HtmlLinkParser();

	@Test
	@DisplayName("#parse gets links")
	void parseGetsLinks() throws IOException {

		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			response.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
					<link href="http://aaronpk.example/webmention-endpoint1" rel="webmention" />
					<link href="http://aaronpk.example/webmention-endpoint2" rel="webmention" />
				</head>
				<body>
					<a href="http://aaronpk.example/webmention-endpoint3" rel="webmention">webmention</a>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlLinkParser.parse(URI.create("https://example.com"),
				response)).containsExactlyInAnyOrder(new Link(URI.create("http://aaronpk.example/webmention-endpoint1"),
					Set.of("webmention")),
				new Link(URI.create("http://aaronpk.example/webmention-endpoint2"), Set.of("webmention")),
				new Link(URI.create("http://aaronpk.example/webmention-endpoint3"), Set.of("webmention")));
		}
	}

	@Test
	@DisplayName("#parse ignores non-HTML responses")
	void parseIgnoresNonHtml() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
			response.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
					<link href="https://example.com" rel="webmention" />
				</head>
				<body>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlLinkParser.parse(URI.create("https://example.com"), response)).isEmpty();
		}
	}

	@Test
	@DisplayName("#parse wraps exceptions for invalid link href")
	void parseHandlesInvalidLinkHref() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			response.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
					<link href="huh? this looks wrong." rel="webmention" />
				</head>
				<body>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThatThrownBy(() -> htmlLinkParser.parse(URI.create("https://example.com"), response)).isNotNull()
				.isInstanceOf(IOException.class);
		}
	}

}
