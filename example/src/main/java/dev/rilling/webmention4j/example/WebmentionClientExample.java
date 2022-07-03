package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.client.WebmentionClient;
import dev.rilling.webmention4j.common.Webmention;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

final class WebmentionClientExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionClientExample.class);

	private static final Option SOURCE = Option.builder()
		.option("s")
		.longOpt("source")
		.hasArg(true)
		.desc("Source URL.")
		.required(true)
		.build();
	private static final Option TARGET = Option.builder()
		.option("t")
		.longOpt("target")
		.hasArg(true)
		.desc("Target URL.")
		.required(true)
		.build();
	private static final Options OPTIONS = new Options().addOption(SOURCE).addOption(TARGET);

	private WebmentionClientExample() {
	}

	/**
	 * Sends a Webmention.
	 * <p>
	 * Arguments:
	 * <ol>
	 *     <li>Source URL.</li>
	 *     <li>Target URL.</li>
	 * </ol>
	 */
	public static void main(String[] args) {
		CommandLine commandLine;
		try {
			commandLine = DefaultParser.builder().build().parse(OPTIONS, args);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Failed to parse arguments.", e);
		}

		URI source = URI.create(commandLine.getOptionValue(SOURCE));
		URI target = URI.create(commandLine.getOptionValue(TARGET));

		WebmentionClientExample webmentionClientExample = new WebmentionClientExample();
		webmentionClientExample.sendWebmention(source, target);
	}

	private void sendWebmention(URI source, URI target) {
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
