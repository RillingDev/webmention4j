package dev.rilling.webmention4j.server;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@SuppressWarnings("serial")
class LoggingWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWebmentionEndpointServlet.class);

	@Override
	protected void handleSubmission(@NotNull URI source, @NotNull URI target) {
		LOGGER.info("Received submission from source '{}'  with target '{}'.", source, target);
	}
}
