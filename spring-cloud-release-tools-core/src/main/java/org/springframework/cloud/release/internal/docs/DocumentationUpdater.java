package org.springframework.cloud.release.internal.docs;

import java.io.File;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class DocumentationUpdater implements ReleaserPropertiesAware {

	private final ProjectDocumentationUpdater projectDocumentationUpdater;
	private final ReleaseTrainContentsUpdater releaseTrainContentsUpdater;
	private ReleaserProperties properties;

	public DocumentationUpdater(ProjectGitHandler gitHandler, ReleaserProperties properties) {
		this.properties = properties;
		this.projectDocumentationUpdater = new ProjectDocumentationUpdater(this.properties, gitHandler);
		this.releaseTrainContentsUpdater = new ReleaseTrainContentsUpdater(this.properties, gitHandler);
	}

	DocumentationUpdater(ReleaserProperties properties, ProjectDocumentationUpdater updater,
			ReleaseTrainContentsUpdater contentsUpdater) {
		this.properties = properties;
		this.projectDocumentationUpdater = updater;
		this.releaseTrainContentsUpdater = contentsUpdater;
	}

	/**
	 * Updates the documentation repository if current release train version is greater or equal
	 * than the one stored in the repo.
	 *
	 * @param currentProject
	 * @param springCloudReleaseBranch
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is used
	 */
	public File updateDocsRepo(ProjectVersion currentProject, String springCloudReleaseBranch) {
		return this.projectDocumentationUpdater
				.updateDocsRepo(currentProject, springCloudReleaseBranch);
	}

	/**
	 * Updates the project page if current release train version is greater or equal
	 * than the one stored in the repo.
	 *
	 * @param projects
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is used or the switch is turned off
	 */
	public File updateProjectRepo(Projects projects) {
		return this.releaseTrainContentsUpdater.updateProjectRepo(projects);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
		this.projectDocumentationUpdater.setReleaserProperties(properties);
		this.releaseTrainContentsUpdater.setReleaserProperties(properties);
	}
}
