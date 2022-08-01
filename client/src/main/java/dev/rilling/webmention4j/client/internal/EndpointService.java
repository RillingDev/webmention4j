package dev.rilling.webmention4j.client.internal;

import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.internal.HttpUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Service handling endpoint contact.
 */
public final class EndpointService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointService.class);

	/**
	 * Sends a Webmention request to the given endpoint.
	 *
	 * @param httpClient HTTP client.
	 *                   Must be configured to follow redirects.
	 *                   Should be configured to use a fitting UA string.
	 * @param endpoint   Endpoint. See {@link EndpointDiscoveryService}.
	 * @param webmention Webmention to send.
	 * @return URL to use to monitor request status (if supported by the endpoint server).
	 * @throws IOException if I/O fails.
	 */
	// Spec: https://www.w3.org/TR/webmention/#h-sender-notifies-receiver
	@NotNull
	public Optional<URI> notifyEndpoint(@NotNull CloseableHttpClient httpClient,
										@NotNull URI endpoint,
										@NotNull Webmention webmention) throws IOException {
		/*
		 * Spec:
		 * 'The sender MUST post x-www-form-urlencoded source and target parameters to the Webmention endpoint,
		 * where source is the URL of the sender's page containing a link,
		 * and target is the URL of the page being linked to.'
		 *
		 * 'Note that if the Webmention endpoint URL contains query string parameters,
		 * the query string parameters MUST be preserved, and MUST NOT be sent in the POST body.'
		 */
		BasicNameValuePair sourcePair = new BasicNameValuePair("source", webmention.source().toString());
		BasicNameValuePair targetPair = new BasicNameValuePair("target", webmention.target().toString());
		ClassicHttpRequest request = ClassicRequestBuilder.post(endpoint)
			.addParameters(sourcePair, targetPair)
			.setCharset(StandardCharsets.UTF_8) // Not part of spec, but probably better than ISO
			.build();

		LOGGER.debug("Sending request '{}'.", request);
		try (ClassicHttpResponse response = httpClient.execute(request)) {
			LOGGER.trace("Received response '{}' from '{}'.", response, endpoint);

			/*
			 * Spec:
			 * 'The Webmention endpoint will validate and process the request, and return an HTTP status code.
			 * Most often, 202 Accepted or 201 Created will be returned,
			 * indicating that the request is queued and being processed asynchronously to prevent DoS (Denial of Service) attacks.
			 * If the response code is 201,
			 * the Location header will include a URL that can be used to monitor the status of the request.
			 *
			 * 'Any 2xx response code MUST be considered a success.'
			 */

			HttpUtils.validateResponse(response);

			/*
			 * Spec:
			 * 'If the response code is 201,
			 * the Location header will include a URL that can be used to monitor the status of the request.'
			 */
			if (response.getCode() == HttpStatus.SC_CREATED) {
				return HttpUtils.extractLocation(response);
			}
			return Optional.empty();
		}
	}


}
