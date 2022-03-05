package dev.rilling.webmention4j.client;

import jakarta.ws.rs.core.Link;
import org.apache.hc.core5.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

interface LinkParser {
	@NotNull Set<Link> parse(@NotNull HttpResponse httpResponse) throws IOException;
}
