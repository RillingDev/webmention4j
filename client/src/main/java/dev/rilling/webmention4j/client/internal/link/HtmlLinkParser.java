package dev.rilling.webmention4j.client.internal.link;

import dev.rilling.webmention4j.common.internal.HtmlUtils;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * {@link LinkParser} checking HTML body for {@link Link}s.
 * If response is not HTML, an empty collection is returned.
 */
public final class HtmlLinkParser implements LinkParser {

	private static final LinkElementEvaluator LINK_ELEMENT_EVALUATOR = new LinkElementEvaluator();

	public @NotNull List<Link> parse(@NotNull URI location, @NotNull ClassicHttpResponse response)
		throws IOException {
		if (!HtmlUtils.isHtml(response) || response.getEntity() == null) {
			return List.of();
		}

		Document document = HtmlUtils.parse(response.getEntity());
		Elements linkElements = document.select(LINK_ELEMENT_EVALUATOR);

		try {
			return linkElements.stream()
				.map(element -> RuntimeDelegate.getInstance()
					.createLinkBuilder()
					.baseUri(location)
					.uri(element.attr("href"))
					.rel(element.attr("rel"))
					.build())
				.map(link -> new Link(link.getUri(), Set.copyOf(link.getRels())))
				.toList();
		} catch (Exception e) {
			throw new IOException("Could not parse link(s) in HTML.", e);
		}
	}

	private static class LinkElementEvaluator extends Evaluator {

		private static final Set<String> LINK_ELEMENT_NAMES = Set.of("link", "a");

		@Override
		public boolean matches(@NotNull Element root, @NotNull Element element) {
			return LINK_ELEMENT_NAMES.contains(element.normalName()) && element.hasAttr("href") &&
				element.hasAttr("rel");
		}
	}
}
