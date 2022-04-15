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

	private static final Server SERVER = new Server(0);
	private static URI SERVLET_URI;
	private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

	@BeforeAll
	static void beforeAll() throws Exception {
		SERVER.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(WebmentionEndpointServlet.class, "/");
		SERVER.setHandler(servletHandler);

		SERVER.start();

		int port = ((NetworkConnector) SERVER.getConnectors()[0]).getLocalPort();
		SERVLET_URI = URIBuilder.localhost().setScheme("http").setPort(port).setPath("/").build();
	}

	@AfterAll
	static void afterAll() throws Exception {
		SERVER.stop();

		HTTP_CLIENT.close();
	}

	@Test
	@DisplayName("Misc: 'Validates Content-Type'")
	void validatesContentType() throws Exception {
		ClassicHttpRequest request = ClassicRequestBuilder.post(SERVLET_URI)
			.addHeader("Content-Type", "text/plain")
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Content type must be &apos;application/x-www-form-urlencoded&apos;.");
		}
	}

	@Test
	@DisplayName("Misc: 'Validates presence of parameters'")
	void validatesParameters() throws Exception {
		ClassicHttpRequest request1 = ClassicRequestBuilder.post(SERVLET_URI)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT.execute(request1)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Required parameter &apos;source&apos; is missing.");
		}

		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(SERVLET_URI)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameter(sourcePair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT.execute(request2)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Required parameter &apos;target&apos; is missing.");
		}
	}


	@Test
	@DisplayName("Misc: 'Validates scheme of parameters'")
	void validatesParameterScheme() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "ftp://example.org");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(SERVLET_URI)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT.execute(request2)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String message = EntityUtils.toString(response.getEntity());
			assertThat(message).contains("Unsupported URL scheme &apos;ftp&apos;.");
		}
	}

	@Test
	@DisplayName("Spec: 'MUST respond with a 200 OK status on success'")
	void returnsOk() throws Exception {
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", "https://example.com");
		BasicNameValuePair targetPair = new BasicNameValuePair("target", "https://example.org");
		ClassicHttpRequest request2 = ClassicRequestBuilder.post(SERVLET_URI)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.addParameters(sourcePair, targetPair)
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT.execute(request2)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_OK);
		}
	}
}
