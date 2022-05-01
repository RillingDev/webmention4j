package dev.rilling.webmention4j.client;

import dev.rilling.webmention4j.client.impl.EndpointDiscoveryService;
import dev.rilling.webmention4j.client.impl.EndpointService;
import dev.rilling.webmention4j.client.impl.link.HeaderLinkParser;
import dev.rilling.webmention4j.client.impl.link.HtmlLinkParser;
import dev.rilling.webmention4j.common.util.HttpUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

public final class WebmentionClient {
	private final EndpointDiscoveryService endpointDiscoveryService;
	private final EndpointService endpointService;
	private final Supplier<CloseableHttpClient> httpClientFactory;

	public WebmentionClient() {
		this(WebmentionClient::createDefaultHttpClient);
	}

	private WebmentionClient(@NotNull Supplier<CloseableHttpClient> httpClientFactory) {
		this(httpClientFactory,
			new EndpointService(),
			new EndpointDiscoveryService(new HeaderLinkParser(), new HtmlLinkParser()));
	}

	WebmentionClient(@NotNull Supplier<CloseableHttpClient> httpClientFactory,
					 @NotNull EndpointService endpointService,
					 @NotNull EndpointDiscoveryService endpointDiscoveryService) {
		this.endpointDiscoveryService = endpointDiscoveryService;
		this.endpointService = endpointService;
		this.httpClientFactory = httpClientFactory;
	}

	// TODO: support async requests


	/**
	 * Checks if a webmention endpoint exists for this target URL.
	 *
	 * @param target Page to check endpoint of.
	 * @throws IOException if I/O fails.
	 */
	public boolean supportsWebmention(@NotNull URI target) throws IOException {
		try (CloseableHttpClient httpClient = httpClientFactory.get()) {
			return endpointDiscoveryService.discoverEndpoint(httpClient, target).isPresent();
		}
	}

	/**
	 * Notifies the target page that it was mention by the source page.
	 *
	 * @param source Source page that is mentioning the target.
	 * @param target Page being mentioned.
	 * @return URL to use to monitor request status (if supported by the endpoint server).
	 * @throws IOException if I/O fails.
	 */
	@NotNull
	public Optional<URI> sendWebmention(@NotNull URI source, @NotNull URI target) throws IOException {
		try (CloseableHttpClient httpClient = httpClientFactory.get()) {
			URI endpoint = endpointDiscoveryService.discoverEndpoint(httpClient, target)
				.orElseThrow(() -> new IOException(
					"Could not find any webmention endpoint URL in the target resource" + "."));

			return endpointService.notifyEndpoint(httpClient, endpoint, source, target);
		}
	}

	@NotNull
	private static CloseableHttpClient createDefaultHttpClient() {
		/*
		 * Spec:
		 * 'Senders MAY customize the HTTP User Agent used when fetching the target URL
		 *  in order to indicate to the recipient that this request is made as part of Webmention discovery.
		 *  In this case, it is recommended to include the string "Webmention" in the User Agent.
		 *  This provides people with a pointer to find out why the discovery request was made.'
		 */
		return HttpClients.custom()
			.setUserAgent(HttpUtils.createUserAgentString("webmention4j-client", WebmentionClient.class.getPackage()))
			.build();
	}
}
