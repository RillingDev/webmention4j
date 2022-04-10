import dev.rilling.webmention4j.server.WebmentionEndpointServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.http.ContentType;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;

public final class WebmentionEndpointServletExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServletExample.class);

	private static final int PORT = 8080;

	private WebmentionEndpointServletExample() {
	}

	public static void main(String[] args) {
		Server server = new Server(PORT);
		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(ExampleBlogPostServlet.class, "/blogpost");
		servletHandler.addServletWithMapping(WebmentionEndpointServlet.class, "/endpoint");
		server.setHandler(servletHandler);

		try {
			server.start();
			LOGGER.info("Listening on port {}.", PORT);
		} catch (Exception e) {
			LOGGER.error("Unexpected error.", e);
		}
	}


	public static class ExampleBlogPostServlet extends HttpServlet {
		@Serial
		private static final long serialVersionUID = 4724567389140030892L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
			resp.setContentType(ContentType.TEXT_PLAIN.toString());
			resp.setHeader("Link", "<../endpoint>; rel=\"webmention\"");
			resp.getWriter().print("Hello World!");
		}
	}
}
