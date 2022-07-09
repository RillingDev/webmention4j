package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.server.AbstractWebmentionEndpointServlet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static dev.rilling.webmention4j.example.CliUtils.parseArgs;
import static dev.rilling.webmention4j.example.CliUtils.printHelp;

public final class WebmentionEndpointServletExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServletExample.class);

	private static final int DEFAULT_PORT = 8080;
	private static final String DEFAULT_ADDRESS = "0.0.0.0";

	private static final Option HELP = Option.builder()
		.option("h")
		.longOpt("help")
		.hasArg(false)
		.desc("Shows this help text.")
		.build();

	private static final Option ADDRESS = Option.builder()
		.option("a")
		.longOpt("address")
		.hasArg(true)
		.desc("Address to listen on. Defaults to '%s'.".formatted(DEFAULT_ADDRESS))
		.build();

	private static final Option PORT = Option.builder()
		.option("p")
		.longOpt("port")
		.hasArg(true)
		.desc("Port to listen on. Defaults to '%s'.".formatted(DEFAULT_PORT))
		.build();

	private static final Options OPTIONS = new Options().addOption(HELP).addOption(ADDRESS).addOption(PORT);

	private WebmentionEndpointServletExample() {
	}

	/**
	 * Starts Webmention endpoint server.
	 * <p>
	 * Call with `--help` for usage information.
	 * <p>
	 * Security:
	 * This server is not designed to be exposed directly to the internet.
	 * Instead, a reverse proxy should be used that directs only HTTP traffic for the POST method to it.
	 */
	public static void main(String[] args) {
		CommandLine commandLine = parseArgs(args, OPTIONS);
		if (commandLine.hasOption(HELP)) {
			printHelp(OPTIONS);
			return;
		}

		String address = commandLine.getOptionValue(ADDRESS, DEFAULT_ADDRESS);
		int port = Integer.parseInt(commandLine.getOptionValue(PORT, String.valueOf(DEFAULT_PORT)));

		WebmentionEndpointServletExample servletExample = new WebmentionEndpointServletExample();
		servletExample.startServer(new InetSocketAddress(address, port));
	}

	private void startServer(InetSocketAddress socketAddress) {
		Server server = new Server(socketAddress);

		server.setErrorHandler(createErrorHandler());

		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(LoggingWebmentionEndpointServlet.class, "/");
		server.setHandler(servletHandler);

		try {
			server.start();
			LOGGER.info("Listening on '{}'.", socketAddress);
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
