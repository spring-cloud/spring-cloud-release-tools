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

package org.springframework.cloud.release.internal.buildsystem;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.cloud.release.SpringCloudReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.GitRepoTests;
import org.springframework.cloud.release.internal.project.Project;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class MavenBomParserTests {

	File springCloudReleaseProject;

	ReleaserProperties properties = SpringCloudReleaserProperties.get();

	@Before
	public void setup() throws URISyntaxException {
		this.springCloudReleaseProject = new File(
				GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI());
	}

	@Test
	@Ignore("flakey")
	public void should_throw_exception_when_boot_pom_is_missing() {
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);
		File file = new File(".");

		thenThrownBy(() -> parser.versionsFromBom(file))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_boot() {
		this.properties.getPom().setPomWithBootStarterParent(null);
		this.properties.getPom().setThisTrainBom(null);
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	@Ignore("flakey")
	public void should_throw_exception_when_boot_version_is_missing_in_pom() {
		this.properties.getPom().setPomWithBootStarterParent("pom.xml");
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(
						"The pom doesn't have a [spring-boot-starter-parent] artifact id");
	}

	@Test
	public void should_populate_sc_release_version() {
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		String scReleaseVersion = parser.versionsFromBom(this.springCloudReleaseProject)
				.versionForProject("spring-cloud-release");

		then(scReleaseVersion).isEqualTo("Dalston.BUILD-SNAPSHOT");
	}

	@Test
	public void should_populate_boot_version() {
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		String bootVersion = parser.versionsFromBom(this.springCloudReleaseProject)
				.versionForProject("spring-boot");

		then(bootVersion).isEqualTo("1.5.1.BUILD-SNAPSHOT");
	}

	@Test
	public void should_throw_exception_when_cloud_pom_is_missing() {
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		thenThrownBy(() -> parser.versionsFromBom(new File(".")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_cloud() {
		this.properties.getPom().setPomWithBootStarterParent(null);
		this.properties.getPom().setThisTrainBom(null);
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_cloud_version_is_missing_in_pom() {
		this.properties.getPom().setPomWithBootStarterParent("pom.xml");
		this.properties.getPom().setThisTrainBom("pom.xml");
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(
						"The pom doesn't have a [spring-cloud-dependencies-parent] artifact id");
	}

	@Test
	public void should_populate_cloud_version() {
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		VersionsFromBom cloudVersionsFromBom = parser
				.versionsFromBom(this.springCloudReleaseProject);

		then(cloudVersionsFromBom.versionForProject("spring-cloud-build"))
				.isEqualTo("1.3.1.BUILD-SNAPSHOT");
		then(cloudVersionsFromBom.projects).contains(allProjects());
	}

	@Test
	public void should_populate_boot_and_cloud_version() {
		BomParser parser = MavenBomParserAccessor.cloudMavenBomParser(this.properties);

		VersionsFromBom cloudVersionsFromBom = parser
				.versionsFromBom(this.springCloudReleaseProject);

		then(cloudVersionsFromBom.versionForProject("spring-boot"))
				.isEqualTo("1.5.1.BUILD-SNAPSHOT");
		then(cloudVersionsFromBom.versionForProject("spring-cloud-build"))
				.isEqualTo("1.3.1.BUILD-SNAPSHOT");
		then(cloudVersionsFromBom.projects).contains(allProjects());
	}

	private Project[] allProjects() {
		return new Project[] { project("spring-cloud-aws", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-bus", "1.3.0.BUILD-SNAPSHOT"),
				project("spring-cloud-contract", "1.1.0.BUILD-SNAPSHOT"),
				project("spring-cloud-cloudfoundry", "1.1.0.BUILD-SNAPSHOT"),
				project("spring-cloud-commons", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-config", "1.3.0.BUILD-SNAPSHOT"),
				project("spring-cloud-netflix", "1.3.0.BUILD-SNAPSHOT"),
				project("spring-cloud-security", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-consul", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-sleuth", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-stream", "Chelsea.BUILD-SNAPSHOT"),
				project("spring-cloud-task", "1.1.2.BUILD-SNAPSHOT"),
				project("spring-cloud-vault", "1.0.0.BUILD-SNAPSHOT"),
				project("spring-cloud-zookeeper", "1.1.0.BUILD-SNAPSHOT") };
	}

	Project project(String name, String value) {
		return new Project(name, value);
	}

}
