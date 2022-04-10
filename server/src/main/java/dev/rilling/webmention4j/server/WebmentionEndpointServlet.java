package dev.rilling.webmention4j.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public final class WebmentionEndpointServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServlet.class);

	@Serial
	private static final long serialVersionUID = 3071031317934821620L;

	private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");
	private static final ContentType EXPECTED_CONTENT_TYPE = ContentType.APPLICATION_FORM_URLENCODED;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			processRequest(req);
		} catch (BadRequestException e) {
			LOGGER.warn("Bad request.", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}

		resp.setStatus(HttpServletResponse.SC_OK);
	}

	private void processRequest(@NotNull HttpServletRequest req) throws BadRequestException {
		if (!EXPECTED_CONTENT_TYPE.isSameMimeType(ContentType.parse(req.getContentType()))) {
			throw new BadRequestException("Content type must be '%s'.".formatted(EXPECTED_CONTENT_TYPE));
		}
		URI source = extractParameterAsUri(req, "source");
		URI target = extractParameterAsUri(req, "target");

		LOGGER.info("Received webmention request with source='{}' and target='{}'.", source, target);
	}

	private @NotNull URI extractParameterAsUri(@NotNull HttpServletRequest req, @NotNull String parameterName)
		throws BadRequestException {
		String parameter = req.getParameter(parameterName);
		if (parameter == null) {
			throw new BadRequestException("Required parameter '%s' is missing.".formatted(parameterName));
		}

		URI uri;
		try {
			uri = new URI(parameter);
		} catch (URISyntaxException e) {
			throw new BadRequestException("Invalid URL syntax.", e);
		}
		if (!SUPPORTED_SCHEMES.contains(uri.getScheme())) {
			throw new BadRequestException("Unsupported URL scheme '%s'.".formatted(uri.getScheme()));
		}
		return uri;
	}

}
