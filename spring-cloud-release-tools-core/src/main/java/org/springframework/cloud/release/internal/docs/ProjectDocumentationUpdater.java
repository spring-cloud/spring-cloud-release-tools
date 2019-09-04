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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectDocumentationUpdater implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory
			.getLogger(ProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;

	private ReleaserProperties properties;

	private final List<CustomProjectDocumentationUpdater> updaters;

	public ProjectDocumentationUpdater(ReleaserProperties properties,
			ProjectGitHandler gitHandler,
			List<CustomProjectDocumentationUpdater> updaters) {
		this.gitHandler = gitHandler;
		this.properties = properties;
		this.updaters = updaters;
	}

	public File updateDocsRepo(ProjectVersion currentProject, String bomBranch) {
		if (!this.properties.getGit().isUpdateDocumentationRepo()) {
			log.info(
					"Will not update documentation repository, since the switch to do so "
							+ "is off. Set [releaser.git.update-documentation-repo] to [true] to change that");
			return null;
		}
		if (!currentProject.isReleaseOrServiceRelease()) {
			log.info(
					"Will not update documentation repository for non release or service release [{}]",
					currentProject.version);
			return null;
		}
		File documentationProject = this.gitHandler.cloneDocumentationProject();
		log.debug("Cloning the doc project to [{}]", documentationProject);
		CustomProjectDocumentationUpdater updater = this.updaters.stream().filter(
				u -> u.isApplicable(documentationProject, currentProject, bomBranch))
				.findFirst().orElse(CustomProjectDocumentationUpdater.NO_OP);
		return updater.updateDocsRepo(documentationProject, currentProject, bomBranch);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}
