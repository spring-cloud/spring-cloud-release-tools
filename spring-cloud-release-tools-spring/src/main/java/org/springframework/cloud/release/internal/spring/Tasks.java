package org.springframework.cloud.release.internal.spring;

import java.util.ArrayList;
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
				args.releaser.createEmail(args.versionFromScRelease, args.projects);
				args.releaser.createBlog(args.versionFromScRelease, args.projects);
				args.releaser.createTweet(args.versionFromScRelease, args.projects);
				args.releaser.createReleaseNotes(args.versionFromScRelease, args.projects);
	},TaskType.POST_RELEASE);
	static Task UPDATE_GUIDES = task("updateGuides", "ug",
			"UPDATE GUIDES",
			"Updating Spring Guides",
			args -> {
				args.releaser.updateSpringGuides(args.versionFromScRelease, args.projects);
	},TaskType.POST_RELEASE);
	static Task UPDATE_SAGAN = task("updateSagan", "g",
			"UPDATE SAGAN",
			"Updating Sagan with release info",
			args -> {
				args.releaser.updateSagan(args.project, args.versionFromScRelease);
	});
	static Task UPDATE_DOCUMENTATION = task("updateDocumentation", "ud",
			"UPDATE DOCUMENTATION",
			"Updating documentation repository",
			args -> {
				args.releaser.updateDocumentationRepository(args.properties, args.versionFromScRelease);
	},TaskType.POST_RELEASE);
	static Task UPDATE_SPRING_PROJECT_PAGE = task("updateSpringProjectPage", "up",
			"UPDATE SPRING PROJECT PAGE",
			"Updating Spring Project page",
			args -> {
				args.releaser.updateSpringProjectPage(args.projects);
	},TaskType.POST_RELEASE);
	static Task RUN_UPDATED_SAMPLES = task("runUpdatedSample", "ru",
			"UPDATE AND RUN SAMPLES",
			"Updates the sample project with versions and runs samples",
			args -> {
				args.releaser.runUpdatedSamples(args.projects);
	},TaskType.POST_RELEASE);

	static final List<Task> DEFAULT_TASKS_PER_PROJECT = Stream.of(
			Tasks.UPDATING_POMS,
			Tasks.BUILD_PROJECT,
			Tasks.COMMIT,
			Tasks.DEPLOY,
			Tasks.PUBLISH_DOCS,
			Tasks.SNAPSHOTS,
			Tasks.PUSH,
			Tasks.CLOSE_MILESTONE,
			Tasks.UPDATE_SAGAN
	).collect(Collectors.toList());

	static final List<Task> DEFAULT_TASKS_PER_RELEASE = Stream.of(
			Tasks.RUN_UPDATED_SAMPLES,
			Tasks.CREATE_TEMPLATES,
			Tasks.UPDATE_GUIDES,
			// Generate docs
			Tasks.UPDATE_DOCUMENTATION,
			Tasks.UPDATE_SPRING_PROJECT_PAGE
	).collect(Collectors.toList());

	static final List<Task> NON_COMPOSITE_TASKS = new ArrayList<Task>() {
		{
			addAll(DEFAULT_TASKS_PER_PROJECT);
			addAll(DEFAULT_TASKS_PER_RELEASE);
		}
	};

	static Task RELEASE = Tasks.task("release", "fr",
			"FULL RELEASE",
			"Perform a full release of this project without interruptions",
			args -> DEFAULT_TASKS_PER_PROJECT.forEach(task -> task.execute(args)));
	static Task POST_RELEASE = Tasks.task("postRelease", "pr",
			"POST RELEASE TASKS",
			"Perform post release tasks for this release without interruptions",
			args -> DEFAULT_TASKS_PER_RELEASE.forEach(task -> task.execute(args)),
			TaskType.POST_RELEASE);
	static Task RELEASE_VERBOSE = Tasks.task("releaseVerbose", "r",
			"FULL VERBOSE RELEASE",
			"Perform a full release of this project in interactive mode (you'll be asked about skipping steps)",
			args -> DEFAULT_TASKS_PER_PROJECT.forEach(task -> task.execute(args)));
	static Task META_RELEASE = Tasks.task("metaRelease", "x",
			"META RELEASE",
			"Perform a meta release of projects",
			args -> DEFAULT_TASKS_PER_PROJECT.forEach(task -> {
				args.properties.getMetaRelease().setEnabled(true);
				task.execute(args);
			}));

	static final List<Task> COMPOSITE_TASKS = Stream.of(
			RELEASE,
			RELEASE_VERBOSE,
			META_RELEASE,
			POST_RELEASE
	).collect(Collectors.toList());

	static final List<Task> ALL_TASKS_PER_PROJECT = Stream.of(
			COMPOSITE_TASKS, DEFAULT_TASKS_PER_PROJECT, DEFAULT_TASKS_PER_RELEASE
	).flatMap(List::stream).collect(Collectors.toList());

	static Task task(String name, String shortName, String header, String description,
			Consumer<Args> function) {
		return task(name, shortName, header, description, function, TaskType.RELEASE);
	}

	static Task task(String name, String shortName, String header, String description,
			Consumer<Args> function, TaskType taskType) {
		return new Task(name, shortName, header, description, function, taskType);
	}

	static List<Task> forNames(List<Task> tasks, List<String> names) {
		return tasks.stream()
				.filter(task -> names.contains(task.name) || names.contains(task.shortName))
				.collect(Collectors.toList());
	}

	static String allTasksInOrder() {
		return ALL_TASKS_PER_PROJECT.stream().map(task -> task.name).collect(Collectors.joining(","));
	}
}

enum TaskType {
	RELEASE, POST_RELEASE
}