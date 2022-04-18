package dev.rilling.webmention4j.server;

import dev.rilling.webmention4j.common.HttpUtils;
import dev.rilling.webmention4j.server.verifier.HtmlVerifier;
import dev.rilling.webmention4j.server.verifier.JsonVerifier;
import dev.rilling.webmention4j.server.verifier.TextVerifier;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public final class WebmentionEndpointServlet extends HttpServlet {

	@Serial
	private static final long serialVersionUID = 3071031317934821620L;

	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionEndpointServlet.class);

	// Static due to not being serializable
	private static final VerificationService verificationService = new VerificationService(List.of(new HtmlVerifier(),
		new TextVerifier(),
		new JsonVerifier()));

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

		/*
		 * Spec:
		 * 'If the receiver chooses to process the request and perform the verification step synchronously (not
		 * recommended), it MUST respond with a 200 OK status on success.'
		 * */
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	@VisibleForTesting
	void processRequest(@NotNull HttpServletRequest req) throws BadRequestException {
		if (!EXPECTED_CONTENT_TYPE.isSameMimeType(ContentType.parse(req.getContentType()))) {
			throw new BadRequestException("Content type must be '%s'.".formatted(EXPECTED_CONTENT_TYPE.getMimeType()));
		}
		URI source = extractParameterAsUri(req, "source");
		URI target = extractParameterAsUri(req, "target");

		// Spec: 'The receiver MUST reject the request if the source URL is the same as the target URL.'
		if (source.equals(target)) {
			throw new BadRequestException("Source and target URL may not be identical.");
		}

		// TODO: allow configuration of allowed target URI hosts.

		LOGGER.debug("Received webmention request with source='{}' and target='{}'.", source, target);

		// TODO: perform check async
		// TODO: re-use client
		try (CloseableHttpClient httpClient = createDefaultHttpClient()) {
			verificationService.isSubmissionValid(httpClient, source, target);
		} catch (IOException | VerificationService.VerificationException e) {
			throw new BadRequestException("Verification of source URI failed.", e);
		}
	}

	private @NotNull URI extractParameterAsUri(@NotNull HttpServletRequest req, @NotNull String parameterName) throws BadRequestException {
		/*
		 * Spec:
		 * 'The receiver MUST check that source and target are valid URLs
		 * and are of schemes that are supported by the receiver.'
		 */
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
		if (!verificationService.isUriSchemeSupported(uri)) {
			throw new BadRequestException("Unsupported URL scheme.");
		}
		return uri;
	}

	@NotNull
	private static CloseableHttpClient createDefaultHttpClient() {
		return HttpClients.custom()
			.setUserAgent(HttpUtils.createUserAgentString("webmention4j-server",
				WebmentionEndpointServlet.class.getPackage()))
			.build();
	}

	private static final class BadRequestException extends Exception {

		@Serial
		private static final long serialVersionUID = -8108083179786850494L;

		/**
		 * @param message User-facing error message.
		 */
		BadRequestException(String message) {
			super(message);
		}

		/**
		 * @param message User-facing error message.
		 * @param cause   Exception cause.
		 */
		BadRequestException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
