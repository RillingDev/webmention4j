package dev.rilling.webmention4j.server.impl.verifier;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

public class JsonVerifier implements Verifier {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@NotNull
	@Override
	public String getSupportedMimeType() {
		return ContentType.APPLICATION_JSON.getMimeType();
	}

	@Override
	public boolean isValid(@NotNull ClassicHttpResponse httpResponse, @NotNull URI target) throws IOException {
		String body;
		try {
			body = EntityUtils.toString(httpResponse.getEntity());
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
		return containsUri(body, target);
	}

	private boolean containsUri(String rootNode, @NotNull URI target) throws IOException {
		/*
		 * Spec:
		 * 'In a JSON (RFC7159) document,
		 *  the receiver should look for properties whose values are an exact match for the URL.'
		 */
		try (JsonParser jp = OBJECT_MAPPER.createParser(rootNode)) {
			while (jp.nextToken() != null) {
				if (jp.currentToken() == JsonToken.VALUE_STRING && target.toString().equals(jp.getText())) {
					return true;
				}
			}
		}
		return false;
	}
}
