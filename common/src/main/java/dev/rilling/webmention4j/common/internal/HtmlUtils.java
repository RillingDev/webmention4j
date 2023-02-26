package dev.rilling.webmention4j.common.internal;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.MessageHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

import java.io.IOException;

public final class HtmlUtils {

	private HtmlUtils() {
	}

	/**
	 * @return If the response has a content-type specifying HTML.
	 */
	public static boolean isHtml(@NotNull MessageHeaders messageHeaders) {
		return HttpUtils.extractContentType(messageHeaders)
			.map(contentType -> contentType.isSameMimeType(ContentType.TEXT_HTML))
			.orElse(false);
	}

	/**
	 * @return The response as a HTML document.
	 * @see #isHtml(MessageHeaders)
	 */
	@NotNull
	public static Document parse(@NotNull HttpEntity entity) throws IOException {
		try {
			return Jsoup.parse(EntityUtils.toString(entity));
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
	}

	/**
	 * Filters for HTML elements that are link-like.
	 * This includes:
	 * <ul>
	 *     <li>anchor tags</li>
	 *     <li>media tags</li>
	 * </ul>
	 */
	public static class LinkLikeElementEvaluator extends Evaluator {

		/**
		 * Extracts the link from a link-like element.
		 *
		 * @param element Element to parse. Must be an element that {@link #matches(Element, Element)} was true for.
		 * @return Link of this element. May not be a valid URI!
		 */
		@NotNull
		public static String getLink(@NotNull Element element) {
			if ("a".equals(element.normalName())) {
				return element.attr("href");
			}
			return element.attr("src");
		}

		@Override
		public boolean matches(@NotNull Element root, @NotNull Element element) {
			return switch (element.normalName()) {
				case "a" -> element.hasAttr("href");
				case "img", "video", "audio" -> element.hasAttr("src");
				default -> false;
			};
		}
	}
}
