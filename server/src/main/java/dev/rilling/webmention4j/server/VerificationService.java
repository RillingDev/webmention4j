package dev.rilling.webmention4j.server;

import dev.rilling.webmention4j.common.HttpUtils;
import dev.rilling.webmention4j.server.verifier.Verifier;
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

class VerificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationService.class);

	public static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

	@NotNull
	private final List<Verifier> verifiers;

	VerificationService(@NotNull List<Verifier> verifiers) {
		this.verifiers = List.copyOf(verifiers);
	}

	/**
	 * Verifies if the source URI mentions the target URI.
	 *
	 * @param httpClient HTTP client.
	 *                   Must be configured to follow redirects.
	 *                   Should be configured to use a fitting UA string.
	 * @param source     Source URI to check.
	 * @param target     Target URI to look for.
	 * @return if the verification of the submission passes,
	 * @throws IOException           if I/O fails.
	 * @throws VerificationException if verification cannot be performed due to an unsupported content type.
	 */
	public boolean isSubmissionValid(@NotNull CloseableHttpClient httpClient, @NotNull URI source, @NotNull URI target)
		throws IOException, VerificationException {
		/*
		 * Spec:
		 * 'If the receiver is going to use the Webmention in some way, (displaying it as a comment on a post,
		 * incrementing a "like" counter, notifying the author of a post), then it MUST perform an HTTP GET request
		 * on source'
		 *
		 * 'The receiver SHOULD include an HTTP Accept header indicating its preference of content types that are acceptable.'
		 */

		ClassicHttpRequest request = ClassicRequestBuilder.get(source).addHeader(createAcceptHeader()).build();

		LOGGER.debug("Verifying source '{}'.", source);
		try (ClassicHttpResponse response = httpClient.execute(request)) {
			if (!HttpUtils.isSuccessful(response.getCode())) {
				throw new IOException("Request failed: %d - '%s'.".formatted(response.getCode(),
					response.getReasonPhrase()));
			}
			return isSubmissionResponseValid(response, source, target);
		}
	}

	private boolean isSubmissionResponseValid(ClassicHttpResponse response, @NotNull URI source, @NotNull URI target)
		throws IOException, VerificationException {
		Optional<Verifier> verifierOptional = HttpUtils.extractContentType(response)
			.flatMap(this::findMatchingVerifier);
		if (verifierOptional.isPresent()) {
			Verifier verifier = verifierOptional.get();
			LOGGER.debug("Found verifier '{}' for source '{}'.", verifier, source);
			return verifier.isValid(response, target);
		} else {
			LOGGER.debug("No verifier supports response content type, rejecting it. {}", response);
			throw new VerificationException("Content type of source is not supported.");
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

	static class VerificationException extends Exception {
		@Serial
		private static final long serialVersionUID = 7007956002984142094L;

		VerificationException(String message) {
			super(message);
		}
	}
}
