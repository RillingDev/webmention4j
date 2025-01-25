package dev.rilling.webmention4j.client.internal.link;

import org.apache.hc.client5.http.utils.URIUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

final class LinkUtils {
	private static final Pattern REL_SEPARATOR = Pattern.compile(" +");

	private LinkUtils() {
	}

	/**
	 * Parses a {@link Link} from a HTML element.
	 *
	 * @param baseUri Base URI of the document
	 * @param href    HREF of the element.
	 * @param rel     Rel of the element.
	 * @return The parsed link.
	 * @throws LinkException if parsing fails
	 */
	// https://datatracker.ietf.org/doc/html/rfc8288#appendix-A.1
	public static @NonNull Link fromElement(@NonNull URI baseUri, @NonNull String href, @Nullable String rel) {
		return createLink(baseUri, href, rel);
	}

	/**
	 * Parses a {@link Link} from a HTTP header.
	 *
	 * @param baseUri     Base URI of the request
	 * @param headerValue The header value.
	 * @return The parsed link.
	 * @throws LinkException if parsing fails
	 */
	// https://datatracker.ietf.org/doc/html/rfc8288#section-3
	public static @NonNull Link fromHeaderValue(
		@NonNull URI baseUri, @NonNull String headerValue) {
		String normalizedHeaderValue = headerValue.trim();

		if (!normalizedHeaderValue.startsWith("<")) {
			throw new LinkException("Missing starting token < in '%s'.".formatted(normalizedHeaderValue));
		}

		final int gtIndex = normalizedHeaderValue.indexOf('>');
		if (gtIndex == -1) {
			throw new LinkException("Missing token > in '%s'.".formatted(normalizedHeaderValue));
		}

		String uri = normalizedHeaderValue.substring(1, gtIndex).trim();
		String params = normalizedHeaderValue.substring(gtIndex + 1).trim();
		String rel = findRelParam(params);
		return createLink(baseUri, uri, rel);
	}

	private static @Nullable String findRelParam(@NonNull String params) {
		final StringTokenizer st = new StringTokenizer(params, ";=\"", true);
		while (st.hasMoreTokens()) {
			checkToken(st, ";");
			final String name = st.nextToken().trim();
			checkToken(st, "=");

			String value = nextNonEmptyToken(st);
			if ("\"".equals(value)) {
				value = st.nextToken();
				checkToken(st, "\"");
			}

			if ("rel".equals(name)) {
				return value;
			}
		}
		return null;
	}

	private static void checkToken(@NonNull StringTokenizer st, @NonNull String expected) {
		String token;
		do {
			token = st.nextToken().trim();
		} while (token.isEmpty());

		if (!expected.equals(token)) {
			throw new LinkException("Expected token '%s' but found '%s'".formatted(expected, token));
		}
	}

	private static @NonNull String nextNonEmptyToken(@NonNull StringTokenizer st) {
		String token;
		do {
			token = st.nextToken().trim();
		} while (token.isEmpty());

		return token;
	}


	static @NonNull Link createLink(@NonNull URI baseUri, @NonNull String uri, @Nullable String rel) {
		Set<String> rels = rel != null ? Set.of(REL_SEPARATOR.split(rel)) : Set.of();

		URI linkUri;
		try {
			linkUri = resolveLinkUri(baseUri, uri);
		} catch (URISyntaxException e) {
			throw new LinkException("Failed to build URI.", e);
		}

		return new Link(linkUri, rels);
	}

	private static URI resolveLinkUri(@NonNull URI baseUri, @NonNull String uri) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder(uri);
		if (uriBuilder.isAbsolute()) {
			return uriBuilder.optimize().build();
		} else {
			return new URIBuilder(URIUtils.resolve(baseUri, uriBuilder.build())).optimize().build();
		}
	}

	/**
	 * Thrown if link parsing fails.
	 */
	public static class LinkException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 4229447237422518713L;

		public LinkException(String message) {
			super(message);
		}

		public LinkException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
