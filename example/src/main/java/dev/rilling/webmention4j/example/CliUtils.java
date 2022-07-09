package dev.rilling.webmention4j.example;

import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;

final class CliUtils {
	private CliUtils() {
	}

	@NotNull
	public static CommandLine parseArgs(@NotNull String[] args, @NotNull Options options) {
		CommandLine commandLine;
		try {
			commandLine = DefaultParser.builder().build().parse(options, args);
		} catch (ParseException e) {
			printHelp(options);
			throw new IllegalArgumentException("Failed to parse arguments.", e);
		}
		return commandLine;
	}

	public static void printHelp(@NotNull Options options) {
		new HelpFormatter().printHelp(" ", options);
	}
}
