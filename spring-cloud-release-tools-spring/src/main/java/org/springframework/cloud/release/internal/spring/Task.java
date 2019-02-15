/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.tech.MakeBuildUnstableException;

/**
 * @author Marcin Grzejszczak
 */
class Task {

	private static final Logger log = LoggerFactory.getLogger(Task.class);

	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";
	static StepSkipper stepSkipper = new ConsoleInputStepSkipper();

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
		args.publishEvent(new TaskCompleted(this, args.projectName(), taskAndException));
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
			}
			else {
				return runTask(args);
			}
		}
		catch (MakeBuildUnstableException atTheEnd) {
			logError("TASK FAILED - WILL MARK THE BUILD UNSTABLE AT THE END!!!", args,
					atTheEnd);
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
		log.error("\n\n\n" + prefix + "\n\nException occurred for project <"
				+ (args.project != null ? args.project.getName() : "") + "> task <"
				+ this.name + "> \n\nwith description <" + this.description + ">\n\n", e);
	}

	private TaskAndException runTask(Args args) {
		this.consumer.accept(args);
		return TaskAndException.success(this);
	}

	private void printLog(boolean interactive) {
		log.info("\n\n\n=== {} ===\n\n{} {}\n\n", this.header, this.description,
				interactive ? MSG : "");
	}

}
