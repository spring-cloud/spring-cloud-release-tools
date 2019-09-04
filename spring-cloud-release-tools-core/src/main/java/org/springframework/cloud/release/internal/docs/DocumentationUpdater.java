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

package org.springframework.cloud.release.internal.docs;

import java.io.File;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.template.TemplateGenerator;

/**
 * @author Marcin Grzejszczak
 */
public class DocumentationUpdater implements ReleaserPropertiesAware {

	private final ProjectDocumentationUpdater projectDocumentationUpdater;

	private final ReleaseTrainContentsUpdater releaseTrainContentsUpdater;

	private ReleaserProperties properties;

	public DocumentationUpdater(ProjectGitHandler gitHandler,
			ReleaserProperties properties, TemplateGenerator templateGenerator,
			ProjectDocumentationUpdater updater) {
		this.properties = properties;
		this.projectDocumentationUpdater = updater;
		this.releaseTrainContentsUpdater = new ReleaseTrainContentsUpdater(
				this.properties, gitHandler, templateGenerator);
	}

	DocumentationUpdater(ReleaserProperties properties,
			ProjectDocumentationUpdater updater,
			ReleaseTrainContentsUpdater contentsUpdater) {
		this.properties = properties;
		this.projectDocumentationUpdater = updater;
		this.releaseTrainContentsUpdater = contentsUpdater;
	}

	/**
	 * Updates the documentation repository if current release train version is greater or
	 * equal than the one stored in the repo.
	 * @param currentProject the project we're parsing
	 * @param bomReleaseBranch branch of the BOM
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used
	 */
	public File updateDocsRepo(ProjectVersion currentProject, String bomReleaseBranch) {
		return this.projectDocumentationUpdater.updateDocsRepo(currentProject,
				bomReleaseBranch);
	}

	/**
	 * Updates the project page if current release train version is greater or equal than
	 * the one stored in the repo.
	 * @param projects list of projects to update versions for
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used or the switch is turned off
	 * @deprecated - index.html doesn't look like this anymore
	 */
	@Deprecated
	public File updateProjectRepo(Projects projects) {
		return this.releaseTrainContentsUpdater.updateProjectRepo(projects);
	}

	/**
	 * Updates the release train wiki page.
	 * @param projects list of projects to update versions for
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used or the switch is turned off
	 */
	public File updateReleaseTrainWiki(Projects projects) {
		return this.releaseTrainContentsUpdater.updateReleaseTrainWiki(projects);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
		this.releaseTrainContentsUpdater.setReleaserProperties(properties);
		this.projectDocumentationUpdater.setReleaserProperties(properties);
	}

}
