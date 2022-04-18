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

class JsonVerifierTest {

	final JsonVerifier jsonVerifier = new JsonVerifier();

	@Test
	@DisplayName("#isValid returns true if a field value for the URL is found")
	void isValidTrueIfFound() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
			response.setEntity(new StringEntity("""
				{
					"name": "foo",
					"url": "https://example.com"
				}""", StandardCharsets.UTF_8));

			assertThat(jsonVerifier.isValid(response, URI.create("https://example.com"))).isTrue();
		}
	}

	@Test
	@DisplayName("#isValid returns false if no field value for the URL is found")
	void isValidFalseIfNotFound() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
			response.setEntity(new StringEntity("""
				{
					"name": "foo",
					"url": "https://foo.example.org"
				}""", StandardCharsets.UTF_8));

			assertThat(jsonVerifier.isValid(response, URI.create("https://example.com"))).isFalse();
		}
	}


	@Test
	@DisplayName("#isValid checks nested object nodes")
	void isValidChecksNested() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
			response.setEntity(new StringEntity("""
				{
					"name": "foo",
					"dates": [],
					"author": null,
					"meta": {
						"references": {
							"url": "https://example.com"
						}
					}
				}""", StandardCharsets.UTF_8));

			assertThat(jsonVerifier.isValid(response, URI.create("https://example.com"))).isTrue();
		}
	}
}
