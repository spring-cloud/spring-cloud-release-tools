/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.cloud.release.internal.tasks.TrainPostReleaseReleaserTask;
import org.springframework.cloud.release.internal.tasks.composite.DryRunCompositeTask;
import org.springframework.cloud.release.internal.tasks.composite.MetaReleaseCompositeTask;
import org.springframework.cloud.release.internal.tasks.composite.MetaReleaseDryRunCompositeTask;
import org.springframework.cloud.release.internal.tasks.composite.ReleaseCompositeTask;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

class TasksToRunFactory {

	private static final Logger log = LoggerFactory.getLogger(TasksToRunFactory.class);

	private final ApplicationContext context;

	TasksToRunFactory(ApplicationContext context) {
		this.context = context;
	}

	TasksToRun release(OptionsAndProperties optionsAndProperties) {
		TasksToRun tasks = releaseTasks(optionsAndProperties);
		tasks.forEach(t -> t.setup(optionsAndProperties.options, optionsAndProperties.properties));
		return tasks;
	}

	private TasksToRun releaseTasks(OptionsAndProperties optionsAndProperties) {
		Options options = optionsAndProperties.options;
		ReleaserProperties properties = optionsAndProperties.properties;
		if (properties.isPostReleaseTasksOnly()) {
			return new TasksToRun();
		}
		if (options.metaRelease) {
			if (options.dryRun) {
				return new TasksToRun(this.context
						.getBean(MetaReleaseDryRunCompositeTask.class));
			}
			return new TasksToRun(this.context
					.getBean(MetaReleaseCompositeTask.class));
		}
		if (options.dryRun) {
			return new TasksToRun(this.context
					.getBean(DryRunCompositeTask.class));
		}
		else if (options.fullRelease) {
			return new TasksToRun(this.context
					.getBean(ReleaseCompositeTask.class));
		}
		// A single project release
		return tasksToRunForSingleProject(options,
				new LinkedList<>(this.context.getBeansOfType(ReleaserTask.class).values()));
	}

	TasksToRun postRelease() {
		return new TasksToRun(this.context
				.getBean(TrainPostReleaseReleaserTask.class));
	}

	private TasksToRun tasksToRunForSingleProject(Options options, List<ReleaserTask> tasks) {
		if (StringUtils.hasText(options.startFrom)) {
			return startFrom(tasks, options);
		}
		else if (StringUtils.hasText(options.range)) {
			return range(tasks, options.range);
		}
		else if (!options.taskNames.isEmpty()) {
			return tasks(tasks, options.taskNames);
		}
		else if (options.interactive) {
			return interactiveOnly(tasks);
		}
		else {
			throw new IllegalStateException("You haven't picked any recognizable option");
		}
	}

	private TasksToRun interactiveOnly(List<ReleaserTask> tasks) {
		log.info(buildOptionsText(tasks).toString());
		return taskFromOption(tasks);
	}

	private TasksToRun tasks(List<ReleaserTask> tasks, List<String> taskNames) {
		return tasks.stream().filter(t -> taskNames.contains(t.name())).collect(Collectors.toCollection(TasksToRun::new));
	}

	private TasksToRun range(List<ReleaserTask> tasks, String range) {
		String[] splitRange = range.split("-");
		String start = splitRange[0];
		String stop = "";
		if (splitRange.length == 2) {
			stop = splitRange[1];
		}
		boolean sameRange = start.equals(stop);
		TasksToRun tasksToRun = new TasksToRun();
		for (ReleaserTask task : tasks) {
			if (start.equals(task.name()) || start.equals(task.shortName())) {
				tasksToRun.add(task);
				if (sameRange) {
					break;
				}
			}
			else if (stop.equals(task.name()) || stop.equals(task.shortName())) {
				tasksToRun.add(task);
				break;
			}
		}
		return tasksToRun;
	}

	private TasksToRun startFrom(List<ReleaserTask> tasks, Options options) {
		TasksToRun tasksToRun = new TasksToRun();
		for (ReleaserTask task : tasks) {
			if (options.startFrom.trim().equals(task.name())
					|| options.startFrom.trim().equals(task.shortName())) {
				tasksToRun.add(task);
			}
		}
		return tasksToRun;
	}

	private StringBuilder buildOptionsText(List<ReleaserTask> allTasks) {
		StringBuilder msg = new StringBuilder();
		msg.append("\n\n\n=== WHAT DO YOU WANT TO DO? ===\n\n");
		for (int i = 0; i < allTasks.size(); i++) {
			msg.append(i).append(") ").append(allTasks.get(i).description())
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

	TasksToRun taskFromOption(List<ReleaserTask> tasks) {
		String input = chosenOption();
		if ("q".equals(input.toLowerCase())) {
			System.exit(0);
			return null;
		}
		if (input.contains("-")) {
			return rangeInteractive(tasks, input);
		}
		else if (input.contains(",")) {
			return tasksInteractive(tasks, input);
		}
		else {
			return singleTask(tasks, input);
		}
	}

	private TasksToRun singleTask(List<ReleaserTask> tasks, String input) {
		int chosenOption = Integer.parseInt(input);
		ReleaserTask task = tasks.get(chosenOption);
		log.info("\n\n\nYou chose [{}]: [{}]\n\n\n", chosenOption, task.description());
		return new TasksToRun(task);
	}

	private TasksToRun tasksInteractive(List<ReleaserTask> tasks, String input) {
		List<String> tasksFromInput = Arrays.asList(input.split(","));
		List<String> taskNames = new ArrayList<>();
		for (String task : tasksFromInput) {
			int taskIndex = Integer.parseInt(task);
			taskNames.add(tasks.get(taskIndex).name());
		}
		return tasks(tasks, taskNames);
	}

	private TasksToRun rangeInteractive(List<ReleaserTask> tasks, String input) {
		String[] range = input.split("-");
		Integer start = Integer.valueOf(range[0]);
		Integer stop = null;
		if (range.length == 2) {
			stop = Integer.valueOf(range[1]);
		}
		String firstName = tasks.get(start).name();
		String second = stop != null ? tasks.get(stop).name() : "";
		return range(tasks, firstName + "-" + second);
	}

	String chosenOption() {
		return System.console() == null ? "-1" : System.console().readLine();
	}
}
