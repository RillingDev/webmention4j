package dev.rilling.webmention4j.server.impl.verifier;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("spec")
class HtmlVerifierSpecTest {

	final HtmlVerifier htmlVerifier = new HtmlVerifier();

	@Test
	@DisplayName("'For example, in an HTML5 document, the receiver should look for <a href=\"*\">, <img " +
		"href=\"*\">, <video src=\"*\"> and other similar links' (anchor)")
	void isValidDetectsAnchorTags() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			response.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
				</head>
				<body>
					<a href="https://example.com">foo</a>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlVerifier.isValid(response, URI.create("https://example.com"))).isTrue();
		}
	}

	@Test
	@DisplayName("'For example, in an HTML5 document, the receiver should look for <a href=\"*\">, <img " +
		"href=\"*\">, <video src=\"*\"> and other similar links' (media)")
	void isValidDetectsMediaTags() throws IOException {
		try (ClassicHttpResponse imgResponse = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			imgResponse.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			imgResponse.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
				</head>
				<body>
					<img src="https://example.com/foo.png"/>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlVerifier.isValid(imgResponse, URI.create("https://example.com/foo.png"))).isTrue();
		}

		try (ClassicHttpResponse audioResponse = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			audioResponse.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			audioResponse.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
				</head>
				<body>
					<audio src="https://example.com/foo.mp3"/>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlVerifier.isValid(audioResponse, URI.create("https://example.com/foo.mp3"))).isTrue();
		}

		try (ClassicHttpResponse videoResponse = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			videoResponse.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			videoResponse.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
				</head>
				<body>
					<video src="https://example.com/foo.mpg"/>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlVerifier.isValid(videoResponse, URI.create("https://example.com/foo.mpg"))).isTrue();
		}
	}

	@Test
	@DisplayName("'The source document MUST have an exact match of the target URL provided in order for it to be considered a valid Webmention'")
	void isValidEnsuresLinkMustBeIdentical() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_HTML.toString());
			response.setEntity(new StringEntity("""
				<html lang="en">
				<head>
					<title>Foo</title>
				</head>
				<body>
					<a href="https://example.com/foo">foo</a>
				</body>
				</html>""", StandardCharsets.UTF_8));

			assertThat(htmlVerifier.isValid(response, URI.create("https://example.com/bar"))).isFalse();
		}
	}
}
