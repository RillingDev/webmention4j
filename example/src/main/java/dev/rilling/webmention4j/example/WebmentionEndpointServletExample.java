package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.server.AbstractWebmentionEndpointServlet;
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebmentionEndpointServletExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServletExample.class);

	private static final Option PORT = Option.builder()
		.option("p")
		.longOpt("port")
		.hasArg(true)
		.desc("Port to listen on.")
		.required(false)
		.build();
	private static final Options OPTIONS = new Options().addOption(PORT);

	private WebmentionEndpointServletExample() {
	}

	/**
	 * Starts endpoint server.
	 * <p>
	 * Arguments:
	 * <ol>
	 *     <li>Port to listen on.</li>
	 * </ol>
	 * <p>
	 * Security:
	 * This server is not designed to be exposed directly to the internet.
	 * Instead, a reverse proxy should be used that directs only HTTP traffic for the POST method to it.
	 */
	public static void main(String[] args) {
		CommandLine commandLine;
		try {
			commandLine = DefaultParser.builder().build().parse(OPTIONS, args);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse arguments.", e);
		}

		int port = Integer.parseInt(commandLine.getOptionValue(PORT, "8080"));
		startServer(port);
	}

	private static void startServer(int port) {
		Server server = new Server(port);

		server.setErrorHandler(createErrorHandler());

		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(LoggingWebmentionEndpointServlet.class, "/");
		server.setHandler(servletHandler);

		try {
			server.start();
			LOGGER.info("Listening on port {}.", port);
		} catch (Exception e) {
			LOGGER.error("Unexpected error.", e);
		}
	}

	@NotNull
	private static ErrorHandler createErrorHandler() {
		ErrorHandler errorHandler = new ErrorHandler();
		errorHandler.setShowServlet(false);
		errorHandler.setShowStacks(false);
		return errorHandler;
	}

	@SuppressWarnings("serial")
	public static class LoggingWebmentionEndpointServlet extends AbstractWebmentionEndpointServlet {

		@Override
		protected void handleWebmention(@NotNull Webmention webmention) {
			LOGGER.info("Received Webmention from source '{}' with target '{}'.",
				webmention.source(),
				webmention.target());
		}
	}

}
