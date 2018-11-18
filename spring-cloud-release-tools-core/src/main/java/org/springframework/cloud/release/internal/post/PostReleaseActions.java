package org.springframework.cloud.release.internal.post;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.gradle.GradleUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.core.NestedExceptionUtils;

/**
 * @author Marcin Grzejszczak
 */
public class PostReleaseActions implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(PostReleaseActions.class);

	private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

	private final ProjectGitHandler projectGitHandler;
	private final ProjectPomUpdater projectPomUpdater;
	private final GradleUpdater gradleUpdater;
	private final ProjectBuilder projectBuilder;
	private final ReleaserProperties properties;

	public PostReleaseActions(ProjectGitHandler projectGitHandler,
			ProjectPomUpdater projectPomUpdater, GradleUpdater gradleUpdater, ProjectBuilder projectBuilder,
			ReleaserProperties properties) {
		this.projectGitHandler = projectGitHandler;
		this.projectPomUpdater = projectPomUpdater;
		this.gradleUpdater = gradleUpdater;
		this.projectBuilder = projectBuilder;
		this.properties = properties;
	}

	/**
	 * Clones the test project, updates it and runs tests
	 *
	 * @param projects - set of project with versions to assert against
	 */
	public void runUpdatedTests(Projects projects) {
		if (!this.properties.getGit().isRunUpdatedSamples() ||
				!this.properties.getMetaRelease().isEnabled()) {
			log.info("Will not update and run test samples, since the switch to do so "
					+ "is off. Set [releaser.git.run-updated-samples] to [true] to change that");
			return;
		}
		File file = this.projectGitHandler.cloneTestSamplesProject();
		ProjectVersion projectVersion = new ProjectVersion(file);
		String releaseTrainVersion  = projects.releaseTrain(this.properties).version;
		Projects newProjects = addVersionForTestsProject(projects, projectVersion, releaseTrainVersion);
		updateWithVersions(file, newProjects);
		this.projectBuilder.build(projectVersion, file.getAbsolutePath());
	}

	/**
	 * Clones all samples for the given project. For each of them, checks out the proper
	 * branch, updates all the poms with the new, bumped versions of release train projects,
	 * commits the changes and pushes them.
	 *
	 * @param projects - set of project with versions to assert against
	 */
	public void updateAllTestSamples(Projects projects) {
		if (!this.properties.getGit().isUpdateAllTestSamples() ||
				!this.properties.getMetaRelease().isEnabled()) {
			log.info("Will not update all test samples, since the switch to do so "
					+ "is off. Set [releaser.git.update-all-test-samples] to [true] to change that");
			return;
		}
		List<ProjectAndException> projectAndExceptions = this.properties.getGit()
				.getAllTestSampleUrls()
				.entrySet()
				.stream()
				.map(e -> updateAllProjects(projects, e))
				.map(this::getResult)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
		log.info("Updated all samples!");
		List<String> exceptionMessages = projectAndExceptions.stream()
				.filter(ProjectAndException::hasException)
				.map(e -> "Project [" + e.key + "] for url [" + e.url + "] "
						+ "has exception [" + Arrays
						.toString(NestedExceptionUtils.getMostSpecificCause(e.ex)
								.getStackTrace()) + "]")
				.collect(Collectors.toList());
		if (!exceptionMessages.isEmpty()) {
			log.warn("Exceptions were found while updating samples");
			log.warn(String.join("\n", exceptionMessages));
		} else {
			log.info("No exceptions were found while updating the samples");
		}
	}

	private Future<List<ProjectAndException>> updateAllProjects(Projects projects, Map.Entry<String, List<String>> e) {
		return SERVICE.submit(() -> {
			String key = e.getKey();
			List<String> value = e.getValue();
			log.info("Running version update for project [{}] and samples {}", key, value);
			ProjectVersion projectVersionForReleaseTrain = projects.forName(key);
			Projects postRelease = projects
					.postReleaseSnapshotVersion(this.properties.getMetaRelease().getProjectsToSkip());
			log.info("Versions to update the samples with \n" + postRelease.stream()
					.map(v -> "[" + v.projectName + " => " + v.version + "]")
					.collect(Collectors.joining("\n")));
			return value.stream()
					.map(url -> run(key, url, () ->
							commitUpdatedProject(projects, key, projectVersionForReleaseTrain, postRelease, url)))
					.map(this::getResult)
					.collect(Collectors.toList());
		});
	}

	private void commitUpdatedProject(Projects projects, String key, ProjectVersion projectVersionForReleaseTrain, Projects postRelease, String url) {
		String releaseTrainVersion = projects
				.forName(this.properties.getMetaRelease().getReleaseTrainProjectName()).version;
		String projectVersion = projects.forName(key).version;
		log.info("Running version update for project [{}], url [{}], "
				+ "release train version [{}] and project version [{}]", key, url,
				releaseTrainVersion, projectVersion);
		File file = this.projectGitHandler
				.cloneAndGuessBranch(url, releaseTrainVersion, projectVersion);
		Projects newPostRelease = new Projects(postRelease);
		newPostRelease.add(new ProjectVersion(file));
		updateWithVersions(file, newPostRelease);
		this.projectGitHandler
				.commit(file, "Updated versions after [" + releaseTrainVersion + "] "
						+ "release train and [" + projectVersionForReleaseTrain.version + "] ["
						+ key + "] project release");
		this.projectGitHandler.pushCurrentBranch(file);
	}

	private void updateWithVersions(File file, Projects newPostRelease) {
		this.projectPomUpdater
				.updateProjectFromReleaseTrain(file, newPostRelease,
						new ProjectVersion(file), false);
		this.gradleUpdater.updateProjectFromBom(file, newPostRelease,
				new ProjectVersion(file), false);
	}

	private ProjectAndFuture run(String key, String url, Runnable runnable) {
		return new ProjectAndFuture(key, url, SERVICE.submit(runnable));
	}

	private ProjectAndException getResult(ProjectAndFuture projectAndFuture) {
		Exception e = null;
		try {
			projectAndFuture.future.get(10, TimeUnit.MINUTES);
			log.info("Done!");
		}
		catch (Exception ex) {
			e = ex;
		}
		return new ProjectAndException(projectAndFuture.key, projectAndFuture.url, e);
	}

	private List<ProjectAndException> getResult(Future<List<ProjectAndException>> future) {
		try {
			return future.get(10, TimeUnit.MINUTES);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Clones the release train documentation project
	 *
	 * @param projects - set of project with versions to assert against
	 */
	public void generateReleaseTrainDocumentation(Projects projects) {
		if (!this.properties.getGit().isUpdateReleaseTrainDocs() ||
				!this.properties.getMetaRelease().isEnabled()) {
			log.info("Will not update the release train documentation, since the switch to do so "
					+ "is off. Set [releaser.git.update-release-train-docs] to [true] to change that");
			return;
		}
		File file = this.projectGitHandler.cloneReleaseTrainDocumentationProject();
		ProjectVersion projectVersion = new ProjectVersion(file);
		String releaseTrainVersion  = projects.releaseTrain(this.properties).version;
		Projects newProjects = addVersionForTestsProject(projects, projectVersion, releaseTrainVersion);
		updateWithVersions(file, newProjects);
		this.projectBuilder.generateReleaseTrainDocs(releaseTrainVersion, file.getAbsolutePath());
	}

	private Projects addVersionForTestsProject(Projects projects, ProjectVersion projectVersion,
			String releaseTrainVersion) {
		Projects newProjects = new Projects(projects);
		newProjects.add(new ProjectVersion(projectVersion.projectName, releaseTrainVersion));
		return newProjects;
	}

	@Override
	public void close() throws IOException {
		SERVICE.shutdown();
	}
}


class ProjectAndFuture {
	final String key;
	final String url;
	final Future future;

	ProjectAndFuture(String key, String url, Future future) {
		this.key = key;
		this.url = url;
		this.future = future;
	}
}

class ProjectAndException {
	final String key;
	final String url;
	final Exception ex;

	ProjectAndException(String key, String url, Exception ex) {
		this.key = key;
		this.url = url;
		this.ex = ex;
	}

	boolean hasException() {
		return this.ex != null;
	}
}