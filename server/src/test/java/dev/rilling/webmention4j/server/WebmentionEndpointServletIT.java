package dev.rilling.webmention4j.server;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.common.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


class WebmentionEndpointServletIT {

	@RegisterExtension
	static final WireMockExtension SOURCE_SERVER = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort())
		.build();

	@RegisterExtension
	static final ServletExtension ENDPOINT_SERVER = new ServletExtension("/endpoint", WebmentionEndpointServlet.class);

	@RegisterExtension
	static final AutoClosableExtension<CloseableHttpClient> HTTP_CLIENT_EXTENSION = new AutoClosableExtension<>(
		HttpClients::createDefault);

	@Test
	@DisplayName("Misc: 'Validates Content-Type'")
	void validatesContentType() throws Exception {
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "text/plain")
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Content type must be &apos;application/x-www-form-urlencoded&apos;.");
		}
	}

	@Test
	@DisplayName("Spec: 'The receiver MUST check that source and target are valid URLs' (presence)")
	void validatesParameterPresence() throws Exception {
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request1)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Required parameter &apos;source&apos; is missing.");
		}

		BasicNameValuePair sourcePair2 = new BasicNameValuePair("source", "https://example.com");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameter(sourcePair2)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request2)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Required parameter &apos;target&apos; is missing.");
		}
	}

	@Test
	@DisplayName("Spec: 'The receiver MUST check that source and target are valid URLs' (format)")
	void validatesParameterFormat() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "http:/\\//\\");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Invalid URL syntax.");
		}
	}

	@Test
	@DisplayName("Spec: 'The receiver MUST check that source and target [...] are of schemes that are supported by the receiver'")
	void validatesParameterScheme() throws Exception {
		BasicNameValuePair sourcePair1 = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair1 = new BasicNameValuePair("target", "ftp://example.org");
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair1, targetPair1)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request1)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Unsupported URL scheme.");
		}


		BasicNameValuePair sourcePair2 = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair2 = new BasicNameValuePair("target", "is-this-even-legal");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair2, targetPair2)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request2)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Unsupported URL scheme.");
		}
	}


	@Test
	@DisplayName("Spec: 'The receiver MUST reject the request if the source URL is the same as the target URL'")
	void validatesParametersIdentical() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.com");
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Source and target URL may not be identical.");
		}
	}

	@Test
	@DisplayName("Spec: 'MUST respond with a 200 OK status on success'")
	void returnsOk() throws Exception {
		SOURCE_SERVER.stubFor(get("/blog/post").willReturn(ok().withHeader("Content-Type", "text/html").withBody("""
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
}
