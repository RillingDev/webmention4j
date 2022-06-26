package dev.rilling.webmention4j.server;

import dev.rilling.webmention4j.common.Webmention;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("serial")
public final class NoopWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {
	@Override
	protected void handleWebmention(@NotNull Webmention webmention) {

	}
}
