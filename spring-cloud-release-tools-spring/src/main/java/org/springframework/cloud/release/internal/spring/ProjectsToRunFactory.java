/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.util.StringUtils;

class ProjectsToRunFactory {

	private static final Logger log = LoggerFactory.getLogger(ProjectsToRunFactory.class);

	private final VersionsToBumpFactory versionsToBumpFactory;
	private final Releaser releaser;
	private final ReleaserPropertiesUpdater updater;

	ProjectsToRunFactory(VersionsToBumpFactory versionsToBumpFactory, Releaser releaser, ReleaserPropertiesUpdater updater) {
		this.versionsToBumpFactory = versionsToBumpFactory;
		this.releaser = releaser;
		this.updater = updater;
	}

	ProjectsToRun get(OptionsAndProperties optionsAndProperties) {
		Options options = optionsAndProperties.options;
		ReleaserProperties properties = optionsAndProperties.properties;
		if (!options.metaRelease) {
			log.info(
					"Single project release picked. Will release only the current project");
			File projectFolder = projectFolder(properties);
			ProjectVersion version = new ProjectVersion(projectFolder);
			ProjectsFromBom projectsFromBom = this.versionsToBumpFactory.get(projectFolder);
			return new ProjectsToRun(new ProjectToRun(projectsFromBom, version, properties));
		}
		return metaReleaseProjectsToRun(options, properties);
	}

	private File projectFolder(ReleaserProperties properties) {
		String workingDir = properties.getWorkingDir();
		return new File(workingDir);
	}

	private ProjectsToRun metaReleaseProjectsToRun(Options options, ReleaserProperties originalProps) {
		return metaReleaseProjects(options, originalProps).stream().map(project -> {
			File clonedProjectFromOrg = this.releaser.clonedProjectFromOrg(project);
			ReleaserProperties properties = updatePropertiesIfCustomConfigPresent(
					originalProps.copy(), clonedProjectFromOrg);
			log.info("Successfully cloned the project [{}] to [{}]", project,
					clonedProjectFromOrg);
			ProjectVersion version = new ProjectVersion(clonedProjectFromOrg);
			ProjectsFromBom projectsFromBom = this.versionsToBumpFactory.get(clonedProjectFromOrg);
			return new ProjectToRun(projectsFromBom, version, properties);
		}).collect(Collectors.toCollection(ProjectsToRun::new));
	}

	private ReleaserProperties updatePropertiesIfCustomConfigPresent(
			ReleaserProperties copy, File clonedProjectFromOrg) {
		return this.updater.updateProperties(copy, clonedProjectFromOrg);
	}

	private List<String> metaReleaseProjects(Options options, ReleaserProperties properties) {
		List<String> projects = new ArrayList<>(
				properties.getFixedVersions().keySet());
		log.info("List of projects that should not be cloned {}",
				properties.getMetaRelease().getProjectsToSkip());
		List<String> filteredProjects = filterProjectsToSkip(projects, properties);
		log.info("List of all projects to clone before filtering {}", filteredProjects);
		if (StringUtils.hasText(options.startFrom)) {
			filteredProjects = filterStartFrom(options, filteredProjects);
		}
		else if (!options.taskNames.isEmpty()) {
			filteredProjects = filterTaskNames(options, filteredProjects);
		}
		log.info("\n\n\nFor meta-release, will release the projects {}\n\n\n",
				filteredProjects);
		return filteredProjects;
	}

	private List<String> filterProjectsToSkip(List<String> projects, ReleaserProperties properties) {
		return projects
					.stream().filter(project -> !properties.getMetaRelease()
							.getProjectsToSkip().contains(project))
					.collect(Collectors.toList());
	}

	private List<String> filterStartFrom(Options options, List<String> filteredProjects) {
		log.info("Start from option provided [{}]", options.startFrom);
		int projectIndex = filteredProjects.indexOf(options.startFrom);
		if (projectIndex < 0) {
			throw new IllegalStateException(
					"Project [" + options.startFrom + "] not found");
		}
		if (log.isDebugEnabled()) {
			log.debug("Index of project [{}] is [{}]", options.startFrom,
					projectIndex);
		}
		filteredProjects = filteredProjects.subList(projectIndex,
				filteredProjects.size());
		options.startFrom = "";
		return filteredProjects;
	}

	private List<String> filterTaskNames(Options options, List<String> filteredProjects) {
		log.info("Task names provided {}", options.taskNames);
		filteredProjects = filteredProjects.stream()
				.filter(project -> options.taskNames.contains(project))
				.collect(Collectors.toList());
		options.taskNames = new ArrayList<>();
		return filteredProjects;
	}

}
