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
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import releaser.SpringCloudReleaserProperties;
import releaser.internal.ReleaserProperties;
import releaser.internal.git.GitRepoTests;

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
	public void should_throw_exception_when_null_is_passed_to_boot() {
		this.properties.getPom().setPomWithBootStarterParent(null);
		this.properties.getPom().setThisTrainBom(null);
		BomParser parser = new MavenBomParser(this.properties, Collections.emptyList());

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_populate_sc_release_version() {
		BomParser parser = new MavenBomParser(this.properties, Collections.emptyList());

		String scReleaseVersion = parser.versionsFromBom(this.springCloudReleaseProject)
				.versionForProject("spring-cloud-release");

		then(scReleaseVersion).isEqualTo("Dalston.BUILD-SNAPSHOT");
	}

	@Test
	public void should_throw_exception_when_cloud_pom_is_missing() {
		BomParser parser = new MavenBomParser(this.properties, Collections.emptyList());

		thenThrownBy(() -> parser.versionsFromBom(new File("."))).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_cloud() {
		this.properties.getPom().setPomWithBootStarterParent(null);
		this.properties.getPom().setThisTrainBom(null);
		BomParser parser = new MavenBomParser(this.properties, Collections.emptyList());

		thenThrownBy(() -> parser.versionsFromBom(this.springCloudReleaseProject))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("Pom is not present");
	}

}
