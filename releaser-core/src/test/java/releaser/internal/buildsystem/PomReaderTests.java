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

import java.io.EOFException;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Test;
import releaser.internal.git.GitRepoTests;
import releaser.internal.tech.PomReader;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class PomReaderTests {

	File springCloudReleaseProjectPom;

	File springCloudReleaseProject;

	File empty;

	File licenseFile;

	@Before
	public void setup() throws URISyntaxException {
		URI scRelease = GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI();
		this.springCloudReleaseProject = new File(scRelease);
		this.springCloudReleaseProjectPom = new File(scRelease.getPath(), "pom.xml");
		this.empty = new File(GitRepoTests.class.getResource("/projects/project/empty.xml").toURI());
		this.licenseFile = new File(scRelease.getPath(), "LICENSE.txt");
	}

	@Test
	public void should_parse_a_valid_pom() {
		Model pom = PomReader.readPom(this.springCloudReleaseProjectPom);

		then(pom).isNotNull();
		then(pom.getArtifactId()).isEqualTo("spring-cloud-starter-build");
	}

	@Test
	public void should_parse_a_valid_pom_when_passing_direcory() {
		Model pom = PomReader.readPom(this.springCloudReleaseProject);

		then(pom).isNotNull();
		then(pom.getArtifactId()).isEqualTo("spring-cloud-starter-build");
	}

	@Test
	public void should_return_null_when_file_is_missing() {
		then(PomReader.readPom(new File("foo/bar"))).isNull();
	}

	@Test
	public void should_throw_exception_when_file_is_invalid() {
		thenThrownBy(() -> PomReader.readPom(this.licenseFile)).hasMessageStartingWith("Failed to read file: ")
				.hasCauseInstanceOf(XmlPullParserException.class);
	}

	@Test
	public void should_throw_exception_when_file_is_empty() {
		thenThrownBy(() -> PomReader.readPom(this.empty)).hasMessageStartingWith("File [")
				.hasMessageContaining("] is empty").hasCauseInstanceOf(EOFException.class);
	}

}
