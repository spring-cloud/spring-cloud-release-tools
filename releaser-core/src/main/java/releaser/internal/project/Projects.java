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

package releaser.internal.project;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import releaser.internal.ReleaserProperties;

/**
 * Abstraction over collection of projects.
 *
 * @author Marcin Grzejszczak
 */
public class Projects extends HashSet<ProjectVersion> {

	public Projects(Set<ProjectVersion> versions) {
		addAll(versions);
	}

	@SuppressWarnings("unchecked")
	public Projects(ProjectVersion... versions) {
		addAll(new HashSet<>(Arrays.stream(versions).filter(Objects::nonNull)
				.collect(Collectors.toList())));
	}

	public Projects(List<ProjectVersion> versions) {
		addAll(versions.stream().filter(Objects::nonNull).collect(Collectors.toList()));
	}

	public static Projects forRollback(ReleaserProperties properties, Projects projects) {
		List<ProjectVersion> foundProjectsToBump = new LinkedList<>();
		List<ProjectVersion> foundProjectsToSkip = new LinkedList<>();
		projects.forEach(projectVersion -> {
			if (properties.getMetaRelease().getProjectsToSkip().stream()
					.anyMatch(projectVersion.projectName::startsWith)) {
				foundProjectsToSkip.add(projectVersion);
			}
			else {
				foundProjectsToBump.add(projectVersion);
			}
		});
		Projects newProjects = new Projects(foundProjectsToSkip);
		foundProjectsToBump.forEach(projectVersion -> newProjects
				.add(new ProjectVersion(projectVersion.projectName,
						projectVersion.postReleaseSnapshotVersion())));
		newProjects.addAll(foundProjectsToSkip);
		return newProjects;
	}

	private static IllegalStateException exception(Projects projects,
			String projectName) {
		return new IllegalStateException("Project with name [" + projectName
				+ "] is not present in the list of projects [" + projects.asList()
				+ "] . " + additionalErrorMessage(projectName));
	}

	private static String additionalErrorMessage(String projectName) {
		return "Either put it in the BOM or set it via the [--releaser.fixed-versions["
				+ projectName + "]=1.0.0.RELEASE] property";
	}

	public ProjectVersion releaseTrain(ReleaserProperties properties) {
		return stream()
				.filter(version -> version.projectName
						.equals(properties.getMetaRelease().getReleaseTrainProjectName())
						|| properties.getMetaRelease().getReleaseTrainDependencyNames()
								.contains(version.projectName))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Projects " + this
						+ " don't contain any of the following release train names ["
						+ properties.getMetaRelease().getReleaseTrainProjectName()
						+ "] or "
						+ properties.getMetaRelease().getReleaseTrainDependencyNames()));
	}

	@Override
	public boolean add(ProjectVersion projectVersion) {
		if (projectVersion == null) {
			return false;
		}
		return super.add(projectVersion);
	}

	public Projects filter(List<String> projectsToSkip) {
		return this.stream().filter(v -> !projectsToSkip.contains(v.projectName))
				.collect(Collectors.toCollection(Projects::new));
	}

	public Projects postReleaseSnapshotVersion(List<String> projectsToSkip) {
		Projects projects = this.stream().filter(v -> projectsToSkip(projectsToSkip, v))
				.collect(Collectors.toCollection(Projects::new));
		Projects bumped = this.stream().map(
				v -> new ProjectVersion(v.projectName, v.postReleaseSnapshotVersion()))
				.collect(Collectors.toCollection(Projects::new));
		Projects merged = new Projects(projects);
		merged.addAll(bumped);
		return merged;
	}

	private boolean projectsToSkip(List<String> projectsToSkip, ProjectVersion version) {
		return projectsToSkip.stream().anyMatch(version.projectName::startsWith);
	}

	public void remove(String projectName) {
		ProjectVersion projectVersion = forName(projectName);
		remove(projectVersion);
	}

	public ProjectVersion forFile(File projectRoot) {
		final ProjectVersion thisProject = new ProjectVersion(projectRoot);
		return this.stream()
				.filter(projectVersion -> projectVersion.projectName
						.equals(thisProject.projectName))
				.findFirst().orElseThrow(() -> exception(this, thisProject.projectName));
	}

	public ProjectVersion forName(String projectName) {
		return this.stream()
				.filter(projectVersion -> projectVersion.projectName.equals(projectName))
				.findFirst().orElseThrow(() -> exception(this, projectName));
	}

	public boolean containsProject(String projectName) {
		return this.stream().anyMatch(
				projectVersion -> projectVersion.projectName.equals(projectName));
	}

	public List<ProjectVersion> forNameStartingWith(String projectName) {
		return this.stream().filter(
				projectVersion -> projectVersion.projectName.startsWith(projectName))
				.collect(Collectors.toList());
	}

	public boolean containsSnapshots() {
		return this.stream().anyMatch(ProjectVersion::isSnapshot);
	}

	public String asList() {
		return this.stream().map(version -> version.projectName + ":" + version.version)
				.collect(Collectors.joining(","));
	}

	public Set<Project> asProjects() {
		return this.stream().map(projectVersion -> new Project(projectVersion.projectName,
				projectVersion.version)).collect(Collectors.toSet());
	}

	@Override
	public String toString() {
		return stream().map(v -> "[" + v.projectName + "=>" + v.version + "]")
				.collect(Collectors.joining("\n"));
	}

}
