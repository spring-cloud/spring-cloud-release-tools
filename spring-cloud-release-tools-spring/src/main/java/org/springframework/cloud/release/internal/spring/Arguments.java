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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.project.ProcessedProject;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;

public class Arguments {

	public final File project;

	public final Projects projects;

	public final ProjectVersion originalVersion;

	public final ProjectVersion versionFromBom;

	public final ReleaserProperties properties;

	public final Options options;

	public final ProjectToRun projectToRun;

	public final List<ProcessedProject> processedProjects;

	private Arguments(ProjectToRun thisProject, Projects projects, ProjectVersion currentProjectFromBom) {
		this.project = thisProject.thisProjectFolder;
		this.projects = projects;
		this.originalVersion = thisProject.originalVersion;
		this.versionFromBom = currentProjectFromBom;
		this.properties = thisProject.thisProjectReleaserProperties;
		this.options = thisProject.options;
		this.projectToRun = thisProject;
		this.processedProjects = new LinkedList<>(Collections.singletonList(new ProcessedProject(thisProject.thisProjectReleaserProperties, this.versionFromBom, this.originalVersion)));
	}

	// in this case the project will be the BOM
	private Arguments(ProjectToRun thisProject, List<ProcessedProject> processedProjects) {
		this.project = thisProject.thisProjectFolder;
		this.projects = new Projects(processedProjects.stream().map(p -> p.newProjectVersion).collect(Collectors.toSet()));
		this.originalVersion = thisProject.originalVersion;
		this.versionFromBom = thisProject.thisProjectVersionFromBom;
		this.properties = thisProject.thisProjectReleaserProperties;
		this.options = thisProject.options;
		this.projectToRun = thisProject;
		this.processedProjects = processedProjects;
	}

	public static Arguments forProject(ProjectToRun thisProject) {
		return new Arguments(thisProject, thisProject.allProjectsFromBom.allProjectVersionsFromBom, thisProject.allProjectsFromBom.currentProjectFromBom);
	}

	public static Arguments forPostRelease(ReleaserProperties properties, ProjectsToRun projectsToRun) {
		List<ProjectToRun> projects = projectsToRun.stream().map(ProjectToRun.ProjectToRunSupplier::get).collect(Collectors.toCollection(LinkedList::new));
		List<ProcessedProject> processedProjects = projectsToRunToProcessedProject(projects);
		ProjectToRun releaseTrainProject = projects.stream()
				.filter(p -> p.originalVersion.projectName.equals(properties.getMetaRelease().getReleaseTrainProjectName())).findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing release train version"));
		return new Arguments(releaseTrainProject, processedProjects);
	}

	private static List<ProcessedProject> projectsToRunToProcessedProject(List<ProjectToRun> projects) {
		return projects.stream().map(p -> new ProcessedProject(p.thisProjectReleaserProperties, p.thisProjectVersionFromBom, p.originalVersion)).collect(Collectors
				.toCollection(LinkedList::new));
	}
}