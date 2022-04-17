package dev.rilling.webmention4j.server;

import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebmentionEndpointServletExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServletExample.class);

	private static final int PORT = 8080;

	private WebmentionEndpointServletExample() {
	}

	public static void main(String[] args) {
		Server server = new Server(PORT);
		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(WebmentionEndpointServlet.class, "/endpoint");
		server.setHandler(servletHandler);

		try {
			server.start();
			LOGGER.info("Listening on port {}.", PORT);
		} catch (Exception e) {
			LOGGER.error("Unexpected error.", e);
		}
	}
}
