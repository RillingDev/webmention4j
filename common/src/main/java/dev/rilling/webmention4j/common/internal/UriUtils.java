package dev.rilling.webmention4j.common.internal;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public final class UriUtils {

	private UriUtils() {
	}

	/**
	 * @return if the given URL is localhost or a loopback IP address.
	 */
	public static boolean isLocalhost(@NonNull URI uri) throws UnknownHostException {
		// Handles 'localhost' check internally.
		return InetAddress.getByName(uri.getHost()).isLoopbackAddress();
	}

	/**
	 * @return if the URL is HTTP or HTTPS.
	 */
	public static boolean isHttp(@NonNull URI uri) {
		return "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
	}
}
