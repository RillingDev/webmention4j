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
class JsonVerifierSpecTest {

	final JsonVerifier jsonVerifier = new JsonVerifier();

	@Test
	@DisplayName("'In a JSON document, the receiver should look for properties whose values are an exact match for " +
		"the URL' (found)")
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
	@DisplayName("'In a JSON document, the receiver should look for properties whose values are an exact match for " +
		"the URL' (not found)")
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
	@DisplayName("'In a JSON document, the receiver should look for properties whose values are an exact match for " +
		"the URL' (found, nested)")
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
