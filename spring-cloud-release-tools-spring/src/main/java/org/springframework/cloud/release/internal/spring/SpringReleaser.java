package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

import static org.springframework.cloud.release.internal.spring.Task.task;

/**
 * Releaser that gets input from console
 *
 * @author Marcin Grzejszczak
 */
public class SpringReleaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Releaser releaser;
	private final ReleaserProperties properties;

	public SpringReleaser(Releaser releaser, ReleaserProperties properties) {
		this.releaser = releaser;
		this.properties = properties;
	}

	private final List<Task> TASKS = Stream.of(
			task("UPDATING POMS",
					"Update poms with versions from Spring Cloud Release",
					args -> args.releaser.updateProjectFromScRelease(args.project, args.projects)),
			task("BUILD PROJECT",
					"Build the project",
					args -> args.releaser.buildProject()),
			task("COMMITTING (ALL) AND PUSHING TAGS (NON-SNAPSHOTS)",
					"Commit, tag and push the tag",
					args -> args.releaser.commitAndPushTags(args.project, args.versionFromScRelease)),
			task("ARTIFACT DEPLOYMENT",
					"Deploy the artifacts to Artifactory",
					args -> args.releaser.deploy()),
			task("PUBLISHING DOCS",
					"Publish the docs",
					args -> args.releaser.publishDocs(args.versionFromScRelease)),
			task("REVERTING CHANGES & BUMPING VERSION (RELEASE ONLY)",
					"Go back to snapshots and bump originalVersion by patch",
					args -> args.releaser.rollbackReleaseVersion(args.project, args.originalVersion, args.versionFromScRelease)),
			task("PUSHING CHANGES",
					"Push the commits",
					args -> args.releaser.pushCurrentBranch(args.project)),
			task("CLOSING MILESTONE",
					"Close the milestone at Github",
					args -> args.releaser.closeMilestone(args.versionFromScRelease)),
			task("CREATING TEMPLATES",
					"Create email / tweet etc. templates",
					args -> {
						args.releaser.createEmail(args.versionFromScRelease);
						args.releaser.createBlog(args.versionFromScRelease, args.projects);
					})
	).collect(Collectors.toList());

	private final List<Task> COMPOSITE_TASKS = Stream.of(
			task("FULL RELEASE",
					"Perform a full release of this project without interruptions",
					args -> TASKS.forEach(task -> task.execute(args))),
			task("FULL VERBOSE RELEASE",
					"Perform a full release of this project in a verbose mode (you'll be asked about skipping steps)",
					args -> TASKS.forEach(task -> task.execute(args)))
	).collect(Collectors.toList());

	private final List<Task> ALL_TASKS = Stream.of(
			COMPOSITE_TASKS,
			TASKS
	).flatMap(List::stream).collect(Collectors.toList());

	public void release() {
		printVersionRetreival();
		String workingDir = this.properties.getWorkingDir();
		File project = new File(workingDir);
		ProjectVersion originalVersion = new ProjectVersion(project);
		Projects projects = this.releaser.retrieveVersionsFromSCRelease();
		ProjectVersion versionFromScRelease = projects.forFile(project);
		log.info(buildOptionsText().toString());
		int chosenOption = chosenOption();
		log.info("\n\n\nYou chose [{}]: [{}]\n\n\n", chosenOption, ALL_TASKS.get(chosenOption).description);
		boolean verbose = chosenOption == 1;
		Task task = taskFromOption(chosenOption);
		Args args = new Args(this.releaser, project, projects, originalVersion, versionFromScRelease,
				this.properties, verbose);
		task.consumer.accept(args);
	}

	private StringBuilder buildOptionsText() {
		StringBuilder msg = new StringBuilder();
		msg.append("\n\n\n=== WHAT DO YOU WANT TO DO? ===\n\n");
		for (int i = 0; i < ALL_TASKS.size(); i++) {
			msg.append(i).append(") ").append(ALL_TASKS.get(i).description).append("\n");
		}
		msg.append("\n\n").append("You can press 'q' to quit\n\n");
		return msg;
	}

	private void printVersionRetreival() {
		log.info("\n\n\n=== RETRIEVING VERSIONS ===\n\nWill clone Spring Cloud Release"
				+ " to retrieve all versions for the branch [{}]", this.properties.getPom().getBranch());
	}

	private Task taskFromOption(int option) {
		return ALL_TASKS.get(option);
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

class Task {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String MSG = "'q' to quit and 's' to skip\n\n";

	final String header;
	final String description;
	final Consumer<Args> consumer;

	Task(String header, String description, Consumer<Args> consumer) {
		this.header = header;
		this.description = description;
		this.consumer = consumer;
	}

	void execute(Args args) {
		boolean verbose = args.verbose;
		printLog(verbose);
		if (verbose) {
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

	static Task task(String header, String description, Consumer<Args> function) {
		return new Task(header, description, function);
	}
}

class Args {
	final Releaser releaser;
	final File project;
	final Projects projects;
	final ProjectVersion originalVersion;
	final ProjectVersion versionFromScRelease;
	final ReleaserProperties properties;
	final boolean verbose;

	Args(Releaser releaser, File project, Projects projects, ProjectVersion originalVersion,
			ProjectVersion versionFromScRelease, ReleaserProperties properties,
			boolean verbose) {
		this.releaser = releaser;
		this.project = project;
		this.projects = projects;
		this.originalVersion = originalVersion;
		this.versionFromScRelease = versionFromScRelease;
		this.properties = properties;
		this.verbose = verbose;
	}
}