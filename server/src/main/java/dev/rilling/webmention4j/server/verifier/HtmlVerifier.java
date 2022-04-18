package dev.rilling.webmention4j.server.verifier;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
		Document document;
		try {
			String body = EntityUtils.toString(httpResponse.getEntity());
			document = Jsoup.parse(body, target.toString());
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
		return document.selectFirst(new HtmlLinkEvaluator(target)) != null;
	}

	private static class HtmlLinkEvaluator extends Evaluator {
		@NotNull
		private final String sourceUriString;

		HtmlLinkEvaluator(@NotNull URI source) {
			sourceUriString = source.toString();
		}

		@Override
		public boolean matches(@NotNull Element root, @NotNull Element element) {
			return switch (element.normalName()) {
				case "a" -> sourceUriString.equals(element.attr("href"));
				case "img", "video", "audio" -> sourceUriString.equals(element.attr("src"));
				default -> false;
			};
		}
	}
}
