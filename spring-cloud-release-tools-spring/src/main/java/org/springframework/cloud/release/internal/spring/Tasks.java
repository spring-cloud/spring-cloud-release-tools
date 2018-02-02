package org.springframework.cloud.release.internal.spring;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All tasks that can be executed by the releaser
 *
 * @author Marcin Grzejszczak
 */
class Tasks {
	static Task UPDATING_POMS = task("updatePoms", "u",
			"UPDATING POMS",
			"Update poms with versions from Spring Cloud Release",
			args -> args.releaser.updateProjectFromScRelease(args.project, args.projects, args.versionFromScRelease));
	static Task BUILD_PROJECT = task("build", "b",
			"BUILD PROJECT",
			"Build the project",
			args -> args.releaser.buildProject(args.versionFromScRelease));
	static Task COMMIT = task("commit", "c",
			"COMMITTING (ALL) AND PUSHING TAGS (NON-SNAPSHOTS)",
			"Commit, tag and push the tag",
			args -> args.releaser.commitAndPushTags(args.project, args.versionFromScRelease));
	static Task DEPLOY = task("deploy", "d",
			"ARTIFACT DEPLOYMENT",
			"Deploy the artifacts",
			args -> args.releaser.deploy(args.versionFromScRelease));
	static Task PUBLISH_DOCS = task("docs", "o",
			"PUBLISHING DOCS",
			"Publish the docs",
			args -> args.releaser.publishDocs(args.versionFromScRelease));
	static Task SNAPSHOTS = task("snapshots", "s",
			"REVERTING CHANGES & BUMPING VERSION (RELEASE ONLY)",
			"Go back to snapshots and bump originalVersion by patch",
			args -> args.releaser.rollbackReleaseVersion(args.project, args.projects, args.versionFromScRelease));
	static Task PUSH = task("push", "p",
			"PUSHING CHANGES",
			"Push the commits",
			args -> args.releaser.pushCurrentBranch(args.project));
	static Task CLOSE_MILESTONE = task("closeMilestone", "m",
			"CLOSING MILESTONE",
			"Close the milestone at Github",
			args -> args.releaser.closeMilestone(args.versionFromScRelease));
	static Task CREATE_TEMPLATES = task("createTemplates", "t",
			"CREATING TEMPLATES",
			"Create email / blog / tweet etc. templates",
			args -> {
				args.releaser.createEmail(args.versionFromScRelease);
				args.releaser.createBlog(args.versionFromScRelease, args.projects);
				args.releaser.createTweet(args.versionFromScRelease);
				args.releaser.createReleaseNotes(args.versionFromScRelease, args.projects);
	});
	static Task UPDATE_SAGAN = task("updateSagan", "g",
			"UPDATE SAGAN",
			"Updating Sagan with release info",
			args -> {
				args.releaser.updateSagan(args.project, args.versionFromScRelease);
	});

	static final List<Task> DEFAULT_TASKS = Stream.of(
			Tasks.UPDATING_POMS,
			Tasks.BUILD_PROJECT,
			Tasks.COMMIT,
			Tasks.DEPLOY,
			Tasks.PUBLISH_DOCS,
			Tasks.SNAPSHOTS,
			Tasks.PUSH,
			Tasks.CLOSE_MILESTONE,
			Tasks.CREATE_TEMPLATES,
			Tasks.UPDATE_SAGAN
	).collect(Collectors.toList());

	static Task RELEASE = Tasks.task("release", "r",
			"FULL RELEASE",
			"Perform a full release of this project without interruptions",
			args -> DEFAULT_TASKS.forEach(task -> task.execute(args)));
	static Task RELEASE_VERBOSE = Tasks.task("release-verbose", "r",
			"FULL VERBOSE RELEASE",
			"Perform a full release of this project in interactive mode (you'll be asked about skipping steps)",
			args -> DEFAULT_TASKS.forEach(task -> task.execute(args)));

	static final List<Task> COMPOSITE_TASKS = Stream.of(
			RELEASE,
			RELEASE_VERBOSE
	).collect(Collectors.toList());

	static final List<Task> ALL_TASKS = Stream.of(
			COMPOSITE_TASKS, DEFAULT_TASKS
	).flatMap(List::stream).collect(Collectors.toList());

	static Task task(String name, String shortName, String header, String description, Consumer<Args> function) {
		return new Task(name, shortName, header, description, function);
	}

	static List<Task> forNames(List<Task> tasks, List<String> names) {
		return tasks.stream()
				.filter(task -> names.contains(task.name) || names.contains(task.shortName))
				.collect(Collectors.toList());
	}

	static String tasksInOrder() {
		return DEFAULT_TASKS.stream().map(task -> task.name).collect(Collectors.joining(","));
	}
}
