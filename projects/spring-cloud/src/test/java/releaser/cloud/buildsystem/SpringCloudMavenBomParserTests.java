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

package releaser.cloud.buildsystem;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import releaser.cloud.SpringCloudReleaserProperties;
import releaser.cloud.docs.TestUtils;
import releaser.internal.ReleaserProperties;
import releaser.internal.buildsystem.BomParser;
import releaser.internal.buildsystem.MavenBomParserAccessor;
import releaser.internal.buildsystem.VersionsFromBom;

import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class SpringCloudMavenBomParserTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File tmpFolder;

	File springCloudReleaseProject;

	ReleaserProperties properties = SpringCloudReleaserProperties.get();

	@Before
	public void setup() throws URISyntaxException, IOException, GitAPIException {
		this.tmpFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.tmpFolder);
		this.springCloudReleaseProject = new File(this.tmpFolder,
				"/spring-cloud-release");
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(
				SpringCloudMavenBomParserTests.class.getResource(relativePath).toURI());
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_boot() {
		this.properties.getPom().setPomWithBootStarterParent(null);
		this.properties.getPom().setThisTrainBom(null);
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_populate_sc_release_version() {
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		String scReleaseVersion = parser.versionsFromBom(this.springCloudReleaseProject)
				.versionForProject("spring-cloud-release");

		then(scReleaseVersion).isNotBlank();
	}

	@Test
	public void should_populate_boot_version() {
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		String bootVersion = parser.versionsFromBom(this.springCloudReleaseProject)
				.versionForProject("spring-boot");

		then(bootVersion).isNotBlank();
	}

	@Test
	public void should_throw_exception_when_cloud_pom_is_missing() {
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		thenThrownBy(() -> parser.versionsFromBom(new File(".")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_cloud() {
		this.properties.getPom().setPomWithBootStarterParent(null);
		this.properties.getPom().setThisTrainBom(null);
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_cloud_version_is_missing_in_pom() {
		this.properties.getPom().setPomWithBootStarterParent("pom.xml");
		this.properties.getPom().setThisTrainBom("pom.xml");
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(
						"The pom doesn't have a [spring-cloud-dependencies-parent] artifact id");
	}

	@Test
	public void should_populate_cloud_version() {
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		VersionsFromBom cloudVersionsFromBom = parser
				.versionsFromBom(this.springCloudReleaseProject);

		thenAllCloudVersionsSet(cloudVersionsFromBom);
	}

	private void thenAllCloudVersionsSet(VersionsFromBom cloudVersionsFromBom) {
		Arrays.asList("spring-cloud-bus", "spring-cloud-contract",
				"spring-cloud-cloudfoundry", "spring-cloud-commons",
				"spring-cloud-config", "spring-cloud-netflix", "spring-cloud-security",
				"spring-cloud-consul", "spring-cloud-sleuth", "spring-cloud-stream",
				"spring-cloud-task", "spring-cloud-vault", "spring-cloud-zookeeper")
				.forEach(s -> then(cloudVersionsFromBom.versionForProject(s))
						.isNotBlank());
	}

	@Test
	public void should_populate_boot_and_cloud_version() {
		BomParser parser = MavenBomParserAccessor.bomParser(this.properties,
				new SpringCloudMavenBomParser());

		VersionsFromBom cloudVersionsFromBom = parser
				.versionsFromBom(this.springCloudReleaseProject);

		then(cloudVersionsFromBom.versionForProject("spring-boot")).isNotBlank();
		then(cloudVersionsFromBom.versionForProject("spring-cloud-build")).isNotBlank();
		thenAllCloudVersionsSet(cloudVersionsFromBom);
	}

}
