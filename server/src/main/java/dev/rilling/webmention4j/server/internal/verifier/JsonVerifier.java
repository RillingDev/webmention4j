package dev.rilling.webmention4j.server.internal.verifier;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.net.URI;

public class JsonVerifier implements Verifier {
	private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();


	@Override
	public String getSupportedMimeType() {
		return ContentType.APPLICATION_JSON.getMimeType();
	}

	@Override
	public boolean isValid(ClassicHttpResponse response, URI target) throws IOException {
		if (response.getEntity() == null) {
			return false;
		}
		String body;
		try {
			body = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
		return containsUri(body, target);
	}

	private boolean containsUri(String rootNode, URI target) throws IOException {
		/*
		 * Spec:
		 * 'In a JSON (RFC7159) document,
		 *  the receiver should look for properties whose values are an exact match for the URL.'
		 */
		try (JsonParser jp = JSON_FACTORY.createParser(rootNode)) {
			while (jp.nextToken() != null) {
				if (jp.currentToken() == JsonToken.VALUE_STRING && target.toString().equals(jp.getText())) {
					return true;
				}
			}
		}
		return false;
	}
}
