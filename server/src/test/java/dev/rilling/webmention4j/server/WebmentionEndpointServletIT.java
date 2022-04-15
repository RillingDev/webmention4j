package dev.rilling.webmention4j.server;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;


class WebmentionEndpointServletIT {

	private static Server server;
	private static URI servletUri;
	private static CloseableHttpClient httpClient;

	@BeforeAll
	static void beforeAll() throws Exception {
		server = new Server(0);
		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(WebmentionEndpointServlet.class, "/");
		server.setHandler(servletHandler);

		server.start();

		int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
		servletUri = URIBuilder.localhost().setScheme("http").setPort(port).setPath("/").build();

		httpClient = HttpClients.createDefault();
	}

	@AfterAll
	static void afterAll() throws Exception {
		server.stop();

		httpClient.close();
	}

	@Test
	@DisplayName("Misc: 'Validates Content-Type'")
	void validatesContentType() throws Exception {
		ClassicHttpRequest request = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "text/plain")
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Content type must be &apos;application/x-www-form-urlencoded&apos;.");
		}
	}

	@Test
	@DisplayName("Spec: 'The receiver MUST check that source and target are valid URLs' (presence)")
	void validatesParameterPresence() throws Exception {
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request1)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Required parameter &apos;source&apos; is missing.");
		}

		BasicNameValuePair sourcePair2 = new BasicNameValuePair("source", "https://example.com");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameter(sourcePair2)
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request2)) {
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
		ClassicHttpRequest request = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request)) {
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
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair1, targetPair1)
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request1)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Unsupported URL scheme.");
		}


		BasicNameValuePair sourcePair2 = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair2 = new BasicNameValuePair("target", "is-this-even-legal");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair2, targetPair2)
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request2)) {
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
		ClassicHttpRequest request = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Source and target URL may not be identical.");
		}
	}

	@Test
	@DisplayName("Spec: 'MUST respond with a 200 OK status on success'")
	void returnsOk() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.org");
		ClassicHttpRequest request = ClassicRequestBuilder.post(servletUri)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
		}
	}
}
