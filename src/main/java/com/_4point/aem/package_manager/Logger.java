package com._4point.aem.package_manager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Logger {
	public Logger log(String msg);
	public Logger log(Supplier<? extends String> msgSupplier);
	
	public static class NoOpLogger implements Logger {
		@Override
		public Logger log(String msg) {
			return this;
		}

		@Override
		public Logger log(Supplier<? extends String> msgSupplier) {
			return this;
		}
	}
	
	public static class PassThroughLogger implements Logger {
		Consumer<? super String> msgConsumer;

		PassThroughLogger(Consumer<? super String> msgConsumer) {
			this.msgConsumer = msgConsumer;
		}

		@Override
		public Logger log(String msg) {
			msgConsumer.accept(msg);
			return this;
		}

		@Override
		public Logger log(Supplier<? extends String> msgSupplier) {
			msgConsumer.accept(msgSupplier.get());
			return this;
		}
	}
}
