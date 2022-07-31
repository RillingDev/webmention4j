package dev.rilling.webmention4j.common;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

/**
 * A Webmention instance.
 * <p>
 * From the <a href="https://www.w3.org/TR/webmention/#introduction">specification</a>:
 * <br>
 * "A Webmention is a notification that one URL links to another.
 * For example, Alice writes an interesting post on her blog.
 * Bob then writes a response to her post on his own site, linking back to Alice's original post.
 * Bob's publishing software sends a Webmention to Alice notifying that her article was replied to,
 * and Alice's software can show that reply as a comment on the original post."
 *
 * @param source Source page that is mentioning the target.
 *               Usually this should be HTTP or HTTPS.
 * @param target Page being mentioned.
 *               Usually this should be HTTP or HTTPS.
 *               May not be the same as source.
 */
public record Webmention(@NotNull URI source, @NotNull URI target) {
	public Webmention(@NotNull URI source, @NotNull URI target) {
		if (source.equals(target)) {
			throw new IllegalArgumentException("Source and target URL must not be identical.");
		}
		this.source = source;
		this.target = target;
	}
}
