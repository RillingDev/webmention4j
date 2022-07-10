package dev.rilling.webmention4j.common.util;

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

import static dev.rilling.webmention4j.common.util.HttpUtils.*;
import static org.assertj.core.api.Assertions.*;

class HttpUtilsTest {
	@Test
	@DisplayName("#validateResponse throws nothing when successful")
	void validateResponseSuccess() throws IOException {
		try (ClassicHttpResponse okResponse = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			okResponse.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
			okResponse.setEntity(new StringEntity("foo", StandardCharsets.UTF_8));

			assertThatNoException().isThrownBy(() -> validateResponse(okResponse));
		}

		try (ClassicHttpResponse createdResponse = new BasicClassicHttpResponse(HttpStatus.SC_CREATED)) {
			createdResponse.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
			createdResponse.setEntity(new StringEntity("foo", StandardCharsets.UTF_8));

			assertThatNoException().isThrownBy(() -> validateResponse(createdResponse));
		}

		try (ClassicHttpResponse acceptedResponse = new BasicClassicHttpResponse(HttpStatus.SC_ACCEPTED)) {
			acceptedResponse.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN.toString());
			acceptedResponse.setEntity(new StringEntity("foo", StandardCharsets.UTF_8));

			assertThatNoException().isThrownBy(() -> validateResponse(acceptedResponse));
		}
	}

	@Test
	@DisplayName("#validateResponse throws on error")
	void validateResponseError() throws IOException {
		try (ClassicHttpResponse notFoundResponse = new BasicClassicHttpResponse(HttpStatus.SC_NOT_FOUND)) {
			assertThatThrownBy(() -> validateResponse(notFoundResponse)).isInstanceOf(IOException.class).hasMessage("""
				Request failed: 404 - Not Found:
				<no body>""");
		}

		try (ClassicHttpResponse internalErrorResponse = new BasicClassicHttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR)) {
			assertThatThrownBy(() -> validateResponse(internalErrorResponse)).isInstanceOf(IOException.class)
				.hasMessage("""
					Request failed: 500 - Internal Server Error:
					<no body>""");
		}
	}

	@Test
	@DisplayName("#extractContentType extracts content type")
	void extractContentTypeExtracts() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
			response.setEntity(new StringEntity("{}", StandardCharsets.UTF_8));

			assertThat(extractContentType(response)).isPresent()
				.get()
				.matches(ContentType.APPLICATION_JSON::isSameMimeType);
		}
	}

	@Test
	@DisplayName("#extractContentType is empty if no content type is specified")
	void extractContentTypeEmpty() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_CREATED)) {
			assertThat(extractContentType(response)).isEmpty();
		}
	}

	@Test
	@DisplayName("#extractLocation extracts URL")
	void extractLocationExtractsUrl() throws Exception {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_CREATED)) {
			response.setHeader(HttpHeaders.LOCATION, "http://example.com");

			assertThat(extractLocation(response)).isPresent().contains(new URI("http://example.com"));
		}
	}

	@Test
	@DisplayName("#extractLocation is empty if no location is specified")
	void extractLocationEmptyNoLocation() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_CREATED)) {
			assertThat(extractLocation(response)).isEmpty();
		}
	}

	@Test
	@DisplayName("#extractLocation throws on invalid URL")
	void extractLocationForInvalidUrl() throws Exception {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_CREATED)) {
			response.setHeader(HttpHeaders.LOCATION, "http:/\\//\\");

			assertThatThrownBy(() -> extractLocation(response)).isInstanceOf(IOException.class)
				.hasMessage("Could not parse location header.");
		}
	}


}
