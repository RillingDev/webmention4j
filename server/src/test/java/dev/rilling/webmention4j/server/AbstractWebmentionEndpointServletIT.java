package dev.rilling.webmention4j.server;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.rilling.webmention4j.common.test.AutoClosableExtension;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;


class AbstractWebmentionEndpointServletIT {

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
	@DisplayName("Validates content type")
	void validatesContentType() throws Exception {
		ClassicHttpRequest request = ClassicRequestBuilder.post(ENDPOINT_SERVER.getServletUri())
			.addHeader("Content-Type", "text/plain")
			.build();

		try (CloseableHttpResponse response = HTTP_CLIENT_EXTENSION.get().execute(request)) {
			assertThat(response.getCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			String actualMessage = EntityUtils.toString(response.getEntity());
			assertThat(actualMessage).contains("Content type must be &apos;application/x-www-form-urlencoded&apos;.");
		}
	}

}
