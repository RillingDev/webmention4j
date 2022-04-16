package dev.rilling.webmention4j.server.verifier;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.io.IOException;
import java.net.URI;

public class HtmlVerifier implements Verifier {

	@NotNull
	@Override
	public String getSupportedMimeType() {
		return "text/html";
	}

	@Override
	public boolean isValid(@NotNull ClassicHttpResponse httpResponse, @NotNull URI target) throws IOException {
		String body;
		try {
			body = EntityUtils.toString(httpResponse.getEntity());
		} catch (ParseException | IOException e) {
			throw new IOException("Could not parse body.", e);
		}
		return Jsoup.parse(body, target.toString()).selectFirst(new HtmlLinkEvaluator(target)) != null;
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
