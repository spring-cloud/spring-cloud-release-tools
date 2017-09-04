package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.StringUtils;

/**
 * Releaser that gets input from console
 *
 * @author Marcin Grzejszczak
 */
public class SpringReleaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Releaser releaser;
	private final ReleaserProperties properties;
	private final List<Task> allTasks;

	@Autowired
	public SpringReleaser(Releaser releaser, ReleaserProperties properties) {
		this.releaser = releaser;
		this.properties = properties;
		this.allTasks = Tasks.ALL_TASKS;
	}

	SpringReleaser(Releaser releaser, ReleaserProperties properties, List<Task> allTasks) {
		this.releaser = releaser;
		this.properties = properties;
		this.allTasks = allTasks;
	}

	/**
	 * Current behaviour - interactive mode
	 */
	public void release() {
		release(new OptionsBuilder().options());
	}

	public void release(Options options) {
		printVersionRetrieval();
		String workingDir = this.properties.getWorkingDir();
		File project = new File(workingDir);
		ProjectVersion originalVersion = new ProjectVersion(project);
		Projects projects = this.releaser.retrieveVersionsFromSCRelease();
		ProjectVersion versionFromScRelease = projects.forFile(project);
		assertNoSnapshotsForANonSnapshotProject(projects, versionFromScRelease);
		final Args defaultArgs = new Args(this.releaser, project, projects, originalVersion, versionFromScRelease,
				this.properties, options.interactive);
		processOptions(options, defaultArgs);
	}

	void processOptions(Options options, Args args) {
		if (StringUtils.hasText(options.startFrom)) {
			startFrom(options, args);
		} else if (StringUtils.hasText(options.range)) {
			range(options, args);
		} else if (!options.taskNames.isEmpty()) {
			tasks(options, args);
		} else if (options.interactive) {
			interactiveOnly(args);
		} else {
			throw new IllegalStateException("You haven't picked any recognizable option");
		}
	}

	private void interactiveOnly(Args defaultArgs) {
		log.info(buildOptionsText().toString());
		int chosenOption = chosenOption();
		log.info("\n\n\nYou chose [{}]: [{}]\n\n\n", chosenOption, this.allTasks.get(chosenOption).description);
		boolean interactive = chosenOption == 1;
		Task task = taskFromOption(chosenOption);
		Args args = new Args(this.releaser, defaultArgs.project, defaultArgs.projects,
				defaultArgs.originalVersion, defaultArgs.versionFromScRelease, this.properties, interactive);
		task.consumer.accept(args);
	}

	private void tasks(Options options, Args defaultArgs) {
		Tasks.forNames(this.allTasks, options.taskNames).forEach(task -> task.consumer.accept(defaultArgs));
	}

	private void range(Options options, Args defaultArgs) {
		String[] splitRange = options.range.split("-");
		String start = splitRange[0];
		String stop = splitRange[1];
		boolean started = false;
		boolean sameRange = start.equals(stop);
		for (Task task : this.allTasks) {
			if (start.equals(task.name) || start.equals(task.shortName)) {
				started = true;
				task.consumer.accept(defaultArgs);
				if (sameRange) {
					break;
				}
			} else if (started) {
				task.consumer.accept(defaultArgs);
			} else if (started && (stop.equals(task.name) || stop.equals(task.shortName))) {
				task.consumer.accept(defaultArgs);
				break;
			}
		}
	}

	private void startFrom(Options options, Args defaultArgs) {
		boolean started = false;
		for (Task task : this.allTasks) {
			if (options.startFrom.equals(task.name) || options.startFrom.equals(task.shortName)) {
				started = true;
				task.consumer.accept(defaultArgs);
			} else if (started) {
				task.consumer.accept(defaultArgs);
			}
		}
	}

	private void assertNoSnapshotsForANonSnapshotProject(Projects projects,
			ProjectVersion versionFromScRelease) {
		if (!versionFromScRelease.isSnapshot() && projects.containsSnapshots()) {
			throw new IllegalStateException("You are trying to release a non snapshot "
					+ "version [" + versionFromScRelease + "] of the project [" + versionFromScRelease.projectName + "] but "
					+ "there is at least one SNAPSHOT library version in the Spring Cloud Release project");
		}
	}

	private StringBuilder buildOptionsText() {
		StringBuilder msg = new StringBuilder();
		msg.append("\n\n\n=== WHAT DO YOU WANT TO DO? ===\n\n");
		for (int i = 0; i < this.allTasks.size(); i++) {
			msg.append(i).append(") ").append(this.allTasks.get(i).description).append("\n");
		}
		msg.append("\n\n").append("You can press 'q' to quit\n\n");
		return msg;
	}

	private void printVersionRetrieval() {
		log.info("\n\n\n=== RETRIEVING VERSIONS ===\n\nWill clone Spring Cloud Release"
				+ " to retrieve all versions for the branch [{}]", this.properties.getPom().getBranch());
	}

	private Task taskFromOption(int option) {
		return this.allTasks.get(option);
	}

	int chosenOption() {
		String input = System.console().readLine();
		switch (input.toLowerCase()) {
		case "q":
			System.exit(0);
		default:
			return Integer.parseInt(input);
		}
	}
}

