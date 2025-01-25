package dev.rilling.webmention4j.client.internal.link;

import java.net.URI;
import java.util.Set;

/**
 * Simplified implementation of a `Link`, supporting only the URI and the 'rel' attribute:
 * <a href="https://datatracker.ietf.org/doc/html/rfc8288">https://datatracker.ietf.org/doc/html/rfc8288</a>
 * <p>
 * The URL is always in the resolved, absolute form.
 */
public record Link(URI uri, Set<String> rel) {

}
