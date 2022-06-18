package dev.rilling.webmention4j.client.impl;

import dev.rilling.webmention4j.common.util.HttpUtils;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * Variant of {@link DefaultRedirectStrategy} that ignores redirects to localhost.
 * <p>
 * This may be useful if redirecting to localhost is security sensitive.
 */
public class LocalhostIgnoringRedirectStrategy extends DefaultRedirectStrategy {
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalhostIgnoringRedirectStrategy.class);

	@Override
	public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
		throws ProtocolException {
		try {
			if (HttpUtils.isLocalhost(getLocationURI(request, response, context))) {
				LOGGER.warn("Ignoring redirect to localhost.");
				return false;
			}
		} catch (UnknownHostException | HttpException e) {
			throw new ProtocolException("Failed to check redirect location.", e);
		}
		return super.isRedirected(request, response, context);
	}
}
