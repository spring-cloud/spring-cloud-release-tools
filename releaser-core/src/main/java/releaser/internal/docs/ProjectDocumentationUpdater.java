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

package releaser.internal.docs;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesAware;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
class ProjectDocumentationUpdater implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory
			.getLogger(ProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;

	private final List<CustomProjectDocumentationUpdater> updaters;

	private ReleaserProperties properties;

	ProjectDocumentationUpdater(ReleaserProperties properties,
			ProjectGitHandler gitHandler,
			List<CustomProjectDocumentationUpdater> updaters) {
		this.gitHandler = gitHandler;
		this.properties = properties;
		this.updaters = updaters;
	}

	public File updateDocsRepo(Projects projects, ProjectVersion currentProject,
			String bomBranch) {
		if (!shouldUpdate(currentProject)) {
			return null;
		}
		File documentationProject = this.gitHandler.cloneDocumentationProject();
		log.debug("Cloning the doc project to [{}]", documentationProject);
		CustomProjectDocumentationUpdater updater = this.updaters.stream().filter(
				u -> u.isApplicable(documentationProject, currentProject, bomBranch))
				.findFirst().orElse(CustomProjectDocumentationUpdater.NO_OP);
		return updater.updateDocsRepoForReleaseTrain(documentationProject, currentProject,
				projects, bomBranch);
	}

	public File updateDocsRepoForSingleProject(Projects projects,
			ProjectVersion currentProject) {
		if (!shouldUpdate(currentProject)) {
			return null;
		}
		File documentationProject = this.gitHandler.cloneDocumentationProject();
		log.debug("Cloning the doc project to [{}]", documentationProject);
		CustomProjectDocumentationUpdater updater = this.updaters.stream()
				.filter(u -> u.isApplicable(documentationProject, currentProject, null))
				.findFirst().orElse(CustomProjectDocumentationUpdater.NO_OP);
		return updater.updateDocsRepoForSingleProject(documentationProject,
				currentProject, projects);
	}

	private boolean shouldUpdate(ProjectVersion currentProject) {
		if (!this.properties.getGit().isUpdateDocumentationRepo()) {
			log.info(
					"Will not update documentation repository, since the switch to do so "
							+ "is off. Set [releaser.git.update-documentation-repo] to [true] to change that");
			return false;
		}
		if (!currentProject.isReleaseOrServiceRelease()) {
			log.info(
					"Will not update documentation repository for non release or service release [{}]",
					currentProject.version);
			return false;
		}
		return true;
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}
