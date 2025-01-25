package dev.rilling.webmention4j.server;

import dev.rilling.webmention4j.common.Webmention;

@SuppressWarnings("serial")
public final class NoopWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {
	@Override
	protected void handleWebmention(Webmention webmention) {

	}
}
