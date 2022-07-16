package dev.rilling.webmention4j.server.impl.verifier;

import dev.rilling.webmention4j.common.util.HtmlUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

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
		return HtmlUtils.parse(httpResponse).selectFirst(new HtmlLinkEvaluator(target)) != null;
	}

	private static class HtmlLinkEvaluator extends Evaluator {
		@NotNull
		private final String targetUriString;

		HtmlLinkEvaluator(@NotNull URI target) {
			targetUriString = target.toString();
		}

		@Override
		public boolean matches(@NotNull Element root, @NotNull Element element) {
			/*
			 * Spec:
			 * '[...] in an HTML5 document, the receiver should look for <a href="*">, <img href="*">,
			 *  <video src="*"> and other similar links.'
			 */
			// Note: The spec does state 'exact match', so strict equality is used rather than resolving the URLs.
			return switch (element.normalName()) {
				case "a" -> targetUriString.equals(element.attr("href"));
				case "img", "video", "audio" -> targetUriString.equals(element.attr("src"));
				default -> false;
			};
		}
	}
}
