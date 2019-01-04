/*
 *  Copyright 2013-2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.ReleaserProperties;

import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.BOOT_DEPENDENCIES_ARTIFACT_ID;
import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.BOOT_STARTER_PARENT_ARTIFACT_ID;
import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.BUILD_ARTIFACT_ID;
import static org.springframework.cloud.release.internal.pom.SpringCloudConstants.CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID;

/**
 * Represents versions taken out from Spring Cloud Release pom
 *
 * @author Marcin Grzejszczak
 */
class Versions {

	private static final String SPRING_BOOT_PROJECT_NAME = "spring-boot";
	static final Versions EMPTY_VERSION = new Versions("");

	String bootVersion;
	String scBuildVersion;
	Set<Project> projects = new HashSet<>();
	ReleaserProperties properties;

	Versions(String bootVersion) {
		this.bootVersion = bootVersion;
		add(SPRING_BOOT_PROJECT_NAME, bootVersion);
		add(BOOT_STARTER_PARENT_ARTIFACT_ID, bootVersion);
		add(BOOT_DEPENDENCIES_ARTIFACT_ID, bootVersion);
		this.properties = new ReleaserProperties();
	}

	Versions(String scBuildVersion, Set<Project> projects) {
		this.scBuildVersion = scBuildVersion;
		add(BUILD_ARTIFACT_ID, scBuildVersion);
		add(CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, scBuildVersion);
		this.projects.addAll(projects);
		this.properties = new ReleaserProperties();
	}

	Versions(String bootVersion, String scBuildVersion, Set<Project> projects) {
		this(new ReleaserProperties(), bootVersion, scBuildVersion, projects);
	}

	Versions(ReleaserProperties properties, String bootVersion, String scBuildVersion, Set<Project> projects) {
		this.properties = properties;
		this.bootVersion = bootVersion;
		this.scBuildVersion = scBuildVersion;
		add(SPRING_BOOT_PROJECT_NAME, bootVersion);
		add(BOOT_STARTER_PARENT_ARTIFACT_ID, bootVersion);
		add(BOOT_DEPENDENCIES_ARTIFACT_ID, bootVersion);
		add(BUILD_ARTIFACT_ID, scBuildVersion);
		add(dependenciesParentArtifactId(), scBuildVersion);
		this.projects.addAll(projects);
	}

	Versions(Set<ProjectVersion> versions, ReleaserProperties properties) {
		this.properties = properties;
		this.bootVersion = versions.stream().filter(projectVersion -> SPRING_BOOT_PROJECT_NAME.equals(projectVersion.projectName))
				.findFirst().orElse(new ProjectVersion(SPRING_BOOT_PROJECT_NAME, "")).version;
		this.scBuildVersion = versions.stream().filter(projectVersion -> BUILD_ARTIFACT_ID.equals(projectVersion.projectName))
				.findFirst().orElse(new ProjectVersion(BUILD_ARTIFACT_ID, "")).version;
		versions.forEach(projectVersion -> setVersion(projectVersion.projectName, projectVersion.version));
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

	String versionForProject(String projectName) {
		return this.projects.stream()
				.filter(project -> nameMatches(projectName, project))
				.findFirst()
				.orElse(Project.EMPTY_PROJECT)
				.version;
	}

	boolean shouldBeUpdated(String projectName) {
		return this.projects.stream()
				.anyMatch(project -> nameMatches(projectName, project));
	}

	boolean shouldSetProperty(Properties properties) {
		return this.projects.stream()
				.anyMatch(project -> properties.containsKey(project.name + ".version"));
	}

	Projects toProjectVersions() {
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
		return !bomArtifactId &&
				(parent || matchesNameWithSuffix(projectName, "-dependencies", project));
	}

	private boolean comparisonOfBomArtifactAndParent(String projectName, Project project) {
		return artifactOrParent(projectName, project.name) || artifactOrParent(project.name, projectName);
	}

	private boolean artifactOrParent(String projectName, String otherProjectName) {
		return projectName.equals(dependenciesArtifactId()) &&
				otherProjectName.equals(dependenciesParentArtifactId());
	}

	private boolean matchesNameWithSuffix(String projectName, String suffix, Project project) {
		boolean containsSuffix = projectName.endsWith(suffix);
		if (!containsSuffix) {
			return false;
		}
		String withoutSuffix = projectName.substring(0, projectName.indexOf(suffix));
		return project.name.equals(withoutSuffix);
	}

	Versions setVersion(String projectName, String version) {
		switch (projectName) {
			case SPRING_BOOT_PROJECT_NAME:
			case BOOT_STARTER_PARENT_ARTIFACT_ID:
			case BOOT_DEPENDENCIES_ARTIFACT_ID:
				updateBootVersions(version);
				break;
			case BUILD_ARTIFACT_ID:
			case CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID:
				updateBuildVersions(version);
				break;
			default:
				if (bomVersionProjectNames().contains(projectName)) {
					updateBomVersions(version);
				} else {
					remove(projectName);
					add(projectName, version);
				}
		}
		return this;
	}

	private List<String> bomVersionProjectNames() {
		List<String> names = new ArrayList<>(this.properties.getMetaRelease()
				.getReleaseTrainDependencyNames());
		names.add(this.properties.getMetaRelease().getReleaseTrainProjectName());
		return names;
	}

	private void updateBuildVersions(String version) {
		this.scBuildVersion = version;
		remove(BUILD_ARTIFACT_ID);
		remove(CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID);
		add(BUILD_ARTIFACT_ID, version);
		add(CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, version);
	}

	private void updateBootVersions(String version) {
		this.bootVersion = version;
		remove(SPRING_BOOT_PROJECT_NAME);
		remove(BOOT_DEPENDENCIES_ARTIFACT_ID);
		remove(BOOT_STARTER_PARENT_ARTIFACT_ID);
		add(SPRING_BOOT_PROJECT_NAME, version);
		add(BOOT_STARTER_PARENT_ARTIFACT_ID, version);
		add(BOOT_DEPENDENCIES_ARTIFACT_ID, version);
	}

	private void updateBomVersions(String version) {
		remove(bomProjectName());
		bomVersionProjectNames().forEach(this::remove);
		add(bomProjectName(), version);
		bomVersionProjectNames().forEach(s -> add(s, version));
	}

	private void add(String key, String value) {
		this.projects.add(new Project(key, value));
	}

	private void remove(String expectedProjectName) {
		this.projects.removeIf(project -> expectedProjectName.equals(project.name));
	}

	@Override public String toString() {
		return "Spring Boot Version=[" + this.bootVersion + ']' + "\nSpring Cloud Build Version=["
				+ this.scBuildVersion + ']' + "\nProjects=\n\t" + this.projects.stream().map(Object::toString).collect(
				Collectors.joining("\n\t"));
	}
}

/**
 * @author Marcin Grzejszczak
 */
class Project {

	static Project EMPTY_PROJECT = new Project("", "");

	final String name;
	final String version;

	Project(String name, String version) {
		this.name = name;
		this.version = version;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Project project = (Project) o;
		if (this.name != null ? !this.name.equals(project.name) : project.name != null)
			return false;
		return this.version != null ?
				this.version.equals(project.version) :
				project.version == null;
	}

	@Override public int hashCode() {
		int result = this.name != null ? this.name.hashCode() : 0;
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		return result;
	}

	@Override public String toString() {
		return "name=[" + this.name + "], version=[" + this.version + ']';
	}
}