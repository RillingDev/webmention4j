package dev.rilling.webmention4j.client;

import dev.rilling.webmention4j.client.link.HeaderLinkParser;
import dev.rilling.webmention4j.client.link.HtmlLinkParser;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

// Verify against https://webmention.rocks/
class EndpointDiscoveryServiceIT {

	final EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService(HttpClients::createDefault,
		new HeaderLinkParser(),
		new HtmlLinkParser());

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
		"https://webmention.rocks/test/12, https://webmention.rocks/test/12/webmention",
		"https://webmention.rocks/test/13, https://webmention.rocks/test/13/webmention",
		"https://webmention.rocks/test/14, https://webmention.rocks/test/14/webmention",
		"https://webmention.rocks/test/15, https://webmention.rocks/test/15",
		"https://webmention.rocks/test/16, https://webmention.rocks/test/16/webmention",
		"https://webmention.rocks/test/17, https://webmention.rocks/test/17/webmention",
		"https://webmention.rocks/test/18, https://webmention.rocks/test/18/webmention",
		/*
		 * "https://webmention.rocks/test/19, https://webmention.rocks/test/19/webmention",
		 * Disabled as Jerseys parser seems to not support this.
		 */
		"https://webmention.rocks/test/20, https://webmention.rocks/test/20/webmention",
		"https://webmention.rocks/test/21, https://webmention.rocks/test/21/webmention?query=yes",
		"https://webmention.rocks/test/22, https://webmention.rocks/test/22/webmention"
		/*
		 * "https://webmention.rocks/test/23,
		 *  https://webmention.rocks/test/23/page/webmention-endpoint/V2POoo8odnb11d1S0OPD"
		 * handled via actual endpoint notification later on
		 */})
	void test1(String targetStr, String actualEndpointStr) throws IOException {
		assertThat(endpointDiscoveryService.discoverEndpoint(URI.create(targetStr))).contains(URI.create(
			actualEndpointStr));
	}
}
