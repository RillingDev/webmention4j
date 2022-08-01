package dev.rilling.webmention4j.server.internal.verifier;

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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("spec")
class JsonVerifierTest {

	final JsonVerifier jsonVerifier = new JsonVerifier();

	@Test
	@DisplayName("#isValid throws on invalid JSON")
	void isValidThrowsIfInvalid() throws IOException {
		try (ClassicHttpResponse response1 = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response1.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
			response1.setEntity(new StringEntity("huh?", StandardCharsets.UTF_8));

			assertThatThrownBy(() -> jsonVerifier.isValid(response1, URI.create("https://example.com"))).isInstanceOf(
				IOException.class);
		}

		try (ClassicHttpResponse response2 = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response2.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
			response2.setEntity(new StringEntity("{{{{{{{{{{{", StandardCharsets.UTF_8));

			assertThatThrownBy(() -> jsonVerifier.isValid(response2, URI.create("https://example.com"))).isInstanceOf(
				IOException.class);
		}
	}
}
