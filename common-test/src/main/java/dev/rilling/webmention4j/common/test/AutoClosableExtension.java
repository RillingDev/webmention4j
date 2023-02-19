package dev.rilling.webmention4j.common.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Supplier;

public final class AutoClosableExtension<T extends AutoCloseable> implements BeforeAllCallback, AfterAllCallback {
	private final Supplier<T> supplier;
	private T instance;

	public AutoClosableExtension(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		instance = supplier.get();
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		instance.close();
	}

	public T get() {
		return instance;
	}
}
