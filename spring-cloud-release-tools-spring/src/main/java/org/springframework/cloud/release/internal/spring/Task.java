package org.springframework.cloud.release.internal.spring;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marcin Grzejszczak
 */
class Task {

	static StepSkipper stepSkipper = new ConsoleInputStepSkipper();

	private static final Logger log = LoggerFactory.getLogger(Task.class);
	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";

	final String name;
	final String shortName;
	final String header;
	final String description;
	final TaskType taskType;
	private final Consumer<Args> consumer;

	Task(String name, String shortName, String header, String description,
			Consumer<Args> consumer) {
		this(name, shortName, header, description, consumer, TaskType.RELEASE);
	}

	Task(String name, String shortName, String header, String description,
			Consumer<Args> consumer, TaskType taskType) {
		this.name = name;
		this.shortName = shortName;
		this.header = header;
		this.description = description;
		this.consumer = consumer;
		this.taskType = taskType;
	}

	void execute(Args args) {
		if (args.taskType != this.taskType) {
			log.info("Skipping [{}] since task type is [{}] and should be [{}]]",
					this.name, this.taskType, args.taskType);
			return;
		}
		try {
			boolean interactive = args.interactive;
			printLog(interactive);
			if (interactive) {
				boolean skipStep = stepSkipper.skipStep();
				if (!skipStep) {
					this.consumer.accept(args);
				}
			} else {
				this.consumer.accept(args);
			}
		} catch (Exception e) {
			log.error("\n\n\nBUILD FAILED!!!\n\nException occurred for task <" +
					this.name + "> \n\nwith description <" + this.description + ">\n\n", e);
			throw e;
		}
	}

	private void printLog(boolean interactive) {
			log.info("\n\n\n=== {} ===\n\n{} {}\n\n", this.header, this.description, interactive ? MSG : "");
	}
}
