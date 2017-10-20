package org.springframework.cloud.release.internal.spring;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class OptionsProcessor {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Releaser releaser;
	private final ReleaserProperties properties;
	private final List<Task> allTasks;

	OptionsProcessor(Releaser releaser, ReleaserProperties properties) {
		this(releaser, properties, Tasks.ALL_TASKS);
	}

	OptionsProcessor(Releaser releaser, ReleaserProperties properties, List<Task> allTasks) {
		this.releaser = releaser;
		this.properties = properties;
		this.allTasks = allTasks;
	}

	void processOptions(Options options, Args defaultArgs) {
		Args args = args(defaultArgs, options.interactive);
		if (options.fullRelease && !options.interactive) {
			log.info("Executing a full release in non-interactive mode");
			releaseTask().execute(args);
		} else if (options.fullRelease && options.interactive) {
			log.info("Executing a full release in interactive mode");
			releaseVerboseTask().execute(args);
		} else if (StringUtils.hasText(options.startFrom)) {
			startFrom(options, args);
		} else if (StringUtils.hasText(options.range)) {
			range(options.range, args);
		} else if (!options.taskNames.isEmpty()) {
			tasks(options.taskNames, args);
		} else if (options.interactive) {
			interactiveOnly(args);
		} else {
			throw new IllegalStateException("You haven't picked any recognizable option");
		}
	}

	Task releaseTask() {
		return Tasks.RELEASE;
	}

	Task releaseVerboseTask() {
		return Tasks.RELEASE_VERBOSE;
	}

	private void interactiveOnly(Args defaultArgs) {
		log.info(buildOptionsText().toString());
		executeTaskFromOption(defaultArgs);
	}

	private void tasks(List<String> taskNames, Args defaultArgs) {
		Tasks.forNames(this.allTasks, taskNames).forEach(task -> task.execute(defaultArgs));
	}

	private void range(String range, Args defaultArgs) {
		String[] splitRange = range.split("-");
		String start = splitRange[0];
		String stop = "";
		if (splitRange.length == 2) {
			stop = splitRange[1];
		}
		boolean started = false;
		boolean sameRange = start.equals(stop);
		for (Task task : this.allTasks) {
			if (start.equals(task.name) || start.equals(task.shortName)) {
				started = true;
				task.execute(defaultArgs);
				if (sameRange) {
					break;
				}
			} else if (started && (stop.equals(task.name) || stop.equals(task.shortName))) {
				task.execute(defaultArgs);
				break;
			} else if (started) {
				task.execute(defaultArgs);
			}
		}
	}

	private void startFrom(Options options, Args defaultArgs) {
		boolean started = false;
		for (Task task : this.allTasks) {
			if (options.startFrom.equals(task.name) || options.startFrom.equals(task.shortName)) {
				started = true;
				task.execute(defaultArgs);
			} else if (started) {
				task.execute(defaultArgs);
			}
		}
	}

	private StringBuilder buildOptionsText() {
		StringBuilder msg = new StringBuilder();
		msg.append("\n\n\n=== WHAT DO YOU WANT TO DO? ===\n\n");
		for (int i = 0; i < this.allTasks.size(); i++) {
			msg.append(i).append(") ").append(this.allTasks.get(i).description).append("\n");
		}
		msg.append("\n").append("You can pick a range of options by using the hyphen - e.g. '2-4' will execute jobs [2,3,4]\n");
		msg.append("You can execute all tasks starting from a job by using a hyphen and providing only one number - e.g. '8-' will execute jobs [8,9,10]\n");
		msg.append("You can execute given tasks by providing a comma separated list of tasks - e.g. '3,7,8' will execute jobs [3,7,8]\n");
		msg.append("\n").append("You can press 'q' to quit\n\n");
		return msg;
	}

	void executeTaskFromOption(Args defaultArgs) {
		String input = chosenOption();
		switch (input.toLowerCase()) {
		case "q":
			System.exit(0);
		default:
			if (input.contains("-")) {
				rangeInteractive(defaultArgs, input);
			} else if (input.contains(",")) {
				tasksInteractive(defaultArgs, input);
			} else {
				singleTask(defaultArgs, input);
			}
		}
	}

	private void singleTask(Args defaultArgs, String input) {
		int chosenOption = Integer.parseInt(input);
		Task task = this.allTasks.get(chosenOption);
		boolean interactive = false;
		if (task == Tasks.RELEASE_VERBOSE) {
			interactive = true;
		}
		log.info("\n\n\nYou chose [{}]: [{}]\n\n\n", chosenOption, task.description);
		task.execute(args(defaultArgs, interactive));
	}

	private void tasksInteractive(Args defaultArgs, String input) {
		List<String> tasks = Arrays.asList(input.split(","));
		List<String> taskNames = new ArrayList<>();
		for (String task : tasks) {
			Integer taskIndex = Integer.valueOf(task);
			taskNames.add(this.allTasks.get(taskIndex).name);
		}
		tasks(taskNames, defaultArgs);
	}

	private void rangeInteractive(Args defaultArgs, String input) {
		String[] range = input.split("-");
		Integer start = Integer.valueOf(range[0]);
		Integer stop = null;
		if (range.length == 2) {
			stop = Integer.valueOf(range[1]);
		}
		String firstName = this.allTasks.get(start).name;
		String second = stop != null ? this.allTasks.get(stop).name : "";
		range(firstName + "-" + second, defaultArgs);
	}

	private Args args(Args defaultArgs, boolean interactive) {
		return new Args(this.releaser, defaultArgs.project, defaultArgs.projects,
				defaultArgs.originalVersion, defaultArgs.versionFromScRelease, this.properties, interactive);
	}

	String chosenOption() {
		return System.console().readLine();
	}
}
