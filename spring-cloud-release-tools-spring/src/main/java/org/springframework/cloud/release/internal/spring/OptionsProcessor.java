/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class OptionsProcessor {

	private static final Logger log = LoggerFactory.getLogger(OptionsProcessor.class);

	private final Releaser releaser;

	private final ReleaserProperties properties;

	private final List<Task> allTasks;

	private final ApplicationEventPublisher applicationEventPublisher;

	OptionsProcessor(Releaser releaser, ReleaserProperties properties,
			ApplicationEventPublisher applicationEventPublisher) {
		this(releaser, properties, applicationEventPublisher,
				Tasks.ALL_TASKS_PER_PROJECT);
	}

	OptionsProcessor(Releaser releaser, ReleaserProperties properties,
			ApplicationEventPublisher applicationEventPublisher, List<Task> allTasks) {
		this.releaser = releaser;
		this.properties = properties;
		this.allTasks = allTasks;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	OptionsProcessor(Releaser releaser, ReleaserProperties properties,
			List<Task> allTasks) {
		this.releaser = releaser;
		this.properties = properties;
		this.allTasks = allTasks;
		this.applicationEventPublisher = null;
	}

	void processOptions(Options options, Args defaultArgs) {
		processOptions(options, defaultArgs, this.allTasks);
	}

	void processOptions(Options options, Args defaultArgs, List<Task> tasks) {
		Args args = args(defaultArgs, options.interactive);
		if (args.taskType == TaskType.POST_RELEASE) {
			if (options.metaRelease) {
				postReleaseTask().execute(args);
				return;
			}
			String chosenOption = chosenOption();
			int pickedInteger = StringUtils.hasText(chosenOption)
					? Integer.parseInt(chosenOption) : -1;
			boolean pickedOptionIsComposite = pickedInteger <= 1 && pickedInteger >= 0;
			boolean pickedOptionIsFromPostRelease = pickedInteger >= Tasks.ALL_TASKS_PER_PROJECT
					.size() - Tasks.DEFAULT_TASKS_PER_RELEASE.size();
			if (options.metaRelease || options.fullRelease || pickedOptionIsComposite) {
				postReleaseTask().execute(args);
			}
			else if (pickedOptionIsFromPostRelease) {
				processNonComposite(options, tasks, args);
			}
			else {
				log.info("Picked option [{}] doesn't allow post release steps",
						pickedInteger);
			}
			return;
		}
		if (options.fullRelease && !options.interactive) {
			log.info("Executing a full release in non-interactive mode");
			releaseTask().execute(args);
		}
		else if (options.fullRelease && options.interactive) {
			log.info("Executing a full release in interactive mode");
			releaseVerboseTask().execute(args);
		}
		else {
			processNonComposite(options, tasks, args);
		}
	}

	private void processNonComposite(Options options, List<Task> tasks, Args args) {
		if (StringUtils.hasText(options.startFrom)) {
			startFrom(tasks, options, args);
		}
		else if (StringUtils.hasText(options.range)) {
			range(tasks, options.range, args);
		}
		else if (!options.taskNames.isEmpty()) {
			tasks(tasks, options.taskNames, args);
		}
		else if (options.interactive) {
			interactiveOnly(tasks, args);
		}
		else {
			throw new IllegalStateException("You haven't picked any recognizable option");
		}
	}

	void postReleaseOptions(Options options, Args defaultArgs) {
		Args args = args(defaultArgs, options.interactive);
		processOptions(options, args);
	}

	Task postReleaseTask() {
		return Tasks.POST_RELEASE;
	}

	Task releaseTask() {
		return Tasks.RELEASE;
	}

	Task releaseVerboseTask() {
		return Tasks.RELEASE_VERBOSE;
	}

	private void interactiveOnly(List<Task> tasks, Args defaultArgs) {
		if (defaultArgs.taskType != TaskType.POST_RELEASE) {
			log.info(buildOptionsText().toString());
		}
		executeTaskFromOption(tasks, defaultArgs);
	}

	private void tasks(List<Task> tasks, List<String> taskNames, Args defaultArgs) {
		Tasks.forNames(tasks, taskNames).forEach(task -> task.execute(defaultArgs));
	}

	private void range(List<Task> tasks, String range, Args defaultArgs) {
		String[] splitRange = range.split("-");
		String start = splitRange[0];
		String stop = "";
		if (splitRange.length == 2) {
			stop = splitRange[1];
		}
		boolean started = false;
		boolean sameRange = start.equals(stop);
		for (Task task : tasks) {
			if (start.equals(task.name) || start.equals(task.shortName)) {
				started = true;
				task.execute(defaultArgs);
				if (sameRange) {
					break;
				}
			}
			else if (started && (stop.equals(task.name) || stop.equals(task.shortName))) {
				task.execute(defaultArgs);
				break;
			}
			else if (started) {
				task.execute(defaultArgs);
			}
		}
	}

	private void startFrom(List<Task> tasks, Options options, Args defaultArgs) {
		boolean started = false;
		for (Task task : tasks) {
			if (options.startFrom.equals(task.name)
					|| options.startFrom.equals(task.shortName)) {
				started = true;
				task.execute(defaultArgs);
			}
			else if (started) {
				task.execute(defaultArgs);
			}
		}
	}

	private StringBuilder buildOptionsText() {
		StringBuilder msg = new StringBuilder();
		msg.append("\n\n\n=== WHAT DO YOU WANT TO DO? ===\n\n");
		for (int i = 0; i < this.allTasks.size(); i++) {
			msg.append(i).append(") ").append(this.allTasks.get(i).description)
					.append("\n");
		}
		msg.append("\n").append(
				"You can pick a range of options by using the hyphen - e.g. '2-4' will execute jobs [2,3,4]\n");
		msg.append("You can execute all tasks starting from a job "
				+ "by using a hyphen and providing only one "
				+ "number - e.g. '8-' will execute jobs [8,9,10]\n");
		msg.append("You can execute given tasks by providing a "
				+ "comma separated list of tasks - e.g. "
				+ "'3,7,8' will execute jobs [3,7,8]\n");
		msg.append("\n").append("You can press 'q' to quit\n\n");
		return msg;
	}

	void executeTaskFromOption(List<Task> tasks, Args defaultArgs) {
		String input = chosenOption();
		switch (input.toLowerCase()) {
		case "q":
			System.exit(0);
		default:
			if (input.contains("-")) {
				rangeInteractive(tasks, defaultArgs, input);
			}
			else if (input.contains(",")) {
				tasksInteractive(tasks, defaultArgs, input);
			}
			else {
				singleTask(tasks, defaultArgs, input);
			}
		}
	}

	private void singleTask(List<Task> tasks, Args defaultArgs, String input) {
		int chosenOption = Integer.parseInt(input);
		Task task = tasks.get(chosenOption);
		boolean interactive = false;
		if (task == Tasks.RELEASE_VERBOSE) {
			interactive = true;
		}
		log.info("\n\n\nYou chose [{}]: [{}]\n\n\n", chosenOption, task.description);
		task.execute(args(defaultArgs, interactive));
	}

	private void tasksInteractive(List<Task> tasks, Args defaultArgs, String input) {
		List<String> tasksFromInput = Arrays.asList(input.split(","));
		List<String> taskNames = new ArrayList<>();
		for (String task : tasksFromInput) {
			int taskIndex = Integer.parseInt(task);
			taskNames.add(tasks.get(taskIndex).name);
		}
		tasks(tasks, taskNames, defaultArgs);
	}

	private void rangeInteractive(List<Task> tasks, Args defaultArgs, String input) {
		String[] range = input.split("-");
		Integer start = Integer.valueOf(range[0]);
		Integer stop = null;
		if (range.length == 2) {
			stop = Integer.valueOf(range[1]);
		}
		String firstName = tasks.get(start).name;
		String second = stop != null ? tasks.get(stop).name : "";
		range(tasks, firstName + "-" + second, defaultArgs);
	}

	private Args args(Args defaultArgs, boolean interactive) {
		return new Args(this.releaser, defaultArgs.project, defaultArgs.projects,
				defaultArgs.originalVersion, defaultArgs.versionFromScRelease,
				this.properties, interactive, defaultArgs.taskType,
				this.applicationEventPublisher);
	}

	String chosenOption() {
		return System.console() == null ? "-1" : System.console().readLine();
	}

}
