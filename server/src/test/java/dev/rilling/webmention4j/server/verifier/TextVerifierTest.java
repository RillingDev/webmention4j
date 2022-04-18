package dev.rilling.webmention4j.server.verifier;

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

import static org.assertj.core.api.Assertions.assertThat;

class TextVerifierTest {

	final TextVerifier textVerifier = new TextVerifier();

	@Test
	@DisplayName("#isValid returns true if the substring is found")
	void isValidTrueIfSubstring() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
			response.setEntity(new StringEntity("""
				Cool thing: https://example.com
				""", StandardCharsets.UTF_8));

			assertThat(textVerifier.isValid(response, URI.create("https://example.com"))).isTrue();
		}
	}

	@Test
	@DisplayName("#isValid returns false if the substring is not found")
	void isValidFalseIfNoSubstring() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
			response.setEntity(new StringEntity("""
				Cool thing: https://example.com
				""", StandardCharsets.UTF_8));

			assertThat(textVerifier.isValid(response, URI.create("https://foo.example.org"))).isFalse();
		}
	}
}
