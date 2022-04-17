package dev.rilling.webmention4j.client.link;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderLinkParserTest {

	final HeaderLinkParser headerLinkParser = new HeaderLinkParser();

	@Test
	@DisplayName("#parse wraps exceptions for invalid header formats")
	void parseHandlesInvalidHeader() throws IOException {
		try (ClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.SC_OK)) {
			response.setHeader(HttpHeaders.LINK, "huh? this looks wrong.");

			assertThatThrownBy(() -> headerLinkParser.parse(URI.create("https://example.com"), response)).isNotNull()
				.isInstanceOf(LinkParser.LinkParsingException.class);
		}
	}
}
