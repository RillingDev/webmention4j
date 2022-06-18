package dev.rilling.webmention4j.server;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("spec")
class AbstractWebmentionEndpointServletSpecIT {

	@RegisterExtension
	static final WireMockExtension SOURCE_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@RegisterExtension
	static final ServletExtension ENDPOINT_SERVER = new ServletExtension("/endpoint",
		NoopWebmentionEndpointServlet.class);

	@RegisterExtension
	static final AutoClosableExtension<CloseableHttpClient> HTTP_CLIENT_EXTENSION = new AutoClosableExtension<>(
		HttpClients::createDefault);

	@Test
	@DisplayName("'The receiver MUST check that source and target are valid URLs' (presence)")
	void validatesParameterPresence() throws Exception {
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request1)) {
			assertErrorResponse(response,
				HttpStatus.SC_BAD_REQUEST,
				"Required parameter &apos;source&apos; is missing.");
		}

		BasicNameValuePair sourcePair2 = new BasicNameValuePair("source", "https://example.com");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameter(sourcePair2)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request2)) {
			assertErrorResponse(response,
				HttpStatus.SC_BAD_REQUEST,
				"Required parameter &apos;target&apos; is missing.");
		}
	}

	@Test
	@DisplayName("'The receiver MUST check that source and target are valid URLs' (format)")
	void validatesParameterFormat() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "http:/\\//\\");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertErrorResponse(response, HttpStatus.SC_BAD_REQUEST, "Invalid URL syntax: &apos;http:/\\//\\&apos;.");
		}
	}

	@Test
	@DisplayName("'The receiver MUST check that source and target [...] are of schemes that are supported by the receiver'")
	void validatesParameterScheme() throws Exception {
		BasicNameValuePair sourcePair1 = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair1 = new BasicNameValuePair("target", "ftp://example.org");
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair1, targetPair1)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request1)) {
			assertErrorResponse(response, HttpStatus.SC_BAD_REQUEST, "URL scheme &apos;ftp&apos; is not supported.");
		}


		BasicNameValuePair sourcePair2 = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair2 = new BasicNameValuePair("target", "is-this-even-legal");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair2, targetPair2)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request2)) {
			assertErrorResponse(response, HttpStatus.SC_BAD_REQUEST, "URL scheme &apos;null&apos; is not supported.");
		}
	}

	@Test
	@DisplayName("'The receiver MUST reject the request if the source URL is the same as the target URL'")
	void validatesParametersIdentical() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.com");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertErrorResponse(response, HttpStatus.SC_BAD_REQUEST, "Source and target URL must not be identical.");
		}
	}

	@Test
	@DisplayName("'If the receiver is going to use the Webmention in some way [...], then it MUST perform an " +
		"HTTP GET request on source [...], to confirm that it actually mentions the target.' (I/O error)")
	void rejectsOnFailedVerificationIoError() throws Exception {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(notFound()));

		BasicNameValuePair sourcePair = new BasicNameValuePair("source", SOURCE_SERVER.url("/blog/post"));
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.com");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertErrorResponse(response,
				HttpStatus.SC_BAD_REQUEST,
				"Verification of source URL could not be performed.");
		}
	}

	@Test
	@DisplayName("'If the receiver is going to use the Webmention in some way [...], then it MUST perform an " +
		"HTTP GET request on source [...], to confirm that it actually mentions the target.' (content type error)")
	void rejectsOnFailedVerificationVerificationError() throws Exception {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.create("text/weird").toString()).withBody("beep boop")));

		BasicNameValuePair sourcePair = new BasicNameValuePair("source", SOURCE_SERVER.url("/blog/post"));
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.com");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertErrorResponse(response,
				HttpStatus.SC_BAD_REQUEST,
				"Verification of source URL failed due to no supported content type being served.");
		}
	}

	@Test
	@DisplayName("'If the receiver is going to use the Webmention in some way [...], then it MUST perform an " +
		"HTTP GET request on source [...], to confirm that it actually mentions the target.' (no link to target in source)")
	void rejectsOnVerificationNoLinkFound() throws Exception {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader(HttpHeaders.CONTENT_TYPE,
			ContentType.TEXT_HTML.toString()).withBody("""
			<html lang="en">
			<head>
				<title>Foo</title>
			</head>
			<body>
				<a href="https://foo.example.org">cool site</a>
			</body>
			</html>""")));

		BasicNameValuePair sourcePair = new BasicNameValuePair("source", SOURCE_SERVER.url("/blog/post"));
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.com");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertErrorResponse(response, HttpStatus.SC_BAD_REQUEST, "Source does not contain link to target URL.");
		}
	}

	@Test
	@DisplayName("'MUST respond with a 200 OK status on success'")
	void returnsOk() throws Exception {
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

		BasicNameValuePair sourcePair = new BasicNameValuePair("source", SOURCE_SERVER.url("/blog/post"));
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.com");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
		}
	}

	private void assertErrorResponse(CloseableHttpResponse response, int statusCode, String message)
		throws IOException, ParseException {
		assertThat(response.getCode()).isEqualTo(statusCode);
		String actualMessage = EntityUtils.toString(response.getEntity());
		assertThat(actualMessage).contains(message);
	}
}
