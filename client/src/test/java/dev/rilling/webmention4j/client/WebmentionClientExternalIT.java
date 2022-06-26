package dev.rilling.webmention4j.client;

import dev.rilling.webmention4j.common.Webmention;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verify implementation against <a href="https://webmention.rocks/">webmention.rocks</a>.
 * <p>
 * This requires you to have a public webpage in {@link #DUMMY_BLOG_POST} that references the pages in {@link #createEndpoints()}
 * (see the webmention.rocks page above).
 */
@Tag("manual")
@Disabled("Designed for manual execution.")
class WebmentionClientExternalIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionClientExternalIT.class);

	private static final URI DUMMY_BLOG_POST = URI.create("https://example.com/"); // <-- Enter your URL instead!


	final WebmentionClient client = new WebmentionClient();

	@ParameterizedTest
	@MethodSource(value = "createEndpoints")
	void supportsWebmention(URI target) throws Exception {
		assertThat(client.supportsWebmention(target)).isTrue();
	}

	@ParameterizedTest
	@MethodSource(value = "createEndpoints")
	void sendWebmention(URI target) {
		final Webmention webmention = new Webmention(DUMMY_BLOG_POST, target);
		assertDoesNotThrow(() -> client.sendWebmention(webmention));
	}

	static Stream<URI> createEndpoints() {
		return Stream.of("https://webmention.rocks/test/1",
			"https://webmention.rocks/test/2",
			"https://webmention.rocks/test/3",
			"https://webmention.rocks/test/4",
			"https://webmention.rocks/test/5",
			"https://webmention.rocks/test/6",
			"https://webmention.rocks/test/7",
			"https://webmention.rocks/test/8",
			"https://webmention.rocks/test/9",
			"https://webmention.rocks/test/10",
			"https://webmention.rocks/test/11",
			"https://webmention.rocks/test/12",
			"https://webmention.rocks/test/13",
			"https://webmention.rocks/test/14",
			"https://webmention.rocks/test/15",
			"https://webmention.rocks/test/16",
			"https://webmention.rocks/test/17",
			"https://webmention.rocks/test/18",
			// TODO: broken due to Link parser issue, see https://github.com/eclipse-ee4j/jersey/issues/3769
			"https://webmention.rocks/test/19",
			"https://webmention.rocks/test/20",
			"https://webmention.rocks/test/21",
			"https://webmention.rocks/test/22",
			// TODO: broken due to how webmention.rocks testcase is structured
			"https://webmention.rocks/test/23/page").map(URI::create);
	}
}
