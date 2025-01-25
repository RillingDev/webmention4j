package dev.rilling.webmention4j.client.internal.link;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.Set;

import static dev.rilling.webmention4j.client.internal.link.LinkUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 This test suite is based on both
 org.apache.cxf.jaxrs.impl.LinkBuilderImplTest from https://cxf.apache.org/
 as well as
 org.glassfish.jersey.tests.e2e.common.message.internal.LinkProviderTest from https://github.com/eclipse-ee4j/jersey
*/
class LinkUtilsTest {

	// org.apache.cxf.jaxrs.impl.LinkBuilderImplTest#testSelfLink
	@Test
	@DisplayName("#fromElement parses self link")
	void fromElementParsesSelfLink() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com"), "https://example.com/page1", "self"), "https://example.com/page1", Set.of("self"));
	}

	// org.apache.cxf.jaxrs.impl.LinkBuilderImplTest#testBuildManyRels
	@Test
	@DisplayName("#fromElement parses many rel")
	void fromElementParsesManyRel() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com"), "https://example.com/page1", "1 2"), "https://example.com/page1", Set.of("1", "2"));
	}

	// org.apache.cxf.jaxrs.impl.LinkBuilderImplTest#testRelativeLink
	@Test
	@DisplayName("#fromElement parses relative link")
	void fromElementParsesRelativeLink() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com/base/path"), "relative", "next"), "https://example.com/base/relative", Set.of("next"));
	}

	// org.apache.cxf.jaxrs.impl.LinkBuilderImplTest#testRelativeLink2
	@Test
	@DisplayName("#fromElement parses relative link 2")
	void fromElementParsesRelativeLink2() {
		assertThatIsEqualToLink(fromElement(URI.create("https://example.com/base/path"), "/relative", "next"), "https://example.com/relative", Set.of("next"));
	}


	// org.glassfish.jersey.tests.e2e.common.message.internal.LinkProviderTest#testValueOf
	@ParameterizedTest
	@CsvSource(value = {"<https://example.com/app/link1>", " <https://example.com/app/link1>", " <https://example.com/app/link1> "})
	@DisplayName("#fromHeaderValue parses")
	void fromHeaderValueParses(String headerValue) {
		assertThatIsEqualToLink(fromHeaderValue(URI.create("https://example.com"), headerValue), "https://example.com/app/link1", Set.of());
	}

	// org.glassfish.jersey.tests.e2e.common.message.internal.LinkProviderTest#testValueOfExceptions
	@ParameterizedTest
	@CsvSource(value = {"https://example.com/app/link1>", "<https://example.com/app/link1", "https://example.com/app/link1"})
	@DisplayName("#fromHeaderValue rejects malformed")
	void fromHeaderValueRejectsMalformed(String headerValue) {
		assertThatThrownBy(() -> fromHeaderValue(URI.create("https://example.com"), headerValue)).isInstanceOf(LinkException.class);
	}

	// org.glassfish.jersey.tests.e2e.common.message.internal.LinkProviderTest#testValueOfParams
	@ParameterizedTest
	@CsvSource(value = {"<https://example.com/app/link1>; rel=\"self\"; other = \"bar\"", "<https://example.com/app/link1>; other=\"bar\"; rel= \"self\"", "<https://example.com/app/link1>;      rel=\"self\";     other=\"bar\"", "< https://example.com/app/link1   >;      rel=\"self\";     other=\"bar\""})
	@DisplayName("#fromHeaderValue parses parameters")
	void fromHeaderValueParsesParameters(String headerValue) {
		assertThatIsEqualToLink(fromHeaderValue(URI.create("https://example.com"), headerValue), "https://example.com/app/link1", Set.of("self"));
	}

	// org.glassfish.jersey.tests.e2e.common.message.internal.LinkProviderTest#testWithoutDoubleQuotes
	@Test
	@DisplayName("#fromHeaderValue parses without quotes")
	void fromHeaderValueParsesWithoutQuotes() {
		assertThatIsEqualToLink(fromHeaderValue(URI.create("https://example.com"), "<https://example.com/app/link1>; rel=self; other = bar"), "https://example.com/app/link1", Set.of("self"));
	}


	@Test
	@DisplayName("#createLink normalizes URI with dot segments unlike URI.normalize()")
	void createLinkNormalizesUri() {
		assertThatIsEqualToLink(createLink(URI.create("https://example.com"), "/../a/b", null), "https://example.com/a/b", Set.of());
	}

	private static void assertThatIsEqualToLink(Link actual, String url, Set<String> rel) {
		assertThat(actual).isEqualTo(new Link(URI.create(url), rel));
	}
}
