package dev.rilling.webmention4j.client;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.List;

class HeaderLinkParser implements LinkParser {

	public @NotNull List<Link> parse(@NotNull URI uri, @NotNull ClassicHttpResponse httpResponse) throws IOException {
		Response.ResponseBuilder responseBuilder = RuntimeDelegate.getInstance().createResponseBuilder();
		for (Header header : httpResponse.getHeaders()) {
			responseBuilder.header(header.getName(), header.getValue());
		}
		try (Response response = responseBuilder.location(uri).build()) {
			return List.copyOf(response.getLinks());
		} catch (Exception e) {
			throw new IOException("Could not parse links.", e);
		}
	}
}
