package dev.rilling.webmention4j.server;

import java.io.Serial;

final class BadRequestException extends Exception {

	@Serial
	private static final long serialVersionUID = -8108083179786850494L;

	/**
	 * @param message User-facing error message.
	 */
	BadRequestException(String message) {
		super(message);
	}

	/**
	 * @param message User-facing error message.
	 * @param cause   Exception cause.
	 */
	BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
