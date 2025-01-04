package dev.rilling.webmention4j.server;

import jakarta.servlet.Servlet;
import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;

class ServletExtension implements BeforeAllCallback, AfterAllCallback {
	private final String specPath;
	private final Class<? extends Servlet> servlet;

	private Server server;
	private URI servletUri;

	ServletExtension(@NotNull String specPath, @NotNull Class<? extends Servlet> servlet) {
		this.specPath = specPath;
		this.servlet = servlet;
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		server = new Server(0);
		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.addServlet(servlet, specPath);
		server.setHandler(servletContextHandler);

		server.start();

		int port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
		servletUri = URIBuilder.localhost().setScheme("http").setPort(port).setPath(specPath).build();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		server.stop();
	}

	public URI getServletUri() {
		return servletUri;
	}
}
