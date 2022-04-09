package dev.rilling.webmention4j.client.link;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Set;

/**
 * Simplified implementation of a `Link`: https://datatracker.ietf.org/doc/html/rfc8288
 */
public record Link(@NotNull URI uri, @NotNull Set<String> rel) {

	@NotNull
	static Link create(@NotNull jakarta.ws.rs.core.Link link) {
		return new Link(link.getUri(), Set.copyOf(link.getRels()));
	}

}
