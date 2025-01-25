package dev.rilling.webmention4j.server;

import dev.rilling.webmention4j.common.Webmention;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("serial")
public final class NoopWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {
	@Override
	protected void handleWebmention(@NonNull Webmention webmention) {

	}
}
