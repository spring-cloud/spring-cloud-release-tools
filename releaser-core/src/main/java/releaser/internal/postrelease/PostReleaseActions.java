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

package releaser.internal.postrelease;

import java.io.Closeable;
import java.io.File;
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
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesUpdater;
import releaser.internal.buildsystem.GradleUpdater;
import releaser.internal.buildsystem.ProjectPomUpdater;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProcessedProject;
import releaser.internal.project.ProjectCommandExecutor;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.tech.ExecutionResult;
import releaser.internal.versions.VersionsFetcher;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class PostReleaseActions implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(PostReleaseActions.class);

	private static final ExecutorService SERVICE = Executors.newCachedThreadPool();

	private final ProjectGitHandler projectGitHandler;

	private final ProjectPomUpdater projectPomUpdater;

	private final GradleUpdater gradleUpdater;

	private final ProjectCommandExecutor projectCommandExecutor;

	private final ReleaserProperties properties;

	private final VersionsFetcher versionsFetcher;

	private final ReleaserPropertiesUpdater releaserPropertiesUpdater;

	public PostReleaseActions(ProjectGitHandler projectGitHandler,
			ProjectPomUpdater projectPomUpdater, GradleUpdater gradleUpdater,
			ProjectCommandExecutor projectCommandExecutor, ReleaserProperties properties,
			VersionsFetcher versionsFetcher,
			ReleaserPropertiesUpdater releaserPropertiesUpdater) {
		this.projectGitHandler = projectGitHandler;
		this.projectPomUpdater = projectPomUpdater;
		this.gradleUpdater = gradleUpdater;
		this.projectCommandExecutor = projectCommandExecutor;
		this.properties = properties;
		this.versionsFetcher = versionsFetcher;
		this.releaserPropertiesUpdater = releaserPropertiesUpdater;
	}

	/**
	 * Clones the projects, checks out the proper branch and runs guides building and
	 * deployment.
	 * @param processedProjects - set of project with versions to assert against
	 * @return result of the execution
	 */
	public ExecutionResult deployGuides(List<ProcessedProject> processedProjects) {
		if (!this.properties.getGit().isUpdateSpringGuides()) {
			log.info(
					"Will not build and deploy latest Spring Guides, since the switch to do so "
							+ "is off. Set [releaser.git.update-spring-guides] to [true] to change that");
			return ExecutionResult.skipped();
		}
		List<ProcessedProject> latestGaProcessedProjects = processedProjects.stream()
				.filter(processedProject -> this.versionsFetcher
						.isLatestGa(processedProject.newProjectVersion))
				.collect(Collectors.toList());
		log.info("Found the following latest ga processed projects "
				+ latestGaProcessedProjects);
		List<ProjectUrlAndException> projectUrlAndExceptions = runDeployGuides(
				latestGaProcessedProjects);
		log.info("Deployed all guides!");
		assertExceptions(projectUrlAndExceptions);
		return ExecutionResult.success();
	}

	/**
	 * Clones the test project, updates it and runs tests.
	 * @param projects - set of project with versions to assert against
	 * @return result of the execution
	 */
	public ExecutionResult runUpdatedTests(Projects projects) {
		if (!this.properties.getGit().isRunUpdatedSamples()
				|| !this.properties.getMetaRelease().isEnabled()) {
			log.info("Will not update and run test samples, since the switch to do so "
					+ "is off. Set [releaser.git.run-updated-samples] to [true] to change that");
			return ExecutionResult.skipped();
		}
		File file = this.projectGitHandler.cloneTestSamplesProject();
		ReleaserProperties projectProps = projectProps(file);
		ProjectVersion projectVersion = newProjectVersion(file);
		String releaseTrainVersion = projects.releaseTrain(projectProps).version;
		Projects newProjects = addVersionForTestsProject(projects, projectVersion,
				releaseTrainVersion);
		updateWithVersions(file, newProjects);
		this.projectCommandExecutor.build(projectProps, projectVersion, projectVersion,
				file.getAbsolutePath());
		return ExecutionResult.success();
	}

	ReleaserProperties projectProps(File file) {
		return this.releaserPropertiesUpdater.updateProperties(this.properties, file);
	}

	/**
	 * Clones all samples for the given project. For each of them, checks out the proper
	 * branch, updates all the poms with the new, bumped versions of release train
	 * projects, commits the changes and pushes them.
	 * @param projects - set of project with versions to assert against
	 * @return result of the execution
	 */
	public ExecutionResult updateAllTestSamples(Projects projects) {
		if (!this.properties.getGit().isUpdateAllTestSamples()
				|| !this.properties.getMetaRelease().isEnabled()) {
			log.info("Will not update all test samples, since the switch to do so "
					+ "is off. Set [releaser.git.update-all-test-samples] to [true] to change that");
			return ExecutionResult.skipped();
		}
		List<ProjectUrlAndException> projectUrlAndExceptions = this.properties.getGit()
				.getAllTestSampleUrls().entrySet().stream()
				.map(e -> updateAllProjects(projects, e)).map(this::getResult)
				.flatMap(Collection::stream).collect(Collectors.toList());
		log.info("Updated all samples!");
		assertExceptions(projectUrlAndExceptions);
		return ExecutionResult.success();
	}

	private void assertExceptions(List<ProjectUrlAndException> projectUrlAndExceptions) {
		String exceptionMessages = projectUrlAndExceptions.stream()
				.filter(ProjectUrlAndException::hasException)
				.map(e -> "Project [" + e.key + "] for url [" + e.url + "] "
						+ "has exception [\n\n"
						+ Arrays.stream(NestedExceptionUtils.getMostSpecificCause(e.ex)
								.getStackTrace()).map(StackTraceElement::toString)
								.collect(Collectors.joining("\n"))
						+ "]")
				.collect(Collectors.joining("\n"));
		if (StringUtils.hasText(exceptionMessages)) {
			throw new IllegalStateException(
					"Exceptions were found while running post release tasks\n"
							+ exceptionMessages);
		}
		else {
			log.info("No exceptions were found while running post release tasks");
		}
	}

	private List<ProjectUrlAndException> runDeployGuides(
			List<ProcessedProject> latestGaProcessedProjects) {
		return latestGaProcessedProjects.stream()
				.map(processedProject -> run(processedProject.projectName(), "",
						() -> SERVICE.submit(() -> {
							String tagName = processedProject.newProjectVersion
									.releaseTagName();
							File clonedProject = this.projectGitHandler
									.cloneProjectFromOrg(processedProject.projectName());
							this.projectGitHandler.checkout(clonedProject, tagName);
							projectBuilder(processedProject).deployGuides(
									processedProject.propertiesForProject,
									processedProject.originalProjectVersion,
									processedProject.newProjectVersion);
						})))
				.map(this::getSingleResult).collect(Collectors.toList());
	}

	ProjectCommandExecutor projectBuilder(ProcessedProject processedProject) {
		return new ProjectCommandExecutor();
	}

	private Future<List<ProjectUrlAndException>> updateAllProjects(Projects projects,
			Map.Entry<String, List<String>> e) {
		return SERVICE.submit(() -> {
			String key = e.getKey();
			List<String> value = e.getValue();
			log.info("Running version update for project [{}] and samples {}", key,
					value);
			ProjectVersion projectVersionForReleaseTrain = projects.forName(key);
			Projects postRelease = getPostReleaseProjects(projects);
			log.info("Versions to update the samples with \n" + postRelease.stream()
					.map(v -> "[" + v.projectName + " => " + v.version + "]")
					.collect(Collectors.joining("\n")));
			return value.stream()
					.map(url -> run(key, url,
							() -> commitUpdatedProject(projects, key,
									projectVersionForReleaseTrain, postRelease, url)))
					.map(this::getSingleResult).collect(Collectors.toList());
		});
	}

	Projects getPostReleaseProjects(Projects projects) {
		return projects.postReleaseSnapshotVersion(
				this.properties.getMetaRelease().getProjectsToSkip());
	}

	private void commitUpdatedProject(Projects projects, String key,
			ProjectVersion projectVersionForReleaseTrain, Projects postRelease,
			String url) {
		String releaseTrainVersion = projects.releaseTrain(this.properties).version;
		String projectVersion = projects.forName(key).version;
		log.info(
				"Running version update for project [{}], url [{}], "
						+ "release train version [{}] and project version [{}]",
				key, url, releaseTrainVersion, projectVersion);
		File file = this.projectGitHandler.cloneAndGuessBranch(url, releaseTrainVersion,
				projectVersion);
		Projects newPostRelease = new Projects(postRelease);
		ProjectVersion newProjectVersion = newProjectVersion(file);
		newPostRelease.add(newProjectVersion);
		updateWithVersions(file, newPostRelease);
		this.projectGitHandler.commit(file,
				"Updated versions after [" + releaseTrainVersion + "] "
						+ "release train and [" + projectVersionForReleaseTrain.version
						+ "] [" + key + "] project release");
		this.projectGitHandler.pushCurrentBranch(file);
	}

	private ReleaserProperties updatedProperties(File file) {
		ReleaserPropertiesUpdater updater = new ReleaserPropertiesUpdater();
		return updater.updateProperties(this.properties, file);
	}

	private ProjectVersion newProjectVersion(File file) {
		try {
			return new ProjectVersion(file);
		}
		catch (Exception ex) {
			ProjectVersion projectVersion = ProjectVersion.notMavenProject(file);
			String name = projectVersion.projectName;
			String version = projectVersion.version;
			log.warn(
					"Exception occurred while trying to read the pom file. Will assume that the project name is ["
							+ name + "] and version [" + version + "]",
					ex);
			return projectVersion;
		}
	}

	private void updateWithVersions(File file, Projects newPostRelease) {
		ReleaserProperties updatedProperties = updatedProperties(file);
		this.projectPomUpdater.updateProjectFromReleaseTrain(file, newPostRelease,
				newProjectVersion(file), false);
		this.gradleUpdater.updateProjectFromReleaseTrain(updatedProperties, file,
				newPostRelease, newProjectVersion(file), false);
	}

	private ProjectAndFuture run(String key, String url, Runnable runnable) {
		return new ProjectAndFuture(key, url, SERVICE.submit(runnable));
	}

	private ProjectUrlAndException getSingleResult(ProjectAndFuture projectAndFuture) {
		Exception e = null;
		try {
			projectAndFuture.future.get(10, TimeUnit.MINUTES);
			log.info("Done!");
		}
		catch (Exception ex) {
			e = ex;
		}
		return new ProjectUrlAndException(projectAndFuture.key, projectAndFuture.url, e);
	}

	private List<ProjectUrlAndException> getResult(
			Future<List<ProjectUrlAndException>> future) {
		try {
			return future.get(10, TimeUnit.MINUTES);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Clones the release train documentation project.
	 * @param projects - set of project with versions to assert against
	 * @return result of the execution
	 */
	public ExecutionResult generateReleaseTrainDocumentation(Projects projects) {
		if (!this.properties.getGit().isUpdateReleaseTrainDocs()
				|| !this.properties.getMetaRelease().isEnabled()) {
			log.info(
					"Will not update the release train documentation, since the switch to do so "
							+ "is off. Set [releaser.git.update-release-train-docs] to [true] to change that");
			return ExecutionResult.skipped();
		}
		ProjectVersion releaseTrain = projects.releaseTrain(this.properties);
		File file = this.projectGitHandler
				.cloneReleaseTrainDocumentationProject(releaseTrain.releaseTagName());
		ReleaserProperties projectProps = projectProps(file);
		String releaseTrainVersion = releaseTrain.version;
		this.projectCommandExecutor.generateReleaseTrainDocs(projectProps,
				releaseTrainVersion, file.getAbsolutePath());
		return ExecutionResult.success();
	}

	private Projects addVersionForTestsProject(Projects projects,
			ProjectVersion projectVersion, String releaseTrainVersion) {
		Projects newProjects = new Projects(projects);
		newProjects
				.add(new ProjectVersion(projectVersion.projectName, releaseTrainVersion));
		return newProjects;
	}

	@Override
	public void close() {
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

class ProjectUrlAndException {

	final String key;

	final String url;

	final Exception ex;

	ProjectUrlAndException(String key, String url, Exception ex) {
		this.key = key;
		this.url = url;
		this.ex = ex;
	}

	boolean hasException() {
		return this.ex != null;
	}

}
