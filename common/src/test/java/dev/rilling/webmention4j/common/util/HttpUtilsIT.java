package dev.rilling.webmention4j.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static dev.rilling.webmention4j.common.util.HttpUtils.isLocalhost;
import static org.assertj.core.api.Assertions.assertThat;

class HttpUtilsIT {

	@Test
	@DisplayName("#isLocalhost returns true for localhost")
	void isLocalhostLocalhost() throws Exception {
		assertThat(isLocalhost(new URL("http://localhost"))).isTrue();
		assertThat(isLocalhost(new URL("http://localhost/foo/bar"))).isTrue();

		assertThat(isLocalhost(new URL("https://example.com"))).isFalse();
		assertThat(isLocalhost(new URL("https://example.com/foo/bar"))).isFalse();
	}


	@Test
	@DisplayName("#isLocalhost returns true for IPv4 loopback")
	void isLocalhostLoopbackAddressIpV4() throws Exception {
		assertThat(isLocalhost(new URL("http://127.0.0.1"))).isTrue();
		assertThat(isLocalhost(new URL("http://127.0.0.1/foo/bar"))).isTrue();

		// https://developers.google.com/style/examples#example-ip-addresses
		assertThat(isLocalhost(new URL("http://192.0.2.1"))).isFalse();
		assertThat(isLocalhost(new URL("http://192.0.2.1/foo/bar"))).isFalse();
	}

	@Test
	@DisplayName("#isLocalhost returns true for IPv6 loopback")
	void isLocalhostLoopbackAddressIpV6() throws Exception {
		assertThat(isLocalhost(new URL("http://[::1]"))).isTrue();
		assertThat(isLocalhost(new URL("http://[::1]/foo/bar"))).isTrue();

		// https://developers.google.com/style/examples#example-ip-addresses
		assertThat(isLocalhost(new URL("http://[2001:db8::]"))).isFalse();
		assertThat(isLocalhost(new URL("http://[2001:db8::]/foo/bar"))).isFalse();
	}
}
