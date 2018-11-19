package org.springframework.cloud.release.internal.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jakewharton.fliptables.FlipTableConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * All tasks that can be executed by the releaser
 *
 * @author Marcin Grzejszczak
 */
class Tasks {
	static Task UPDATING_POMS = task("updatePoms", "u",
			"UPDATING POMS",
			"Update poms with versions from Spring Cloud Release",
			args -> args.releaser.updateProjectFromBom(args.project, args.projects, args.versionFromScRelease));
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
	static Task UPDATE_RELEASE_TRAIN_DOCUMENTATION = task("updateReleaseTrainDocs", "ur",
			"UPDATE RELEASE TRAIN DOCS",
			"Update release train documentation",
			args -> {
				args.releaser.generateReleaseTrainDocumentation(args.projects);
	},TaskType.POST_RELEASE);
	static Task UPDATE_ALL_SAMPLES = task("updateAllSamples", "ua",
			"UPDATE ALL SAMPLES WITH RELEASE TRAIN BUMPED VERSIONS",
			"Update all samples with release train bumped versions",
			args -> {
				args.releaser.updateAllSamples(args.projects);
	},TaskType.POST_RELEASE);
	static Task UPDATE_RELEASE_TRAIN_WIKI = task("updateReleaseTrainWiki", "uw",
			"UPDATE RELEASE TRAIN WIKI",
			"Update release train wiki page",
			args -> {
				args.releaser.updateReleaseTrainWiki(args.projects);
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
			Tasks.UPDATE_RELEASE_TRAIN_DOCUMENTATION,
			Tasks.UPDATE_DOCUMENTATION,
			Tasks.UPDATE_SPRING_PROJECT_PAGE,
			Tasks.UPDATE_RELEASE_TRAIN_WIKI,
			Tasks.UPDATE_ALL_SAMPLES
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
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT).accept(args));
	static Task POST_RELEASE = Tasks.task("postRelease", "pr",
			"POST RELEASE TASKS",
			"Perform post release tasks for this release without interruptions",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_RELEASE).accept(args),
			TaskType.POST_RELEASE);
	static Task RELEASE_VERBOSE = Tasks.task("releaseVerbose", "r",
			"FULL VERBOSE RELEASE",
			"Perform a full release of this project in interactive mode (you'll be asked about skipping steps)",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT).accept(args));
	static Task META_RELEASE = Tasks.task("metaRelease", "x",
			"META RELEASE",
			"Perform a meta release of projects",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT,
					(args1 -> args.properties.getMetaRelease().setEnabled(true)))
					.accept(args));

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

class CompositeConsumer implements Consumer<Args> {

	private static final Logger log = LoggerFactory.getLogger(CompositeConsumer.class);

	private final List<Task> tasks;
	private final Consumer<Args> setup;

	CompositeConsumer(List<Task> tasks) {
		this.tasks = tasks;
		this.setup = args -> {};
	}

	CompositeConsumer(List<Task> tasks, Consumer<Args> setup) {
		this.tasks = tasks;
		this.setup = setup;
	}

	@Override
	public void accept(Args args) {
		this.setup.accept(args);
		List<Table> table = this.tasks.stream()
				.map(task -> new Table(task.execute(args)))
				.collect(Collectors.toList());
		String string = "\n\n***** BUILD REPORT *****\n\n"
				+ FlipTableConverters.fromIterable(table, Table.class)
				+ "\n\n***** BUILD REPORT *****\n\n";
		List<Table> brokenTasks = table.stream()
				.filter(table1 -> StringUtils.hasText(table1.thrownException))
				.collect(Collectors.toList());
		if (!brokenTasks.isEmpty()) {
			String brokenBuilds = "\n\n[BUILD UNSTABLE] One of the tasks is failing!\n\n" +
					FlipTableConverters.fromIterable(brokenTasks, Table.class) + "\n\n";
			log.info(string + brokenBuilds);
			throw new IllegalStateException("[BUILD UNSTABLE] One of the tasks is failing! + \n\n\n" + brokenBuilds);
		} else {
			log.info(string);
		}
	}


}

class Table {
	final String taskCaption;
	final String taskDescription;
	final String taskState;
	final String thrownException;

	Table(TaskAndException tae) {
		this.taskCaption = tae.task.name;
		this.taskDescription = tae.task.description;
		this.taskState = tae.taskState.name().toLowerCase();
		this.thrownException = tae.exception == null ? "" : Arrays
				.stream(tae.exception.getStackTrace())
				.map(s -> {
					String[] strings = s.toString().split("\\.");
					return strings[strings.length - 3] + "." + strings[strings.length - 2] + "." + strings[strings.length - 1];
				})
				.limit(15)
				.collect(Collectors.joining("\n"));
	}

	public String getTaskCaption() {
		return this.taskCaption;
	}

	public String getTaskDescription() {
		return this.taskDescription;
	}

	public String getTaskState() {
		return this.taskState;
	}

	public String getThrownException() {
		return this.thrownException;
	}
}