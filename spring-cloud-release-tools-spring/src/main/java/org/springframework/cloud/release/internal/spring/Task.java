package org.springframework.cloud.release.internal.spring;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marcin Grzejszczak
 */
class Task {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String MSG = "'q' to quit and 's' to skip\n\n";

	final String name;
	final String shortName;
	final String header;
	final String description;
	final Consumer<Args> consumer;

	Task(String name, String shortName, String header, String description, Consumer<Args> consumer) {
		this.name = name;
		this.shortName = shortName;
		this.header = header;
		this.description = description;
		this.consumer = consumer;
	}

	void execute(Args args) {
		boolean interactive = args.interactive;
		printLog(interactive);
		if (interactive) {
			boolean skipStep = skipStep();
			if (!skipStep) {
				consumer.accept(args);
			}
		} else {
			consumer.accept(args);
		}
	}

	private void printLog(boolean shouldSkip) {
		log.info("\n\n\n=== {} ===\n\n{} {}\n\n", header, description, shouldSkip ? MSG : "");
	}

	boolean skipStep() {
		String input = System.console().readLine();
		switch (input.toLowerCase()) {
		case "s":
			return true;
		case "q":
			System.exit(0);
			return true;
		default:
			return false;
		}
	}

}
