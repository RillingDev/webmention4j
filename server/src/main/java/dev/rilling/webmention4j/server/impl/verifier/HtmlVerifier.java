package dev.rilling.webmention4j.server.impl.verifier;

import dev.rilling.webmention4j.common.util.HtmlUtils;
import dev.rilling.webmention4j.common.util.HtmlUtils.LinkLikeElementEvaluator;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

public class HtmlVerifier implements Verifier {

	@NotNull
	@Override
	public String getSupportedMimeType() {
		return ContentType.TEXT_HTML.getMimeType();
	}

	@Override
	public boolean isValid(@NotNull ClassicHttpResponse httpResponse, @NotNull URI target) throws IOException {
		return HtmlUtils.parse(httpResponse)
			/*
			 * Spec:
			 * '[...] in an HTML5 document, the receiver should look for <a href="*">, <img href="*">,
			 *  <video src="*"> and other similar links.'
			 */.select(new LinkLikeElementEvaluator()).stream().map(LinkLikeElementEvaluator::getLink)
			// Note: The spec does state 'exact match', so strict equality is used rather than resolving the URLs.
			// TODO: maybe comparing resolved/parsed URIs would make more sense
			.anyMatch(link -> target.toString().equals(link));
	}

}
