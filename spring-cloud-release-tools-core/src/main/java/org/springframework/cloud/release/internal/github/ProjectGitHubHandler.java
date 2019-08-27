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

package org.springframework.cloud.release.internal.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.tech.TemporaryFileStorage;

/**
 * Contains business logic around Github operations.
 *
 * @author Marcin Grzejszczak
 */
public class ProjectGitHubHandler implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory.getLogger(ProjectGitHubHandler.class);

	private final GithubMilestones githubMilestones;

	private final GithubIssues githubIssues;

	private ReleaserProperties properties;

	public ProjectGitHubHandler(ReleaserProperties properties) {
		this.properties = properties;
		this.githubMilestones = new GithubMilestones(properties);
		this.githubIssues = new GithubIssues(properties);
		registerShutdownHook();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(TemporaryFileStorage::cleanup));
	}

	public void closeMilestone(ProjectVersion releaseVersion) {
		this.githubMilestones.closeMilestone(releaseVersion);
	}

	public void createIssueInSpringGuides(Projects projects, ProjectVersion version) {
		this.githubIssues.fileIssueInSpringGuides(projects, version);
	}

	public void createIssueInStartSpringIo(Projects projects, ProjectVersion version) {
		this.githubIssues.fileIssueInStartSpringIo(projects, version);
	}

	public String milestoneUrl(ProjectVersion releaseVersion) {
		return this.githubMilestones.milestoneUrl(releaseVersion);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}
