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

package releaser.internal.buildsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import releaser.internal.ReleaserProperties;
import releaser.internal.project.Project;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * Represents versions taken out from a release train POM.
 *
 * @author Marcin Grzejszczak
 */
public class VersionsFromBom {

	public static final VersionsFromBom EMPTY_VERSION = new VersionsFromBom();

	Set<Project> projects = new HashSet<>();

	ReleaserProperties properties;

	CustomBomParser parser;

	private VersionsFromBom() {
		this.properties = new ReleaserProperties();
		this.properties.getPom().setThisTrainBom("unknown-bom");
		this.properties.getMetaRelease().setReleaseTrainProjectName("unknown-bom");
	}

	VersionsFromBom(ReleaserProperties releaserProperties, CustomBomParser parser) {
		this.properties = releaserProperties;
		this.parser = parser;
	}

	VersionsFromBom(ReleaserProperties releaserProperties, CustomBomParser parser,
			Set<Project> projects) {
		this.properties = releaserProperties;
		this.parser = parser;
		projects.forEach(project -> setVersion(project.name, project.version));
	}

	VersionsFromBom(ReleaserProperties releaserProperties, CustomBomParser parser,
			VersionsFromBom... projects) {
		this.properties = releaserProperties;
		this.parser = parser;
		Arrays.stream(projects).forEach(p -> this.projects.addAll(p.projects));
	}

	private String bomProjectName() {
		return this.properties.getMetaRelease().getReleaseTrainProjectName();
	}

	private String dependenciesArtifactId() {
		String artifactId = this.properties.getPom().getThisTrainBom();
		return artifactId.split(File.separator)[0];
	}

	private String dependenciesParentArtifactId() {
		return dependenciesArtifactId() + "-parent";
	}

	public String versionForProject(String projectName) {
		return this.projects.stream().filter(project -> nameMatches(projectName, project))
				.findFirst().orElse(Project.EMPTY_PROJECT).version;
	}

	public boolean shouldBeUpdated(String projectName) {
		return this.projects.stream()
				.anyMatch(project -> nameMatches(projectName, project));
	}

	public boolean shouldSetProperty(Properties properties) {
		return this.projects.stream()
				.anyMatch(project -> properties.containsKey(project.name + ".version"));
	}

	public Projects toProjectVersions() {
		return this.projects.stream()
				.map(project -> new ProjectVersion(project.name, project.version))
				.collect(Collectors.toCollection(Projects::new));
	}

	/**
	 * The only exception is spring-cloud-dependencies (e.g. Greenwich.RELEASE) and
	 * spring-cloud-dependencies-parent (e.g. 2.1.0.RELEASE)
	 */
	private boolean nameMatches(String projectName, Project project) {
		if (project.name.equals(projectName)) {
			return true;
		}
		boolean parent = matchesNameWithSuffix(projectName, "-parent", project);
		boolean bomArtifactId = comparisonOfBomArtifactAndParent(projectName, project);
		return !bomArtifactId && (parent
				|| matchesNameWithSuffix(projectName, "-dependencies", project));
	}

	private boolean comparisonOfBomArtifactAndParent(String projectName,
			Project project) {
		return artifactOrParent(projectName, project.name)
				|| artifactOrParent(project.name, projectName);
	}

	private boolean artifactOrParent(String projectName, String otherProjectName) {
		return projectName.equals(dependenciesArtifactId())
				&& otherProjectName.equals(dependenciesParentArtifactId());
	}

	private boolean matchesNameWithSuffix(String projectName, String suffix,
			Project project) {
		boolean containsSuffix = projectName.endsWith(suffix);
		if (!containsSuffix) {
			return false;
		}
		String withoutSuffix = projectName.substring(0, projectName.indexOf(suffix));
		return project.name.equals(withoutSuffix);
	}

	public VersionsFromBom setVersion(String projectName, String version) {
		Set<Project> projects = parser.setVersion(this.projects, projectName, version);
		if (!projects.equals(this.projects)) {
			this.projects.clear();
			this.projects.addAll(projects);
			return this;
		}
		if (bomVersionProjectNames().contains(projectName)) {
			updateBomVersions(version);
		}
		else {
			remove(projectName);
			add(projectName, version);
		}
		return this;
	}

	private List<String> bomVersionProjectNames() {
		List<String> names = new ArrayList<>(
				this.properties.getMetaRelease().getReleaseTrainDependencyNames());
		names.add(this.properties.getMetaRelease().getReleaseTrainProjectName());
		return names;
	}

	private void updateBomVersions(String version) {
		remove(bomProjectName());
		bomVersionProjectNames().forEach(this::remove);
		add(bomProjectName(), version);
		bomVersionProjectNames().forEach(s -> add(s, version));
	}

	public void add(String key, String value) {
		this.projects.add(new Project(key, value));
	}

	public void remove(String expectedProjectName) {
		this.projects.removeIf(project -> expectedProjectName.equals(project.name));
	}

	public Set<Project> projects() {
		return this.projects;
	}

	@Override
	public String toString() {
		return "Projects=\n\t" + this.projects.stream().map(Object::toString)
				.collect(Collectors.joining("\n\t"));
	}

}
