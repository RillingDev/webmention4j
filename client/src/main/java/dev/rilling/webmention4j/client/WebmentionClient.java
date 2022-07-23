package dev.rilling.webmention4j.client;

import dev.rilling.webmention4j.client.impl.EndpointDiscoveryService;
import dev.rilling.webmention4j.client.impl.EndpointService;
import dev.rilling.webmention4j.client.impl.LocalhostRejectingRedirectStrategy;
import dev.rilling.webmention4j.client.impl.link.HeaderLinkParser;
import dev.rilling.webmention4j.client.impl.link.HtmlLinkParser;
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.util.HttpUtils;
import dev.rilling.webmention4j.common.util.UriUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

// Spec: '3.1 Sending Webmentions'
public final class WebmentionClient {
	private final EndpointDiscoveryService endpointDiscoveryService;
	private final EndpointService endpointService;
	private final HttpClientFactory httpClientFactory;
	private final Config config;


	/**
	 * Creates a new client with the default configuration.
	 */
	public WebmentionClient() {
		this(new Config());
	}

	/**
	 * Creates a new client with a custom configuration.
	 *
	 * @param config Custom configuration.
	 */
	public WebmentionClient(@NotNull Config config) {
		this(new Config(config),
			WebmentionClient::createDefaultHttpClient,
			new EndpointService(),
			new EndpointDiscoveryService(new HeaderLinkParser(), new HtmlLinkParser()));
	}

	WebmentionClient(@NotNull Config config,
					 @NotNull HttpClientFactory httpClientFactory,
					 @NotNull EndpointService endpointService,
					 @NotNull EndpointDiscoveryService endpointDiscoveryService) {
		this.config = config;
		this.endpointDiscoveryService = endpointDiscoveryService;
		this.endpointService = endpointService;
		this.httpClientFactory = httpClientFactory;
	}

	// TODO: support async requests

	/**
	 * Checks if a Webmention endpoint exists for this target URL.
	 *
	 * @param target Page to check endpoint of.
	 * @throws IOException if I/O fails.
	 */
	public boolean supportsWebmention(@NotNull URI target) throws IOException {
		try (CloseableHttpClient httpClient = httpClientFactory.create(true)) {
			return endpointDiscoveryService.discoverEndpoint(httpClient, target).isPresent();
		}
	}

	/**
	 * Notifies the target page that it was mention by the source page.
	 *
	 * @param webmention Webmention to send.
	 * @return URL to use to monitor request status (if supported by the endpoint server).
	 * @throws IOException if I/O fails.
	 */
	@NotNull
	public Optional<URI> sendWebmention(@NotNull Webmention webmention) throws IOException {
		URI endpoint;
		try (CloseableHttpClient httpClient = httpClientFactory.create(true)) {
			// Spec: '3.1.2 Sender discovers receiver Webmention endpoint'
			endpoint = endpointDiscoveryService.discoverEndpoint(httpClient, webmention.target())
				.orElseThrow(() -> new IOException("Could not find any webmention endpoint URL in the target resource."));
		}

		/*
		 * Spec:
		 * 'During the discovery step, if the sender discovers the endpoint is localhost or a loopback IP address (127.0.0.0/8),
		 *  it SHOULD NOT send the Webmention to that endpoint.'
		 *
		 * Note that this is check needs to also be done following redirects (see #createDefaultHttpClient).
		 */
		if (!config.isAllowLocalhostEndpoint() && UriUtils.isLocalhost(endpoint)) {
			throw new IOException(("Endpoint '%s' is localhost or a loopback IP address, refusing to notify.").formatted(
				endpoint));
		}
		try (CloseableHttpClient httpClient = httpClientFactory.create(config.isAllowLocalhostEndpoint())) {
			// Spec: '3.1.3 Sender notifies receiver'
			return endpointService.notifyEndpoint(httpClient, endpoint, webmention);
		}
	}

	/**
	 * Configuration POJO.
	 */
	public static class Config {
		private boolean allowLocalhostEndpoint;

		/**
		 * Creates a new configuration with default values.
		 */
		public Config() {
			allowLocalhostEndpoint = false;
		}

		private Config(@NotNull Config original) {
			allowLocalhostEndpoint = original.allowLocalhostEndpoint;
		}

		/**
		 * Configures if the client should send Webmentions to an endpoint that is localhost or a loopback IP address.
		 * Defaults to {@code false}.
		 * <br>
		 * From the <a href="https://www.w3.org/TR/webmention/#avoid-sending-webmentions-to-localhost">specification</a>:
		 * <br>
		 * "When the sender discovers the receiver's Webmention endpoint,
		 * there are few legitimate reasons for the endpoint to be localhost or any other loopback address.
		 * If the sender has any services that listen on localhost that don't require authentication,
		 * it's possible for a malicious Webmention receiver to craft a Webmention endpoint
		 * that could cause the sender to make an arbitrary POST request to itself."
		 */
		public void setAllowLocalhostEndpoint(boolean allowLocalhostEndpoint) {
			this.allowLocalhostEndpoint = allowLocalhostEndpoint;
		}

		/**
		 * @see #setAllowLocalhostEndpoint(boolean)
		 */
		public boolean isAllowLocalhostEndpoint() {
			return allowLocalhostEndpoint;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Config config = (Config) o;
			return allowLocalhostEndpoint == config.allowLocalhostEndpoint;
		}

		@Override
		public int hashCode() {
			return Objects.hash(allowLocalhostEndpoint);
		}

		@Override
		public String toString() {
			return "Config{" + "allowLocalhostEndpoint=" + allowLocalhostEndpoint + '}';
		}
	}

	@FunctionalInterface
	private interface HttpClientFactory {
		CloseableHttpClient create(boolean allowLocalhostRedirect);
	}

	@NotNull
	private static CloseableHttpClient createDefaultHttpClient(boolean allowLocalhostRedirect) {
		/*
		 * Spec:
		 * 'Senders MAY customize the HTTP User Agent used when fetching the target URL
		 *  in order to indicate to the recipient that this request is made as part of Webmention discovery.
		 *  In this case, it is recommended to include the string "Webmention" in the User Agent.
		 *  This provides people with a pointer to find out why the discovery request was made.'
		 */
		HttpClientBuilder builder = HttpClients.custom();
		if (!allowLocalhostRedirect) {
			/*
			 * Spec:
			 * 'During the discovery step, if the sender discovers the endpoint is localhost or a loopback IP address (127.0.0.0/8),
			 *  it SHOULD NOT send the Webmention to that endpoint.'
			 */
			builder.setRedirectStrategy(new LocalhostRejectingRedirectStrategy());
		}
		return builder.setUserAgent(HttpUtils.createUserAgentString("webmention4j-client",
			WebmentionClient.class.getPackage())).build();
	}

}
