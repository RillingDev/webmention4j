package dev.rilling.webmention4j.example;

import org.apache.commons.cli.*;

final class CliUtils {
	private CliUtils() {
	}

	public static CommandLine parseArgs(String[] args, Options options) {
		CommandLine commandLine;
		try {
			commandLine = DefaultParser.builder().get().parse(options, args);
		} catch (ParseException e) {
			printHelp(options);
			throw new IllegalArgumentException("Failed to parse arguments.", e);
		}
		return commandLine;
	}

	public static void printHelp(Options options) {
		new HelpFormatter().printHelp(" ", options);
	}
}
