package dev.rilling.webmention4j.client.internal.link;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static dev.rilling.webmention4j.client.internal.link.LinkUtils.fromElement;
import static org.assertj.core.api.Assertions.assertThat;

// This test suite is based on org.apache.cxf.jaxrs.impl.LinkBuilderImplTest from https://cxf.apache.org/
class LinkUtilsTest {

	@Test
	@DisplayName("#fromElement parses self link")
	void fromElementSelfLink() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com"), "https://example.com/page1", "self"), "https://example.com/page1", Set.of("self"));
	}

	@Test
	@DisplayName("#fromElement parses many rel")
	void fromElementManyRel() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com"), "https://example.com/page1", "1 2"), "https://example.com/page1", Set.of("1", "2"));
	}

	@Test
	@DisplayName("#fromElement parses relative link")
	void fromElementRelativeLink() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com/base/path"), "relative", "next"), "https://example.com/base/relative", Set.of("next"));
	}

	@Test
	@DisplayName("#fromElement parses relative link 2")
	void fromElementRelativeLink2() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com/base/path"), "/relative", "next"), "https://example.com/relative", Set.of("next"));
	}

	private static void assertThatIsEqualToLink(Link actual, String url, Set<String> rel) {
		assertThat(actual).isEqualTo(new Link(URI.create(url), rel));
	}
}
