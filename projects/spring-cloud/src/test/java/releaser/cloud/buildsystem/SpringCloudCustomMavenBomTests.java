/*
 * Copyright 2013-2022 the original author or authors.
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

package releaser.cloud.buildsystem;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import releaser.cloud.SpringCloudReleaserProperties;
import releaser.internal.buildsystem.CustomBomParser;
import releaser.internal.buildsystem.VersionsFromBom;
import releaser.internal.buildsystem.VersionsFromBomBuilder;
import releaser.internal.project.Project;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class SpringCloudCustomMavenBomTests {

	@Test
	public void should_add_boot_to_versions_when_version_is_created() {
		List<CustomBomParser> bomParsers = Collections.singletonList(new SpringCloudMavenBomParser());
		VersionsFromBom customVersionsFromBom = new VersionsFromBomBuilder()
				.releaserProperties(SpringCloudReleaserProperties.get()).parsers(bomParsers)
				.projects(springCloudBuildProjects()).retrieveFromBom();
		customVersionsFromBom.setVersion("spring-boot", "1.2.3.RELEASE");

		then(customVersionsFromBom.versionForProject("spring-boot")).isEqualTo("1.2.3.RELEASE");
		then(customVersionsFromBom.versionForProject("spring-boot-starter-parent")).isEqualTo("1.2.3.RELEASE");
		then(customVersionsFromBom.versionForProject("spring-boot-dependencies")).isEqualTo("1.2.3.RELEASE");
	}

	@Test
	public void should_update_projects_for_boot() {
		VersionsFromBom versionsFromBom = mixedVersions().setVersion("spring-boot", "3.0.0");

		then(versionsFromBom.versionForProject("spring-boot")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-boot-starter-parent")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-boot-dependencies")).isEqualTo("3.0.0");

		versionsFromBom = mixedVersions().setVersion("spring-boot-starter-parent", "3.0.0");

		then(versionsFromBom.versionForProject("spring-boot")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-boot-starter-parent")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-boot-dependencies")).isEqualTo("3.0.0");

		versionsFromBom = mixedVersions().setVersion("spring-boot-dependencies", "3.0.0");

		then(versionsFromBom.versionForProject("spring-boot")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-boot-starter-parent")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-boot-dependencies")).isEqualTo("3.0.0");
	}

	@Test
	public void should_update_projects_for_build() {
		VersionsFromBom versionsFromBom = mixedVersions().setVersion("spring-cloud-build", "3.0.0");

		then(versionsFromBom.versionForProject("spring-cloud-build")).isEqualTo("3.0.0");

		versionsFromBom = mixedVersions().setVersion("spring-cloud-build", "3.0.0");

		then(versionsFromBom.versionForProject("spring-cloud-dependencies-parent")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.RELEASE");

		versionsFromBom = mixedVersions().setVersion("spring-cloud-dependencies-parent", "3.0.0");

		then(versionsFromBom.versionForProject("spring-cloud-build")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies-parent")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.RELEASE");
	}

	private VersionsFromBom mixedVersions() {
		return new VersionsFromBomBuilder().releaserProperties(SpringCloudReleaserProperties.get())
				.parsers(Collections.singletonList(new SpringCloudMavenBomParser())).projects(mixedProjects()).merged();
	}

	Set<Project> springCloudBuildProjects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("spring-cloud-build", "1.2.3.BUILD-SNAPSHOT"));
		return projects;
	}

	Set<Project> mixedProjects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("foo", "1.0.0.BUILD-SNAPSHOT"));
		projects.add(new Project("fooBar", "1.0.0.RELEASE"));
		projects.add(new Project("spring-boot", "1.0.0"));
		projects.add(new Project("spring-cloud-build", "2.0.0"));
		projects.add(new Project("spring-cloud-release", "Greenwich.RELEASE"));
		projects.add(new Project("spring-cloud-dependencies", "Greenwich.RELEASE"));
		projects.add(new Project("spring-cloud-stream-starters", "Fishtown.RELEASE"));
		return projects;
	}

}
