package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.client.WebmentionClient;
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.util.HtmlUtils;
import dev.rilling.webmention4j.common.util.HttpUtils;
import dev.rilling.webmention4j.common.util.UriUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static dev.rilling.webmention4j.example.CliUtils.parseArgs;
import static dev.rilling.webmention4j.example.CliUtils.printHelp;

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
		.required(false)
		.build();

	private static final Option CRAWL = Option.builder()
		.option("c")
		.longOpt("crawl")
		.hasArg(false)
		.desc("Specifies that the source URL should be crawled and Webmentions should be sent for its links.")
		.required(false)
		.build();
	// TODO: invert this
	private static final Option SKIP_IDENTICAL_HOST = Option.builder()
		.option("sih")
		.longOpt("skip-identical-host")
		.hasArg(false)
		.desc("When used with '--%s', do not sent Webmention for links where the host is the same as the current one.".formatted(
			CRAWL.getLongOpt()))
		.required(false)
		.build();

	private static final Options OPTIONS = new Options().addOption(HELP)
		.addOption(SOURCE)
		.addOption(TARGET)
		.addOption(CRAWL)
		.addOption(SKIP_IDENTICAL_HOST);

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

		WebmentionClientExample webmentionClientExample = new WebmentionClientExample();
		if (commandLine.hasOption(CRAWL)) {
			webmentionClientExample.sendWebmentionForLinked(source, commandLine.hasOption(SKIP_IDENTICAL_HOST));
		} else if (commandLine.hasOption(TARGET)) {
			URI target = URI.create(commandLine.getOptionValue(TARGET));
			webmentionClientExample.sendWebmention(source, target);
		} else {
			throw new IllegalArgumentException("Either '--%s' or '--%s' has to be specified.".formatted(TARGET.getLongOpt(),
				CRAWL.getLongOpt()));
		}
	}

	private void sendWebmentionForLinked(@NotNull URI source, boolean skipIdenticalHost) {
		Document sourceDocument = readSourceDocument(source);
		for (Element element : sourceDocument.select(new HtmlUtils.LinkLikeElementEvaluator())) {
			URI target;
			try {
				target = new URI(HtmlUtils.LinkLikeElementEvaluator.getLink(element));
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Encountered illegal link.", e);
			}

			// TODO: check this
			// Resolve any relative links so we have a full URI.
			target = source.resolve(target);

			if (!UriUtils.isHttp(target)) {
				LOGGER.info("Skipping link '{}' due to unsupported scheme.", target);
				continue;
			}

			if (skipIdenticalHost && target.getHost().equals(source.getHost())) {
				LOGGER.info("Skipping link '{}' due having the same host as source.", target);
				continue;
			}

			sendWebmention(source, target);
		}
	}

	@NotNull
	private Document readSourceDocument(@NotNull URI source) {
		try (CloseableHttpClient httpClient = createHttpClient(); ClassicHttpResponse response = httpClient.execute(
			ClassicRequestBuilder.get(source).build())) {
			HttpUtils.validateResponse(response);

			if (!HtmlUtils.isHtml(response)) {
				throw new IllegalArgumentException("Response is not HTML.");
			}
			return HtmlUtils.parse(response);
		} catch (IOException e) {
			throw new IllegalStateException("Could not crawl URL.", e);
		}
	}

	@NotNull
	private CloseableHttpClient createHttpClient() {
		return HttpClients.custom()
			.setUserAgent(HttpUtils.createUserAgentString("webmention4j-client-example",
				WebmentionClientExample.class.getPackage()))
			.build();
	}


	private void sendWebmention(@NotNull URI source, @NotNull URI target) {
		Webmention webmention = new Webmention(source, target);
		WebmentionClient webmentionClient = new WebmentionClient();
		LOGGER.info("Sending Webmention '{}'.", webmention);
		try {
			if (!webmentionClient.supportsWebmention(target)) {
				LOGGER.info("No endpoint found for target URL.");
				return;
			}

			webmentionClient.sendWebmention(webmention);
			LOGGER.info("Success!");
		} catch (IOException e) {
			LOGGER.error("Unhandled error.", e);
		}
	}
}
