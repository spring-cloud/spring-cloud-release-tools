/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.cloud.buildsystem;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.CustomBomParser;
import org.springframework.cloud.release.internal.buildsystem.PomReader;
import org.springframework.cloud.release.internal.buildsystem.Project;
import org.springframework.cloud.release.internal.buildsystem.VersionsFromBom;
import org.springframework.cloud.release.internal.buildsystem.VersionsFromBomBuilder;

import static org.springframework.cloud.release.cloud.buildsystem.SpringCloudBomConstants.BOOT_DEPENDENCIES_ARTIFACT_ID;
import static org.springframework.cloud.release.cloud.buildsystem.SpringCloudBomConstants.BOOT_STARTER_PARENT_ARTIFACT_ID;
import static org.springframework.cloud.release.cloud.buildsystem.SpringCloudBomConstants.BUILD_ARTIFACT_ID;
import static org.springframework.cloud.release.cloud.buildsystem.SpringCloudBomConstants.CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID;
import static org.springframework.cloud.release.cloud.buildsystem.SpringCloudBomConstants.SPRING_BOOT;

public class SpringCloudMavenBomParser implements CustomBomParser {

	private static final Logger log = LoggerFactory
			.getLogger(SpringCloudMavenBomParser.class);

	@Override
	public boolean isApplicable(File root, ReleaserProperties properties,
			Set<Project> projects) {
		return isMaven(root) && root.getName().startsWith("spring-cloud") || projects
				.stream().anyMatch(project -> BUILD_ARTIFACT_ID.equals(project.name));
	}

	@Override
	public VersionsFromBom parseBom(File root, ReleaserProperties properties) {
		VersionsFromBom springCloudBuild = springCloudBuild(root, properties);
		VersionsFromBom boot = bootVersion(root, properties);
		return new VersionsFromBomBuilder().releaserProperties(properties)
				.projects(springCloudBuild, boot).versionsFromBom();
	}

	private VersionsFromBom springCloudBuild(File root, ReleaserProperties properties) {
		Model model = PomReader.pom(root, properties.getPom().getThisTrainBom());
		String buildArtifact = model.getParent().getArtifactId();
		log.debug("[{}] artifact id is equal to [{}]",
				CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, buildArtifact);
		if (!CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID.equals(buildArtifact)) {
			throw new IllegalStateException(
					"The pom doesn't have a [spring-cloud-dependencies-parent] artifact id");
		}
		String buildVersion = model.getParent().getVersion();
		log.debug("Spring Cloud Build version is equal to [{}]", buildVersion);
		VersionsFromBom scBuild = new VersionsFromBomBuilder()
				.releaserProperties(properties).versionsFromBom();
		scBuild.add(BUILD_ARTIFACT_ID, buildVersion);
		scBuild.add(CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, buildVersion);
		return scBuild;
	}

	VersionsFromBom bootVersion(File root, ReleaserProperties properties) {
		String pomWithBootStarterParent = properties.getPom()
				.getPomWithBootStarterParent();
		Model model = PomReader.pom(root, pomWithBootStarterParent);
		if (model == null) {
			return VersionsFromBom.EMPTY_VERSION;
		}
		String bootArtifactId = model.getParent().getArtifactId();
		log.debug("Boot artifact id is equal to [{}]", bootArtifactId);
		if (!SpringCloudBomConstants.BOOT_STARTER_PARENT_ARTIFACT_ID.equals(bootArtifactId)) {
			if (log.isDebugEnabled()) {
				throw new IllegalStateException("The pom doesn't have a ["
						+ SpringCloudBomConstants.BOOT_STARTER_PARENT_ARTIFACT_ID + "] artifact id");
			}
			return VersionsFromBom.EMPTY_VERSION;
		}
		String bootVersion = model.getParent().getVersion();
		log.debug("Boot version is equal to [{}]", bootVersion);
		VersionsFromBom versionsFromBom = new VersionsFromBomBuilder()
				.releaserProperties(properties).versionsFromBom();
		versionsFromBom.add(SPRING_BOOT, bootVersion);
		versionsFromBom.add(BOOT_STARTER_PARENT_ARTIFACT_ID, bootVersion);
		versionsFromBom.add(BOOT_DEPENDENCIES_ARTIFACT_ID, bootVersion);
		return versionsFromBom;
	}

	@Override
	public Set<Project> setVersion(Set<Project> projects, String projectName,
			String version) {
		Set<Project> newProjects = new LinkedHashSet<>(projects);
		switch (projectName) {
		case SPRING_BOOT:
		case BOOT_STARTER_PARENT_ARTIFACT_ID:
		case BOOT_DEPENDENCIES_ARTIFACT_ID:
			updateBootVersions(newProjects, version);
			break;
		case BUILD_ARTIFACT_ID:
		case CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID:
			updateBuildVersions(newProjects, version);
			break;
		}
		return newProjects;
	}

	private void updateBootVersions(Set<Project> newProjects, String version) {
		remove(newProjects, SPRING_BOOT);
		remove(newProjects, BOOT_DEPENDENCIES_ARTIFACT_ID);
		remove(newProjects, BOOT_STARTER_PARENT_ARTIFACT_ID);
		add(newProjects, SPRING_BOOT, version);
		add(newProjects, BOOT_STARTER_PARENT_ARTIFACT_ID, version);
		add(newProjects, BOOT_DEPENDENCIES_ARTIFACT_ID, version);
	}

	private void updateBuildVersions(Set<Project> newProjects, String version) {
		remove(newProjects, BUILD_ARTIFACT_ID);
		remove(newProjects, CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID);
		add(newProjects, BUILD_ARTIFACT_ID, version);
		add(newProjects, CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, version);
	}

	private void add(Set<Project> projects, String key, String value) {
		projects.add(new Project(key, value));
	}

	private void remove(Set<Project> projects, String expectedProjectName) {
		projects.removeIf(project -> expectedProjectName.equals(project.name));
	}

}
