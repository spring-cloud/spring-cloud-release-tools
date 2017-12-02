/*
 *  Copyright 2013-2017 the original author or authors.
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.release.internal.git.GitRepoTests;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class PomReaderTests {

	PomReader pomReader = new PomReader();
	File springCloudReleaseProjectPom;
	File springCloudReleaseProject;
	File licenseFile;

	@Before
	public void setup() throws URISyntaxException {
		URI scRelease = GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI();
		this.springCloudReleaseProject = new File(scRelease);
		this.springCloudReleaseProjectPom = new File(scRelease.getPath(), "pom.xml");
		this.licenseFile = new File(scRelease.getPath(), "LICENSE.txt");
	}

	@Test
	public void should_parse_a_valid_pom() {
		Model pom = this.pomReader.readPom(this.springCloudReleaseProjectPom);

		then(pom).isNotNull();
		then(pom.getArtifactId()).isEqualTo("spring-cloud-starter-build");
	}

	@Test
	public void should_parse_a_valid_pom_when_passing_direcory() {
		Model pom = this.pomReader.readPom(this.springCloudReleaseProject);

		then(pom).isNotNull();
		then(pom.getArtifactId()).isEqualTo("spring-cloud-starter-build");
	}

	@Test
	public void should_throw_exception_when_file_is_missing() {
		thenThrownBy(() -> this.pomReader.readPom(new File("foo/bar")))
				.hasMessageStartingWith("Failed to read file: ")
				.hasCauseInstanceOf(IOException.class);
	}

	@Test
	public void should_throw_exception_when_file_is_invalid() {
		thenThrownBy(() -> this.pomReader.readPom(this.licenseFile))
				.hasMessageStartingWith("Failed to read file: ")
				.hasCauseInstanceOf(XmlPullParserException.class);
	}
}