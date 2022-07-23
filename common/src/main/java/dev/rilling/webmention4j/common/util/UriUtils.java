package dev.rilling.webmention4j.common.util;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public final class UriUtils {
	private UriUtils() {
	}

	/**
	 * @return if the given URL is localhost or a loopback IP address.
	 */
	public static boolean isLocalhost(@NotNull URI uri) throws UnknownHostException {
		// Handles 'localhost' check internally.
		return InetAddress.getByName(uri.getHost()).isLoopbackAddress();
	}
}
