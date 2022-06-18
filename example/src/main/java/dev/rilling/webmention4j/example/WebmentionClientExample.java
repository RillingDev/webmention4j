package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.client.WebmentionClient;
import dev.rilling.webmention4j.common.Webmention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

final class WebmentionClientExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionClientExample.class);

	private WebmentionClientExample() {
	}

	/**
	 * Sends webmention.
	 * <p>
	 * Argument 1: Source URL.
	 * Argument 2: Target URL.
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("Expecting 2 arguments.");
		}

		URI source = URI.create(args[0]);
		URI target = URI.create(args[1]);

		WebmentionClient webmentionClient = new WebmentionClient();
		try {
			if (!webmentionClient.supportsWebmention(target)) {
				LOGGER.info("No endpoint found for target URL.");
				return;
			}

			webmentionClient.sendWebmention(new Webmention(source, target));
			LOGGER.info("Success!");
		} catch (IOException e) {
			LOGGER.error("Unhandled error.", e);
		}
	}
}
