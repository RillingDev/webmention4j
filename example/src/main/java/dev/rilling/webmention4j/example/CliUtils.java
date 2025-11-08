package dev.rilling.webmention4j.example;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.IOException;

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
		HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
		try {
			formatter.printHelp("<command>", "", options, "", true);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to print help.", e);
		}
	}
}
