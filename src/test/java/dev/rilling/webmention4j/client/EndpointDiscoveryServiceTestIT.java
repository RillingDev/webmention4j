package dev.rilling.webmention4j.client;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

// Verify against https://webmention.rocks/
class EndpointDiscoveryServiceTestIT {

	final EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService(HttpClients::createDefault);

	@ParameterizedTest
	@CsvSource({"https://webmention.rocks/test/1, https://webmention.rocks/test/1/webmention",
		"https://webmention.rocks/test/2, https://webmention.rocks/test/2/webmention",
		"https://webmention.rocks/test/3, https://webmention.rocks/test/3/webmention",
		"https://webmention.rocks/test/4, https://webmention.rocks/test/4/webmention",
		"https://webmention.rocks/test/5, https://webmention.rocks/test/5/webmention",
		"https://webmention.rocks/test/6, https://webmention.rocks/test/6/webmention",
		"https://webmention.rocks/test/7, https://webmention.rocks/test/7/webmention",
		"https://webmention.rocks/test/8, https://webmention.rocks/test/8/webmention",
		"https://webmention.rocks/test/9, https://webmention.rocks/test/9/webmention",
		"https://webmention.rocks/test/10, https://webmention.rocks/test/10/webmention",
		"https://webmention.rocks/test/11, https://webmention.rocks/test/11/webmention",
		"https://webmention.rocks/test/12, https://webmention.rocks/test/12/webmention",})
	void test1(String targetStr, String actualEndpointStr) throws IOException {
		assertThat(endpointDiscoveryService.discoverEndpoint(URI.create(targetStr))).contains(URI.create(
			actualEndpointStr));
	}
}
