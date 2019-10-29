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
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All tasks that can be executed by the releaser.
 *
 * @author Marcin Grzejszczak
 */
final class Tasks {

	private Tasks() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	static Task UPDATING_POMS = task("updatePoms", "u", "UPDATING VERSIONS",
			"Update versions from the BOM",
			args -> args.releaser.updateProjectFromBom(args.project, args.projects,
					args.versionFromScRelease));
	static Task BUILD_PROJECT = task("build", "b", "BUILD PROJECT", "Build the project",
			args -> args.releaser.buildProject(args.versionFromScRelease));
	static Task COMMIT = task("commit", "c",
			"COMMITTING (ALL) AND PUSHING TAGS (NON-SNAPSHOTS)",
			"Commit, tag and push the tag", args -> args.releaser
					.commitAndPushTags(args.project, args.versionFromScRelease));
	static Task DEPLOY = task("deploy", "d", "ARTIFACT DEPLOYMENT",
			"Deploy the artifacts",
			args -> args.releaser.deploy(args.versionFromScRelease));
	static Task PUBLISH_DOCS = task("docs", "o", "PUBLISHING DOCS", "Publish the docs",
			args -> args.releaser.publishDocs(args.versionFromScRelease));
	static Task SNAPSHOTS = task("snapshots", "s",
			"REVERTING CHANGES & BUMPING VERSION (RELEASE ONLY)",
			"Go back to snapshots and bump originalVersion by patch",
			args -> args.releaser.rollbackReleaseVersion(args.project, args.projects,
					args.versionFromScRelease));
	static Task PUSH = task("push", "p", "PUSHING CHANGES", "Push the commits",
			args -> args.releaser.pushCurrentBranch(args.project));
	static Task CLOSE_MILESTONE = task("closeMilestone", "m", "CLOSING MILESTONE",
			"Close the milestone at Github",
			args -> args.releaser.closeMilestone(args.versionFromScRelease));
	static Task CREATE_TEMPLATES = task("createTemplates", "t", "CREATING TEMPLATES",
			"Create email / blog / tweet etc. templates", args -> {
				args.releaser.createEmail(args.versionFromScRelease, args.projects);
				args.releaser.createBlog(args.versionFromScRelease, args.projects);
				args.releaser.createTweet(args.versionFromScRelease, args.projects);
				args.releaser.createReleaseNotes(args.versionFromScRelease,
						args.projects);
			}, TaskType.POST_RELEASE);
	static Task UPDATE_GUIDES = task("updateGuides", "ug", "UPDATE GUIDES",
			"Updating Spring Guides", args -> {
				args.releaser.updateSpringGuides(args.versionFromScRelease, args.projects,
						args.processedProjects);
			}, TaskType.POST_RELEASE);
	static Task UPDATE_START_SPRING_IO = task("updateStartSpringIo", "us",
			"UPDATE START.SPRING.IO", "Updating start.spring.io", args -> {
				args.releaser.updateStartSpringIo(args.versionFromScRelease,
						args.projects);
			}, TaskType.POST_RELEASE);
	static Task UPDATE_SAGAN = task("updateSagan", "g", "UPDATE SAGAN",
			"Updating Sagan with release info", args -> {
				args.releaser.updateSagan(args.project, args.versionFromScRelease);
			});
	static Task UPDATE_DOCUMENTATION = task("updateDocumentation", "ud",
			"UPDATE DOCUMENTATION", "Updating documentation repository", args -> {
				args.releaser.updateDocumentationRepository(args.properties,
						args.versionFromScRelease);
			}, TaskType.POST_RELEASE);

	@Deprecated
	static Task UPDATE_SPRING_PROJECT_PAGE = task("updateSpringProjectPage", "up",
			"UPDATE SPRING PROJECT PAGE", "Updating Spring Project page", args -> {
				args.releaser.updateSpringProjectPage(args.projects);
			}, TaskType.POST_RELEASE);
	static Task RUN_UPDATED_SAMPLES = task("runUpdatedSample", "ru",
			"UPDATE AND RUN SAMPLES",
			"Updates the sample project with versions and runs samples", args -> {
				args.releaser.runUpdatedSamples(args.projects);
			}, TaskType.POST_RELEASE);
	static Task UPDATE_RELEASE_TRAIN_DOCUMENTATION = task("updateReleaseTrainDocs", "ur",
			"UPDATE RELEASE TRAIN DOCS", "Update release train documentation", args -> {
				args.releaser.generateReleaseTrainDocumentation(args.projects);
			}, TaskType.POST_RELEASE);
	static Task UPDATE_ALL_SAMPLES = task("updateAllSamples", "ua",
			"UPDATE ALL SAMPLES WITH RELEASE TRAIN BUMPED VERSIONS",
			"Update all samples with release train bumped versions", args -> {
				args.releaser.updateAllSamples(args.projects);
			}, TaskType.POST_RELEASE);
	static Task UPDATE_RELEASE_TRAIN_WIKI = task("updateReleaseTrainWiki", "uw",
			"UPDATE RELEASE TRAIN WIKI", "Update release train wiki page", args -> {
				args.releaser.updateReleaseTrainWiki(args.projects);
			}, TaskType.POST_RELEASE);

	static final List<Task> DEFAULT_TASKS_PER_PROJECT = Stream
			.of(Tasks.UPDATING_POMS, Tasks.BUILD_PROJECT, Tasks.COMMIT, Tasks.DEPLOY,
					Tasks.PUBLISH_DOCS, Tasks.SNAPSHOTS, Tasks.PUSH,
					Tasks.CLOSE_MILESTONE, Tasks.UPDATE_SAGAN)
			.collect(Collectors.toList());

	static final List<Task> DEFAULT_DRY_RUN_TASKS_PER_PROJECT = Stream
			.of(Tasks.UPDATING_POMS, Tasks.BUILD_PROJECT).collect(Collectors.toList());

	static final List<Task> DEFAULT_TASKS_PER_RELEASE = Stream
			.of(Tasks.RUN_UPDATED_SAMPLES, Tasks.CREATE_TEMPLATES, Tasks.UPDATE_GUIDES,
					Tasks.UPDATE_START_SPRING_IO,
					Tasks.UPDATE_RELEASE_TRAIN_DOCUMENTATION, Tasks.UPDATE_DOCUMENTATION,
					Tasks.UPDATE_RELEASE_TRAIN_WIKI, Tasks.UPDATE_ALL_SAMPLES)
			.collect(Collectors.toList());

	static final List<Task> NON_COMPOSITE_TASKS = new ArrayList<Task>() {
		{
			addAll(DEFAULT_TASKS_PER_PROJECT);
			addAll(DEFAULT_TASKS_PER_RELEASE);
		}
	};

	static Task RELEASE = Tasks.task("release", "fr", "FULL RELEASE",
			"Perform a full release of this project without interruptions",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT).accept(args));

	static Task DRY_RUN = Tasks.task("dryRun", "dr", "DRY RUN",
			"Perform a dry run release of a single project - bumps versions and installs them locally",
			args -> new CompositeConsumer(DEFAULT_DRY_RUN_TASKS_PER_PROJECT)
					.accept(args));

	static Task POST_RELEASE = Tasks.task("postRelease", "pr", "POST RELEASE TASKS",
			"Perform post release tasks for this release without interruptions",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_RELEASE).accept(args),
			TaskType.POST_RELEASE);
	static Task RELEASE_VERBOSE = Tasks.task("releaseVerbose", "r",
			"FULL VERBOSE RELEASE",
			"Perform a full release of this project in interactive mode (you'll be asked about skipping steps)",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT).accept(args));
	static Task META_RELEASE = Tasks.task("metaRelease", "x", "META RELEASE",
			"Perform a meta release of projects",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT,
					(args1 -> args.properties.getMetaRelease().setEnabled(true)))
							.accept(args));
	static Task META_RELEASE_DRY_RUN = Tasks.task("metaReleaseDryRun", "xdr",
			"META RELEASE DRY RUN", "Perform a meta release dry run of projects",
			args -> new CompositeConsumer(DEFAULT_DRY_RUN_TASKS_PER_PROJECT,
					(args1 -> args.properties.getMetaRelease().setEnabled(true)))
							.accept(args));

	static final List<Task> COMPOSITE_TASKS = Stream.of(RELEASE, RELEASE_VERBOSE, DRY_RUN,
			META_RELEASE, POST_RELEASE, META_RELEASE_DRY_RUN)
			.collect(Collectors.toList());

	static final List<Task> ALL_TASKS_PER_PROJECT = Stream
			.of(COMPOSITE_TASKS, DEFAULT_TASKS_PER_PROJECT, DEFAULT_TASKS_PER_RELEASE)
			.flatMap(List::stream).collect(Collectors.toList());

	static Task task(String name, String shortName, String header, String description,
			Consumer<Args> function) {
		return task(name, shortName, header, description, function, TaskType.RELEASE);
	}

	static Task task(String name, String shortName, String header, String description,
			Consumer<Args> function, TaskType taskType) {
		return new Task(name, shortName, header, description, function, taskType);
	}

	static List<Task> forNames(List<Task> tasks, List<String> names) {
		return tasks.stream().filter(
				task -> names.contains(task.name) || names.contains(task.shortName))
				.collect(Collectors.toList());
	}

	static String allTasksInOrder() {
		return ALL_TASKS_PER_PROJECT.stream().map(task -> task.name)
				.collect(Collectors.joining(","));
	}

}

enum TaskType {

	RELEASE, POST_RELEASE

}

class CompositeConsumer implements Consumer<Args> {

	private final List<Task> tasks;

	private final Consumer<Args> setup;

	CompositeConsumer(List<Task> tasks) {
		this.tasks = tasks;
		this.setup = args -> {
		};
	}

	CompositeConsumer(List<Task> tasks, Consumer<Args> setup) {
		this.tasks = tasks;
		this.setup = setup;
	}

	@Override
	public void accept(Args args) {
		this.setup.accept(args);
		this.tasks.forEach(task -> task.execute(args));
	}

}
