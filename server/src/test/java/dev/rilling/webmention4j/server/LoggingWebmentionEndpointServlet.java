package dev.rilling.webmention4j.server;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.net.URI;

public final class LoggingWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWebmentionEndpointServlet.class);

	@Serial
	private static final long serialVersionUID = -3936170599783265569L;

	public LoggingWebmentionEndpointServlet() {
		super(verificationService);
	}

	@Override
	protected void handleSubmission(@NotNull URI source, @NotNull URI target) {
		LOGGER.info("Received submission from source '{}'  with target '{}'.", source, target);
	}
}
