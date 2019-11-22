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

package org.springframework.cloud.release.internal.versions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.MavenBomParserAccessor;
import org.springframework.cloud.release.internal.buildsystem.ProjectPomUpdater;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.util.FileSystemUtils;

class VersionsFromBomFetcherTests {

	File temporaryFolder;

	@BeforeEach
	void setup() throws IOException, URISyntaxException {
		this.temporaryFolder = Files.createTempDirectory("versions-fetcher").toFile();
		this.temporaryFolder.mkdirs();
		File projects = new File(this.temporaryFolder, "projects");
		projects.mkdirs();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(localFile("/projects"), projects);
	}

	@Test
	void should_return_true_when_current_version_is_the_latest_ga()
			throws URISyntaxException {
		ProjectVersion projectVersion = new ProjectVersion("spring-cloud-contract",
				"2.5.0.RELEASE");
		URI initilizrUri = VersionsFromBomFetcherTests.class
				.getResource("/raw/initializr.yml").toURI();
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateSpringGuides(true);
		properties.getVersions().setAllVersionsFileUrl(initilizrUri.toString());
		properties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release/").toURI().toString() + "/");
		ProjectPomUpdater updater = new ProjectPomUpdater(properties, Collections
				.singletonList(MavenBomParserAccessor.cloudMavenBomParser(properties)));
		VersionsFetcher versionsFetcher = new VersionsFetcher(properties, updater);

		boolean latestGa = versionsFetcher.isLatestGa(projectVersion);

		BDDAssertions.then(latestGa).isTrue();
	}

	@Test
	void should_return_false_when_current_version_is_not_the_latest_ga()
			throws URISyntaxException {
		ProjectVersion projectVersion = new ProjectVersion("spring-cloud-contract",
				"1.0.0.RELEASE");
		URI initilizrUri = VersionsFromBomFetcherTests.class
				.getResource("/raw/initializr.yml").toURI();
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateSpringGuides(true);
		properties.getVersions().setAllVersionsFileUrl(initilizrUri.toString());
		properties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release/").toURI().toString() + "/");
		ProjectPomUpdater updater = new ProjectPomUpdater(properties, Collections
				.singletonList(MavenBomParserAccessor.cloudMavenBomParser(properties)));
		VersionsFetcher versionsFetcher = new VersionsFetcher(properties, updater);

		boolean latestGa = versionsFetcher.isLatestGa(projectVersion);

		BDDAssertions.then(latestGa).isTrue();
	}

	@Test
	void should_return_false_when_current_version_is_not_present_in_the_bom()
			throws URISyntaxException {
		ProjectVersion projectVersion = new ProjectVersion("spring-cloud-non-existant",
				"1.0.0.RELEASE");
		URI initilizrUri = VersionsFromBomFetcherTests.class
				.getResource("/raw/initializr.yml").toURI();
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateSpringGuides(true);
		properties.getVersions().setAllVersionsFileUrl(initilizrUri.toString());
		properties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release/").toURI().toString());
		ProjectPomUpdater updater = new ProjectPomUpdater(properties, new ArrayList<>());
		VersionsFetcher versionsFetcher = new VersionsFetcher(properties, updater);

		boolean latestGa = versionsFetcher.isLatestGa(projectVersion);

		BDDAssertions.then(latestGa).isFalse();
	}

	@Test
	void should_return_false_when_current_version_is_not_ga() {
		ProjectVersion projectVersion = new ProjectVersion("spring-cloud-contract",
				"1.0.0.BUILD-SNAPSHOT");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateSpringGuides(true);
		ProjectPomUpdater updater = new ProjectPomUpdater(properties, new ArrayList<>());
		VersionsFetcher versionsFetcher = new VersionsFetcher(properties, updater);

		boolean latestGa = versionsFetcher.isLatestGa(projectVersion);

		BDDAssertions.then(latestGa).isFalse();
	}

	@Test
	void should_return_false_when_exception_occurs_while_fetching_version_info() {
		ProjectVersion projectVersion = new ProjectVersion("spring-cloud-contract",
				"1.0.0.BUILD-SNAPSHOT");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateSpringGuides(true);
		VersionsFetcher versionsFetcher = new VersionsFetcher(properties,
				new ProjectPomUpdater(properties, new ArrayList<>()) {
					@Override
					public Projects retrieveVersionsFromReleaseTrainBom(String branch,
							boolean updateFixedVersions) {
						throw new IllegalStateException("BOOM!");
					}
				});

		boolean latestGa = versionsFetcher.isLatestGa(projectVersion);

		BDDAssertions.then(latestGa).isFalse();
	}

	private File localFile(String relativePath) throws URISyntaxException {
		return new File(
				VersionsFromBomFetcherTests.class.getResource(relativePath).toURI());
	}

	private File file(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

}
