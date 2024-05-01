package dev.rilling.webmention4j.client.internal.link;

import jakarta.ws.rs.ext.RuntimeDelegate;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Set;

final class LinkUtils {
	private LinkUtils() {
	}

	public static @NotNull Link fromHeaderValue(
		@NotNull URI baseUri, @NotNull String headerValue) {
		jakarta.ws.rs.core.Link link = RuntimeDelegate.getInstance()
			.createLinkBuilder()
			.baseUri(baseUri)
			.link(headerValue).build();
		return new Link(link.getUri(), Set.copyOf(link.getRels()));

	}

	public static @NotNull Link fromElement(@NotNull URI baseUri, @NotNull String href, @NotNull String rel) {
		jakarta.ws.rs.core.Link link = RuntimeDelegate.getInstance()
			.createLinkBuilder()
			.baseUri(baseUri)
			.uri(href)
			.rel(rel)
			.build();
		return new Link(link.getUri(), Set.copyOf(link.getRels()));
	}
}
