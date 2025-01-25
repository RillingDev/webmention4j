package dev.rilling.webmention4j.example;

import org.apache.commons.cli.*;
import org.checkerframework.checker.nullness.qual.NonNull;

final class CliUtils {
	private CliUtils() {
	}

	@NonNull
	public static CommandLine parseArgs(@NonNull String[] args, @NonNull Options options) {
		CommandLine commandLine;
		try {
			commandLine = DefaultParser.builder().build().parse(options, args);
		} catch (ParseException e) {
			printHelp(options);
			throw new IllegalArgumentException("Failed to parse arguments.", e);
		}
		return commandLine;
	}

	public static void printHelp(@NonNull Options options) {
		new HelpFormatter().printHelp(" ", options);
	}
}
