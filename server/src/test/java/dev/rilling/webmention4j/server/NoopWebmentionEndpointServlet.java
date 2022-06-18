package dev.rilling.webmention4j.server;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

@SuppressWarnings("serial")
public final class NoopWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {
	@Override
	protected void handleWebmention(@NotNull URI source, @NotNull URI target) {

	}
}
