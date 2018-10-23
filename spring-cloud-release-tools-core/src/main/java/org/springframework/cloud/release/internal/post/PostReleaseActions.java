package org.springframework.cloud.release.internal.post;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.project.ProjectBuilder;

/**
 * @author Marcin Grzejszczak
 */
public class PostReleaseActions {

	private static final Logger log = LoggerFactory.getLogger(PostReleaseActions.class);

	private final ProjectGitHandler projectGitHandler;
	private final ProjectPomUpdater projectPomUpdater;
	private final ProjectBuilder projectBuilder;
	private final ReleaserProperties properties;

	public PostReleaseActions(ProjectGitHandler projectGitHandler,
			ProjectPomUpdater projectPomUpdater, ProjectBuilder projectBuilder,
			ReleaserProperties properties) {
		this.projectGitHandler = projectGitHandler;
		this.projectPomUpdater = projectPomUpdater;
		this.projectBuilder = projectBuilder;
		this.properties = properties;
	}

	/**
	 * Clones the test project, updates it and runs tests
	 *
	 * @param projects - set of project with versions to assert agains
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
		Projects newProjects = addVersionForTestsProject(projects, projectVersion);
		this.projectPomUpdater
				.updateProjectFromReleaseTrain(file, newProjects, projectVersion, false);
		this.projectBuilder.build(projectVersion, file.getAbsolutePath());
	}

	private Projects addVersionForTestsProject(Projects projects, ProjectVersion projectVersion) {
		Projects newProjects = new Projects(projects);
		newProjects.add(new ProjectVersion(projectVersion.projectName,
				projects.forName(this.properties.getMetaRelease().getReleaseTrainProjectName()).version));
		return newProjects;
	}
}
