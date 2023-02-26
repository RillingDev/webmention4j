package dev.rilling.webmention4j.example;

import dev.rilling.webmention4j.client.WebmentionClient;
import dev.rilling.webmention4j.common.Webmention;
import dev.rilling.webmention4j.common.internal.HtmlUtils;
import dev.rilling.webmention4j.common.internal.HttpUtils;
import dev.rilling.webmention4j.common.internal.UriUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
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
	private static final Option INCLUDE_IDENTICAL_HOST = Option.builder()
		.option("iih")
		.longOpt("include-identical-host")
		.hasArg(false)
		.desc(("When used with '--%s', send Webmention for links where the host is the same as the current one." +
			" If omitted, these are skipped.").formatted(CRAWL.getLongOpt()))
		.required(false)
		.build();

	private static final Option ALLOW_LOCALHOST_ENDPOINT = Option.builder()
		.option("ale")
		.longOpt("allow-localhost-endpoint")
		.hasArg(false)
		.desc(
			"Configures if the client should send Webmentions to an endpoint that is localhost or a loopback IP address." +
				" If omitted, these are ignored.")
		.required(false)
		.build();

	private static final Options OPTIONS = new Options().addOption(HELP)
		.addOption(SOURCE)
		.addOption(TARGET)
		.addOption(CRAWL)
		.addOption(INCLUDE_IDENTICAL_HOST)
		.addOption(ALLOW_LOCALHOST_ENDPOINT);

	private final WebmentionClient webmentionClient;

	private WebmentionClientExample(WebmentionClient.Config config) {
		webmentionClient = new WebmentionClient(config);
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

		WebmentionClient.Config config = new WebmentionClient.Config();
		if (commandLine.hasOption(ALLOW_LOCALHOST_ENDPOINT)) {
			config.setAllowLocalhostEndpoint(true);
		}
		WebmentionClientExample webmentionClientExample = new WebmentionClientExample(config);

		if (commandLine.hasOption(CRAWL)) {
			webmentionClientExample.sendWebmentionForLinked(source, commandLine.hasOption(INCLUDE_IDENTICAL_HOST));
		} else if (commandLine.hasOption(TARGET)) {
			URI target = URI.create(commandLine.getOptionValue(TARGET));
			webmentionClientExample.sendWebmention(source, target);
		} else {
			throw new IllegalArgumentException("Either '--%s' or '--%s' has to be specified.".formatted(TARGET.getLongOpt(),
				CRAWL.getLongOpt()));
		}
	}

	private void sendWebmentionForLinked(URI source, boolean includeIdenticalHost) {
		Document sourceDocument = readSourceDocument(source);
		for (Element element : sourceDocument.select(new HtmlUtils.LinkLikeElementEvaluator())) {
			URI target;
			String linkStr = HtmlUtils.LinkLikeElementEvaluator.getLink(element);
			try {
				target = new URI(linkStr);
			} catch (URISyntaxException e) {
				LOGGER.warn("Skipping link '{}' due to invalid syntax.", linkStr, e);
				continue;
			}

			if (!includeIdenticalHost && (!target.isAbsolute() || target.getHost().equals(source.getHost()))) {
				LOGGER.info("Skipping link '{}' due having the same host as source.", target);
				continue;
			}

			if (!UriUtils.isHttp(target)) {
				LOGGER.info("Skipping link '{}' due to unsupported scheme '{}'.", target, target.getScheme());
				continue;
			}

			sendWebmention(source, target);
		}
	}

	private Document readSourceDocument(URI source) {
		try (CloseableHttpClient httpClient = createHttpClient()) {
			return httpClient.execute(
				ClassicRequestBuilder.get(source).build(),
				response -> {
					if (!HtmlUtils.isHtml(response) || response.getEntity() == null) {
						throw new IllegalArgumentException("Response is not HTML.");
					}
					return HtmlUtils.parse(response.getEntity());
				});
		} catch (IOException e) {
			throw new IllegalStateException("Could not crawl URL.", e);
		}
	}

	private CloseableHttpClient createHttpClient() {
		return HttpClients.custom()
			.setUserAgent(HttpUtils.createUserAgentString("webmention4j-client-example",
				WebmentionClientExample.class.getPackage()))
			.build();
	}


	private void sendWebmention(URI source, URI target) {
		Webmention webmention = new Webmention(source, target);
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
