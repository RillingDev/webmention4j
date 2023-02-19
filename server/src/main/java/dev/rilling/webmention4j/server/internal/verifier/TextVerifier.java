package dev.rilling.webmention4j.server.internal.verifier;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

public class TextVerifier implements Verifier {

	@NotNull
	@Override
	public String getSupportedMimeType() {
		return ContentType.TEXT_PLAIN.getMimeType();
	}

	@Override
	public boolean isValid(@NotNull ClassicHttpResponse response, @NotNull URI target) throws IOException {
		String body;
		try {
			body = EntityUtils.toString(response.getEntity());
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
		// Spec: 'If the document is plain text, the receiver should look for the URL by searching for the string.'
		return body.contains(target.toString());
	}
}
