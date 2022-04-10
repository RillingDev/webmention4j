package dev.rilling.webmention4j.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;

public final class WebmentionEndpointServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServlet.class);

	@Serial
	private static final long serialVersionUID = 3071031317934821620L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (!ContentType.APPLICATION_FORM_URLENCODED.isSameMimeType(ContentType.parse(req.getContentType()))) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid content type.");
			return;
		}
		String source = req.getParameter("source");
		String target = req.getParameter("target");
		if (source == null || target == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expecting both the parameter 'source' and 'target'.");
		}
		LOGGER.info("Found webmention request with source='{}' and target='{}'.", source, target);
	}
}
