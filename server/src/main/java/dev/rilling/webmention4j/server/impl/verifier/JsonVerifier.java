package dev.rilling.webmention4j.server.impl.verifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

public class JsonVerifier implements Verifier {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@NotNull
	@Override
	public String getSupportedMimeType() {
		return ContentType.APPLICATION_JSON.getMimeType();
	}

	@Override
	public boolean isValid(@NotNull ClassicHttpResponse httpResponse, @NotNull URI target) throws IOException {
		JsonNode rootNode;
		try {
			String body = EntityUtils.toString(httpResponse.getEntity());
			rootNode = OBJECT_MAPPER.readTree(body);
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
		if (rootNode == null) {
			throw new IOException("No JSON content found.");
		}

		return containsUri(rootNode, target);
	}

	private boolean containsUri(@NotNull JsonNode rootNode, @NotNull URI target) {
		Iterator<JsonNode> elements = rootNode.elements();
		while (elements.hasNext()) {
			JsonNode node = elements.next();
			if (node.isTextual() && node.textValue().equals(target.toString())) {
				return true;
			}
			if (node.isObject()) {
				// TODO: limit depth
				return containsUri(node, target);
			}
		}
		return false;
	}
}
