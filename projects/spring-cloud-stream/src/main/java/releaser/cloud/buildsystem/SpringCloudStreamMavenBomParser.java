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

package releaser.cloud.buildsystem;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.buildsystem.CustomBomParser;
import releaser.internal.buildsystem.VersionsFromBom;
import releaser.internal.buildsystem.VersionsFromBomBuilder;
import releaser.internal.project.Project;
import releaser.internal.tech.PomReader;

import org.springframework.util.StringUtils;

import static releaser.cloud.buildsystem.SpringCloudBomConstants.BOOT_DEPENDENCIES_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.BOOT_STARTER_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.BOOT_STARTER_PARENT_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.BUILD_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.CLOUD_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.CLOUD_DEPENDENCIES_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.CLOUD_RELEASE_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.CLOUD_STARTER_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.CLOUD_STARTER_PARENT_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.SPRING_BOOT;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.STREAM_DEPS_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.STREAM_DOCS_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.STREAM_STARTER_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.STREAM_STARTER_BUILD_ARTIFACT_ID;
import static releaser.cloud.buildsystem.SpringCloudBomConstants.STREAM_STARTER_PARENT_ARTIFACT_ID;

class SpringCloudStreamMavenBomParser implements CustomBomParser {

	private static final Logger log = LoggerFactory.getLogger(SpringCloudStreamMavenBomParser.class);

	@Override
	public VersionsFromBom parseBom(File root, ReleaserProperties properties) {
		VersionsFromBom springCloudBuild = springCloudBuild(root, properties);
		VersionsFromBom boot = bootVersion(root, properties);
		log.debug("Added Spring Cloud Build [{}] and boot versions [{}]", springCloudBuild, boot);
		return new VersionsFromBomBuilder().thisProjectRoot(root).releaserProperties(properties)
				.projects(springCloudBuild, boot).merged();
	}

	private VersionsFromBom springCloudBuild(File root, ReleaserProperties properties) {
		String buildVersion = buildVersion(root, properties);
		if (StringUtils.isEmpty(buildVersion)) {
			return VersionsFromBom.EMPTY_VERSION;
		}
		VersionsFromBom scBuild = new VersionsFromBomBuilder().thisProjectRoot(root).releaserProperties(properties)
				.merged();
		scBuild.add(BUILD_ARTIFACT_ID, buildVersion);
		scBuild.add(CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, buildVersion);
		return scBuild;
	}

	private String buildVersion(File root, ReleaserProperties properties) {
		String buildVersion = properties.getFixedVersions().get(BUILD_ARTIFACT_ID);
		if (StringUtils.hasText(buildVersion)) {
			return buildVersion;
		}
		File pom = new File(root, properties.getPom().getThisTrainBom());
		if (!pom.exists()) {
			return "";
		}
		Model model = PomReader.pom(root, properties.getPom().getThisTrainBom());
		String buildArtifact = model.getParent().getArtifactId();
		log.debug("[{}] artifact id is equal to [{}]", CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, buildArtifact);
		if (!CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID.equals(buildArtifact)) {
			throw new IllegalStateException("The pom doesn't have a [spring-cloud-dependencies-parent] artifact id");
		}
		buildVersion = model.getParent().getVersion();
		log.debug("Spring Cloud Build version is equal to [{}]", buildVersion);
		return buildVersion;
	}

	private String boot(File root, ReleaserProperties properties) {
		String bootVersion = properties.getFixedVersions().get(SPRING_BOOT);
		if (StringUtils.hasText(bootVersion)) {
			return bootVersion;
		}
		String pomWithBootStarterParent = properties.getPom().getPomWithBootStarterParent();
		File pom = new File(root, pomWithBootStarterParent);
		if (!pom.exists()) {
			return "";
		}
		Model model = PomReader.pom(root, pomWithBootStarterParent);
		if (model == null) {
			return "";
		}
		String bootArtifactId = model.getParent().getArtifactId();
		log.debug("Boot artifact id is equal to [{}]", bootArtifactId);
		if (!BOOT_STARTER_PARENT_ARTIFACT_ID.equals(bootArtifactId)) {
			if (log.isDebugEnabled()) {
				throw new IllegalStateException(
						"The pom doesn't have a [" + BOOT_STARTER_PARENT_ARTIFACT_ID + "] artifact id");
			}
			return "";
		}
		return model.getParent().getVersion();
	}

	VersionsFromBom bootVersion(File root, ReleaserProperties properties) {
		String bootVersion = boot(root, properties);
		if (StringUtils.isEmpty(bootVersion)) {
			return VersionsFromBom.EMPTY_VERSION;
		}
		log.debug("Boot version is equal to [{}]", bootVersion);
		VersionsFromBom versionsFromBom = new VersionsFromBomBuilder().thisProjectRoot(root)
				.releaserProperties(properties).merged();
		versionsFromBom.add(SPRING_BOOT, bootVersion);
		versionsFromBom.add(BOOT_STARTER_PARENT_ARTIFACT_ID, bootVersion);
		versionsFromBom.add(BOOT_DEPENDENCIES_ARTIFACT_ID, bootVersion);
		return versionsFromBom;
	}

	@Override
	public Set<Project> setVersion(Set<Project> projects, String projectName, String version) {
		Set<Project> newProjects = new LinkedHashSet<>(projects);
		switch (projectName) {
		case SPRING_BOOT:
		case BOOT_STARTER_ARTIFACT_ID:
		case BOOT_STARTER_PARENT_ARTIFACT_ID:
		case BOOT_DEPENDENCIES_ARTIFACT_ID:
			updateBootVersions(newProjects, version);
			break;
		case BUILD_ARTIFACT_ID:
		case CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID:
			updateBuildVersions(newProjects, version);
			break;
		case CLOUD_ARTIFACT_ID:
		case CLOUD_DEPENDENCIES_ARTIFACT_ID:
		case CLOUD_RELEASE_ARTIFACT_ID:
		case CLOUD_STARTER_ARTIFACT_ID:
		case CLOUD_STARTER_PARENT_ARTIFACT_ID:
			updateSpringCloudVersions(newProjects, version);
			break;
		case STREAM_DEPS_ARTIFACT_ID:
		case STREAM_STARTER_ARTIFACT_ID:
		case STREAM_STARTER_BUILD_ARTIFACT_ID:
		case STREAM_STARTER_PARENT_ARTIFACT_ID:
		case STREAM_DOCS_ARTIFACT_ID:
			updateStreamVersions(newProjects, version);
			break;
		}
		return newProjects;
	}

	private void updateBootVersions(Set<Project> newProjects, String version) {
		remove(newProjects, SPRING_BOOT);
		remove(newProjects, BOOT_STARTER_ARTIFACT_ID);
		remove(newProjects, BOOT_STARTER_PARENT_ARTIFACT_ID);
		remove(newProjects, BOOT_DEPENDENCIES_ARTIFACT_ID);
		add(newProjects, SPRING_BOOT, version);
		add(newProjects, BOOT_STARTER_ARTIFACT_ID, version);
		add(newProjects, BOOT_STARTER_PARENT_ARTIFACT_ID, version);
		add(newProjects, BOOT_DEPENDENCIES_ARTIFACT_ID, version);
	}

	private void updateBuildVersions(Set<Project> newProjects, String version) {
		remove(newProjects, BUILD_ARTIFACT_ID);
		remove(newProjects, CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID);
		add(newProjects, BUILD_ARTIFACT_ID, version);
		add(newProjects, CLOUD_DEPENDENCIES_PARENT_ARTIFACT_ID, version);
	}

	private void updateSpringCloudVersions(Set<Project> newProjects, String version) {
		remove(newProjects, CLOUD_DEPENDENCIES_ARTIFACT_ID);
		remove(newProjects, CLOUD_ARTIFACT_ID);
		remove(newProjects, CLOUD_RELEASE_ARTIFACT_ID);
		remove(newProjects, CLOUD_STARTER_ARTIFACT_ID);
		remove(newProjects, CLOUD_STARTER_PARENT_ARTIFACT_ID);
		add(newProjects, CLOUD_DEPENDENCIES_ARTIFACT_ID, version);
		add(newProjects, CLOUD_ARTIFACT_ID, version);
		add(newProjects, CLOUD_RELEASE_ARTIFACT_ID, version);
		add(newProjects, CLOUD_STARTER_ARTIFACT_ID, version);
		add(newProjects, CLOUD_STARTER_PARENT_ARTIFACT_ID, version);
	}

	private void updateStreamVersions(Set<Project> newProjects, String version) {
		remove(newProjects, STREAM_DEPS_ARTIFACT_ID);
		remove(newProjects, STREAM_STARTER_ARTIFACT_ID);
		remove(newProjects, STREAM_STARTER_BUILD_ARTIFACT_ID);
		remove(newProjects, STREAM_STARTER_PARENT_ARTIFACT_ID);
		add(newProjects, STREAM_DEPS_ARTIFACT_ID, version);
		add(newProjects, STREAM_STARTER_ARTIFACT_ID, version);
		add(newProjects, STREAM_STARTER_BUILD_ARTIFACT_ID, version);
		add(newProjects, STREAM_STARTER_PARENT_ARTIFACT_ID, version);
	}

	private void add(Set<Project> projects, String key, String value) {
		projects.add(new Project(key, value));
	}

	private void remove(Set<Project> projects, String expectedProjectName) {
		projects.removeIf(project -> expectedProjectName.equals(project.name));
	}

}
