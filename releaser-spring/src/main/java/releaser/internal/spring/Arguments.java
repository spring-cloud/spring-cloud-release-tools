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

package releaser.internal.spring;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.project.ProcessedProject;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import org.springframework.util.Assert;

/**
 * Arguments for a task.
 */
public final class Arguments implements Serializable {

	private static final Logger log = LoggerFactory.getLogger(Arguments.class);

	/**
	 * Cloned location of the project.
	 */
	public final File project;

	/**
	 * All projects taken from the BOM.
	 */
	public final Projects projects;

	/**
	 * Original version of this project before any manipulations.
	 */
	public final ProjectVersion originalVersion;

	/**
	 * Version of this project from the BOM.
	 */
	public final ProjectVersion versionFromBom;

	/**
	 * Releaser properties updated for this project.
	 */
	public final ReleaserProperties properties;

	/**
	 * Options updated for this project.
	 */
	public final Options options;

	/**
	 * Current project to run.
	 */
	public final ProjectToRun projectToRun;

	/**
	 * List of processed projects.
	 */
	public final List<ProcessedProject> processedProjects;

	private Arguments(ProjectToRun thisProject, Projects projects,
			ProjectVersion currentProjectFromBom) {
		this.project = thisProject.thisProjectFolder;
		this.projects = projects;
		this.originalVersion = thisProject.originalVersion;
		log.info("Creating Arguments for: " + thisProject.name() + "; Original version: "
				+ this.originalVersion);
		Assert.isTrue(this.originalVersion != null,
				"Original Version must not be null for project: " + thisProject.name());
		this.versionFromBom = currentProjectFromBom;
		this.properties = thisProject.thisProjectReleaserProperties;
		this.options = thisProject.options;
		this.projectToRun = thisProject;
		this.processedProjects = new LinkedList<>(Collections.singletonList(
				new ProcessedProject(thisProject.thisProjectReleaserProperties,
						this.versionFromBom, this.originalVersion)));
	}

	// in this case the project will be the BOM
	private Arguments(ProjectToRun thisProject,
			List<ProcessedProject> processedProjects) {

		this.project = thisProject.thisProjectFolder;
		this.projects = new Projects(processedProjects.stream()
				.map(p -> p.newProjectVersion).collect(Collectors.toSet()));
		this.originalVersion = thisProject.originalVersion;
		log.info("Creating Arguments for: " + thisProject.name() + "; Original version: "
				+ this.originalVersion);
		Assert.isTrue(this.originalVersion != null,
				"Original Version must not be null for project: " + thisProject.name());
		this.versionFromBom = thisProject.thisProjectVersionFromBom;
		this.properties = thisProject.thisProjectReleaserProperties;
		this.options = thisProject.options;
		this.projectToRun = thisProject;
		this.processedProjects = processedProjects;
	}

	public static Arguments forProject(ProjectToRun thisProject) {
		return new Arguments(thisProject,
				thisProject.allProjectsFromBom.allProjectVersionsFromBom,
				thisProject.allProjectsFromBom.currentProjectFromBom);
	}

	public static Arguments forPostRelease(ReleaserProperties properties,
			ProjectsToRun projectsToRun) {
		List<ProjectToRun> projects = projectsToRun.stream()
				.map(ProjectToRun.ProjectToRunSupplier::get)
				.collect(Collectors.toCollection(LinkedList::new));
		List<ProcessedProject> processedProjects = projectsToRunToProcessedProject(
				projects);
		ProjectToRun releaseTrainProject = projects.stream()
				.filter(p -> isReleaseTrainProject(properties, p)).findFirst()
				.orElseThrow(
						() -> new IllegalStateException("Missing release train version"));
		return new Arguments(releaseTrainProject, processedProjects);
	}

	private static boolean isReleaseTrainProject(ReleaserProperties properties,
			ProjectToRun p) {
		return p.originalVersion.projectName
				.equals(properties.getMetaRelease().getReleaseTrainProjectName())
				|| properties.getMetaRelease().getReleaseTrainDependencyNames()
						.contains(p.originalVersion.projectName)
				|| p.name()
						.equals(properties.getMetaRelease().getReleaseTrainProjectName());
	}

	private static List<ProcessedProject> projectsToRunToProcessedProject(
			List<ProjectToRun> projects) {
		return projects.stream()
				.map(p -> new ProcessedProject(p.thisProjectReleaserProperties,
						p.thisProjectVersionFromBom, p.originalVersion))
				.collect(Collectors.toCollection(LinkedList::new));
	}

	public ProjectVersion releaseTrain() {
		return this.projects.releaseTrain(this.properties);
	}

	@Override
	public String toString() {
		return "Arguments{" + "project=" + project + ", projects=" + projects
				+ ", originalVersion=" + originalVersion + ", versionFromBom="
				+ versionFromBom + ", properties=" + properties + ", options=" + options
				+ ", projectToRun=" + projectToRun + ", processedProjects="
				+ processedProjects + '}';
	}

}
