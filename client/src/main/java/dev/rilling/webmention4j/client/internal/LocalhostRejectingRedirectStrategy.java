package dev.rilling.webmention4j.client.internal;

import dev.rilling.webmention4j.common.internal.UriUtils;
import org.apache.hc.client5.http.RedirectException;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.net.UnknownHostException;

/**
 * Variant of {@link DefaultRedirectStrategy} that rejects redirects to localhost.
 * <p>
 * This may be useful if redirecting to localhost is security sensitive.
 */
public class LocalhostRejectingRedirectStrategy extends DefaultRedirectStrategy {

	@Override
	public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
		throws ProtocolException {
		boolean redirected = super.isRedirected(request, response, context);
		if (!redirected) {
			return false;
		}

		if (isLocalhostRedirect(request, response, context)) {
			throw new RedirectException("Redirects to localhost are not supported in this context.");
		}
		return true;
	}

	private boolean isLocalhostRedirect(HttpRequest request, HttpResponse response, HttpContext context)
		throws ProtocolException {
		try {
			return UriUtils.isLocalhost(getLocationURI(request, response, context));
		} catch (UnknownHostException | HttpException e) {
			throw new ProtocolException("Failed to check redirect location.", e);
		}
	}
}
