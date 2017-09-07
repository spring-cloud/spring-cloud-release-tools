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
	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";

	final String name;
	final String shortName;
	final String header;
	final String description;
	private final Consumer<Args> consumer;

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

	private void printLog(boolean interactive) {
		log.info("\n\n\n=== {} ===\n\n{} {}\n\n", header, description, interactive ? MSG : "");
	}

	boolean skipStep() {
		String input = chosenOption();
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

	String chosenOption() {
		return System.console().readLine();
	}

}
