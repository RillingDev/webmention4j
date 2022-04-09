package dev.rilling.webmention4j.client;

import dev.rilling.webmention4j.client.link.HeaderLinkParser;
import dev.rilling.webmention4j.client.link.HtmlLinkParser;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

public final class WebmentionClient {
	private final EndpointDiscoveryService endpointDiscoveryService;
	private final EndpointService endpointService;

	public WebmentionClient() {
		this(HttpClients::createDefault);
	}

	public WebmentionClient(@NotNull Supplier<CloseableHttpClient> httpClientFactory) {
		this(new EndpointService(httpClientFactory),
			new EndpointDiscoveryService(httpClientFactory, new HeaderLinkParser(), new HtmlLinkParser()));
	}

	WebmentionClient(EndpointService endpointService, EndpointDiscoveryService endpointDiscoveryService) {
		this.endpointDiscoveryService = endpointDiscoveryService;
		this.endpointService = endpointService;
	}

	public void send(URI source, URI target) throws IOException {
		// 3.1.2: Follow all redirects.
		// TODO: set fitting UA
		URI endpoint = endpointDiscoveryService.discoverEndpoint(target)
			.orElseThrow(() -> new IOException("Could not find any webmention endpoint URI in the target resource."));

		endpointService.notifyEndpoint(endpoint, source, target);
	}

}
