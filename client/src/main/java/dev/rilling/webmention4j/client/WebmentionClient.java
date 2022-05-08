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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

// Spec: '3.1 Sending Webmentions'
public final class WebmentionClient {
	private final EndpointDiscoveryService endpointDiscoveryService;
	private final EndpointService endpointService;
	private final Supplier<CloseableHttpClient> httpClientFactory;
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
					 @NotNull Supplier<CloseableHttpClient> httpClientFactory,
					 @NotNull EndpointService endpointService,
					 @NotNull EndpointDiscoveryService endpointDiscoveryService) {
		this.config = config;
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
			// Spec: '3.1.2 Sender discovers receiver Webmention endpoint'
			URI endpoint = endpointDiscoveryService.discoverEndpoint(httpClient, target)
				.orElseThrow(() -> new IOException("Could not find any webmention endpoint URL in the target resource."));

			/*
			 * Spec:
			 * 'During the discovery step, if the sender discovers the endpoint is localhost or a loopback IP address (127.0.0.0/8),
			 *  it SHOULD NOT send the Webmention to that endpoint.'
			 */
			// TODO: also perform this check when following redirects during notification.
			if (!config.isAllowLocalhostEndpoint() && HttpUtils.isLocalhost(endpoint)) {
				throw new IOException(("Endpoint '%s' is localhost or a loopback IP address, refusing to notify.").formatted(
					endpoint));
			}

			// Spec: '3.1.3 Sender notifies receiver'
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

		Config(@NotNull Config original) {
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

	;
}
