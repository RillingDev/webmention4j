package dev.rilling.webmention4j.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

class WebmentionClientExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionClientExample.class);

	public static void main(String[] args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("Expecting 2 arguments.");
		}

		URI source = URI.create(args[0]);
		URI target = URI.create(args[1]);

		WebmentionClient webmentionClient = new WebmentionClient();
		try {
			webmentionClient.notify(source, target);
			LOGGER.info("Success!");
		} catch (IOException e) {
			LOGGER.error("Unhandled error.", e);
		}
	}
}
