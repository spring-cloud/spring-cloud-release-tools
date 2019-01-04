package org.springframework.cloud.release.internal.spring;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.tech.MakeBuildUnstableException;

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

	TaskAndException execute(Args args) {
		TaskAndException taskAndException = doExecute(args);
		args.publishEvent(new TaskCompleted(this,
				args.projectName(), taskAndException));
		return taskAndException;
	}

	private TaskAndException doExecute(Args args) {
		if (args.taskType != this.taskType) {
			log.info("Skipping [{}] since task type is [{}] and should be [{}]]",
					this.name, this.taskType, args.taskType);
			return TaskAndException.skipped(this);
		}
		try {
			boolean interactive = args.interactive;
			printLog(interactive);
			if (interactive) {
				boolean skipStep = stepSkipper.skipStep();
				if (!skipStep) {
					return runTask(args);
				}
				return TaskAndException.skipped(this);
			} else {
				return runTask(args);
			}
		}
		catch (MakeBuildUnstableException atTheEnd) {
			logError("TASK FAILED - WILL MARK THE BUILD UNSTABLE AT THE END!!!", args, atTheEnd);
			return TaskAndException.failure(this, atTheEnd);
		}
		catch (Exception e) {
			logError("BUILD FAILED!!!", args, e);
			if (this.taskType == TaskType.RELEASE) {
				throw e;
			}
			return TaskAndException.failure(this, e);
		}
	}

	private void logError(String prefix, Args args, Exception e) {
		log.error("\n\n\n" + prefix + "\n\nException occurred for project <" +
				(args.project != null ? args.project.getName() : "") + "> task <" +
				this.name + "> \n\nwith description <" + this.description + ">\n\n", e);
	}

	private TaskAndException runTask(Args args) {
		this.consumer.accept(args);
		return TaskAndException.success(this);
	}

	private void printLog(boolean interactive) {
			log.info("\n\n\n=== {} ===\n\n{} {}\n\n", this.header, this.description, interactive ? MSG : "");
	}
}
