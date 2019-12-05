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

package org.springframework.cloud.release.internal.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.GradleUpdater;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class GradleUpdaterTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_substitute_values_in_gradle_properties() throws IOException {
		File projectRoot = tmpFile("gradleproject");
		ReleaserProperties properties = new ReleaserProperties();
		Map<String, String> props = new HashMap<String, String>() {
			{
				put("foo", "spring-cloud-contract");
				put("bar", "spring-cloud-sleuth");
			}
		};
		properties.getGradle().setGradlePropsSubstitution(props);
		Projects projects = new Projects(
				new ProjectVersion("spring-cloud-contract", "1.0.0"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.0"));

		new GradleUpdater(properties).updateProjectFromReleaseTrain(projectRoot, projects,
				new ProjectVersion("spring-cloud-contract", "1.0.0"), true);

		then(asString(tmpFile("gradleproject/gradle.properties"))).contains("foo=1.0.0");
		then(asString(tmpFile("gradleproject/child/gradle.properties")))
				.contains("bar=2.0.0");
	}

	@Test
	public void should_throw_exception_if_snapshots_remain() {
		File projectRoot = tmpFile("gradleproject");
		ReleaserProperties properties = new ReleaserProperties();
		Map<String, String> props = new HashMap<String, String>() {
			{
				put("foo", "spring-cloud-contract");
				put("bar", "spring-cloud-sleuth");
			}
		};
		properties.getGradle().setGradlePropsSubstitution(props);
		Projects projects = new Projects(
				new ProjectVersion("spring-cloud-contract", "1.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.0"));

		thenThrownBy(() -> new GradleUpdater(properties).updateProjectFromReleaseTrain(
				projectRoot, projects,
				new ProjectVersion("spring-cloud-contract", "1.0.0.RELEASE"), true))
						.hasMessageContaining(
								"(BUILD-)?SNAPSHOT.*$] pattern in line number [1]");
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(GradleUpdaterTests.class.getResource(relativePath).toURI());
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

}
