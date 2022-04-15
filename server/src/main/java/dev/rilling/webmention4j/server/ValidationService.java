package dev.rilling.webmention4j.server;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Set;

class ValidationService {
	public static final Set<String> SUPPORTED_SCHEMES = Set.of("http", "https");

	public void validateSubmission(@NotNull URI source, @NotNull URI target) {
		// TODO: validate
	}
}
