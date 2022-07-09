package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.client.WebmentionClient;
import dev.rilling.webmention4j.common.Webmention;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static dev.rilling.webmention4j.example.CliUtils.parseArgs;
import static dev.rilling.webmention4j.example.CliUtils.printHelp;

// TODO: Allow crawling all links in a page and send Webmention for them.
public final class WebmentionClientExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebmentionClientExample.class);

	private static final Option HELP = Option.builder()
		.option("h")
		.longOpt("help")
		.hasArg(false)
		.desc("Shows this help text.")
		.required(false)
		.build();

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
	private static final Options OPTIONS = new Options().addOption(HELP).addOption(SOURCE).addOption(TARGET);

	private WebmentionClientExample() {
	}

	/**
	 * Sends a Webmention.
	 * <p>
	 * Call with `--help` for usage information.
	 */
	public static void main(String[] args) {
		CommandLine commandLine = parseArgs(args, OPTIONS);
		if (commandLine.hasOption(HELP)) {
			printHelp(OPTIONS);
			return;
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
