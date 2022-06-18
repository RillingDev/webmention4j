package dev.rilling.webmention4j.server.impl;

import dev.rilling.webmention4j.common.util.HttpUtils;
import dev.rilling.webmention4j.server.impl.verifier.Verifier;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicHeader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VerificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationService.class);

	private static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

	@NotNull
	private final List<Verifier> verifiers;

	public VerificationService(@NotNull List<Verifier> verifiers) {
		this.verifiers = List.copyOf(verifiers);
	}

	/**
	 * Checks if the URLs scheme allows for validation.
	 */
	public boolean isUriSchemeSupported(@NotNull URI uri) {
		return uri.getScheme() != null && SUPPORTED_SCHEMES.contains(uri.getScheme());
	}

	/**
	 * Verifies if the source URL mentions the target URL.
	 *
	 * @param httpClient HTTP client.
	 *                   Must be configured to follow redirects.
	 *                   Should be configured to use a fitting UA string.
	 * @param source     Source URL to check.
	 * @param target     Target URL to look for.
	 * @return if the verification of the Webmention passes,
	 * @throws IOException                     if I/O fails.
	 * @throws UnsupportedContentTypeException if verification cannot be performed due to an unsupported content type.
	 */
	//Spec: https://www.w3.org/TR/webmention/#webmention-verification
	public boolean isWebmentionValid(@NotNull CloseableHttpClient httpClient, @NotNull URI source, @NotNull URI target)
		throws IOException, UnsupportedContentTypeException {
		/*
		 * Spec:
		 * 'MUST perform an HTTP GET request on source [...].
		 * The receiver SHOULD include an HTTP Accept header indicating its preference of content
		 * types that are acceptable.'
		 */
		ClassicHttpRequest request = ClassicRequestBuilder.get(source).addHeader(createAcceptHeader()).build();

		LOGGER.debug("Verifying source '{}'.", source);
		try (ClassicHttpResponse response = httpClient.execute(request)) {
			if (response.getCode() == HttpStatus.SC_NOT_ACCEPTABLE) {
				throw new UnsupportedContentTypeException(
					"Remote server does not support any of the content types supported for verification.");
			}
			HttpUtils.validateResponse(response);
			return isResponseValid(response, source, target);
		}
	}

	private boolean isResponseValid(ClassicHttpResponse response, @NotNull URI source, @NotNull URI target)
		throws IOException, UnsupportedContentTypeException {
		/*
		 * Spec:
		 * 'The receiver SHOULD use per-media-type rules to determine whether the source document mentions the target URL.
		 * [...]
		 *  The source document MUST have an exact match of the target URL provided in order
		 *  for it to be considered a valid Webmention.'
		 */
		Optional<Verifier> verifierOptional = HttpUtils.extractContentType(response).flatMap(this::findMatchingVerifier);
		if (verifierOptional.isPresent()) {
			Verifier verifier = verifierOptional.get();
			LOGGER.debug("Found verifier '{}' for source '{}'.", verifier, source);
			return verifier.isValid(response, target);
		} else {
			throw new UnsupportedContentTypeException("Content type of remote server response is not supported.");
		}
	}

	private @NotNull Header createAcceptHeader() {
		String acceptValue = verifiers.stream().map(Verifier::getSupportedMimeType).collect(Collectors.joining(", "));
		return new BasicHeader(HttpHeaders.ACCEPT, acceptValue);
	}

	private @NotNull Optional<Verifier> findMatchingVerifier(@NotNull ContentType contentType) {
		return verifiers.stream()
			.filter(verifier -> verifier.getSupportedMimeType().equals(contentType.getMimeType()))
			.findFirst();
	}

	public static class UnsupportedContentTypeException extends Exception {
		@Serial
		private static final long serialVersionUID = 7007956002984142094L;

		UnsupportedContentTypeException(String message) {
			super(message);
		}
	}
}
