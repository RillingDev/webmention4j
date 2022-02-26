package dev.rilling.webmention4j.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;

public final class WebmentionSender {

	private final URI endpoint;

	private WebmentionSender(URI endpoint) {
		this.endpoint = endpoint;
	}

	public URI getEndpoint() {
		return endpoint;
	}

	public static WebmentionSender forUri(URI uri) throws IOException, InterruptedException {
		// 3.1.2: Follow all redirects.
		// TODO: set fitting UA
		HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
		EndpointDiscoveryService endpointDiscoveryService = new EndpointDiscoveryService(httpClient);

		URI endpoint = endpointDiscoveryService.discover(uri).orElseThrow(() -> new IOException(
			"Could not find any webmention endpoint URI in the target resource."));

		return new WebmentionSender(endpoint);
	}

}
