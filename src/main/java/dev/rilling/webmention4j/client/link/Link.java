package dev.rilling.webmention4j.client.link;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Naive implementation of a `Link`: https://datatracker.ietf.org/doc/html/rfc8288
 */
public record Link(@NotNull URI uri, @NotNull Set<String> rel) {

	@NotNull
	static Link create(@NotNull jakarta.ws.rs.core.Link link) {
		return new Link(link.getUri(), parseDelimitedRel(link.getRel()));
	}

	private static @NotNull Set<String> parseDelimitedRel(@Nullable String relValue) {
		if (relValue == null) {
			return Collections.emptySet();
		}
		return Set.copyOf(Arrays.asList(relValue.split(" ")));
	}
}
