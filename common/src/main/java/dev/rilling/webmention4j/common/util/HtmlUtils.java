package dev.rilling.webmention4j.common.util;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public final class HtmlUtils {
	private HtmlUtils() {
	}


	/**
	 * @return If the response has a content-type specifying HTML.
	 */
	public static boolean isHtml(@NotNull HttpResponse httpResponse) {
		return HttpUtils.extractContentType(httpResponse)
			.map(contentType -> contentType.isSameMimeType(ContentType.TEXT_HTML))
			.orElse(false);
	}

	/**
	 * @return The response as a HTML document.
	 * @see #isHtml(HttpResponse)
	 */
	public static Document parse(@NotNull ClassicHttpResponse httpResponse) throws IOException {
		try {
			String body = EntityUtils.toString(httpResponse.getEntity());
			return Jsoup.parse(body);
		} catch (ParseException e) {
			throw new IOException("Could not parse body.", e);
		}
	}
}
