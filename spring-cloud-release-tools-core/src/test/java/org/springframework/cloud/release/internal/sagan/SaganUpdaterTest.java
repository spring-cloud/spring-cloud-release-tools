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

package org.springframework.cloud.release.internal.sagan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
public class SaganUpdaterTest {

	SaganClient saganClient = Mockito.mock(SaganClient.class);

	ReleaserProperties properties = new ReleaserProperties();

	SaganUpdater saganUpdater;

	Projects projects = new Projects();

	@Before
	public void setup() {
		Project project = new Project();
		project.projectReleases.addAll(Arrays.asList(release("2.2.0.RC1"),
				release("2.3.0.BUILD-SNAPSHOT"), release("2.2.0.M4")));
		BDDMockito.given(this.saganClient.getProject(anyString())).willReturn(project);
		this.properties.getSagan().setUpdateSagan(true);
		this.saganUpdater = new SaganUpdater(this.saganClient, this.properties);
	}

	private Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}

	@Test
	public void should_not_update_sagan_when_switch_is_off() {
		this.properties.getSagan().setUpdateSagan(false);

		this.saganUpdater.updateSagan(new File("."), "master", version("2.2.0.M1"),
				version("2.2.0.M1"), projects);

		then(this.saganClient).shouldHaveZeroInteractions();
	}

	@Test
	public void should_update_sagan_releases_for_milestone() {
		this.saganUpdater.updateSagan(new File("."), "master", version("2.2.0.M1"),
				version("2.2.0.M1"), projects);

		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("2.2.0.M1",
						"https://cloud.spring.io/spring-cloud-static/foo/{version}/reference/html/",
						"PRERELEASE")));
	}

	@Test
	public void should_update_sagan_releases_for_rc() {
		this.saganUpdater.updateSagan(new File("."), "master", version("2.2.0.RC1"),
				version("2.2.0.RC1"), projects);

		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("2.2.0.RC1",
						"https://cloud.spring.io/spring-cloud-static/foo/{version}/reference/html/",
						"PRERELEASE")));
	}

	@Test
	public void should_not_update_docs_for_sagan_when_current_version_older() {
		given(this.saganClient.updateRelease(BDDMockito.anyString(),
				BDDMockito.anyList())).willReturn(a2_0_0_ReleaseProject());

		this.saganUpdater.updateSagan(new File("."), "master", version("2.2.0.RC1"),
				version("2.2.0.RC1"), projects);

		then(this.saganClient).should(BDDMockito.never())
				.patchProject(BDDMockito.any(Project.class));

	}

	private Project a2_0_0_ReleaseProject() {
		Project project = new Project();
		Release release = new Release();
		release.version = "2.0.0.RELEASE";
		release.current = true;
		project.projectReleases = Collections.singletonList(release);
		return project;
	}

	@Test
	public void should_not_update_docs_for_sagan_when_files_exist_but_content_does_not_differ()
			throws IOException {
		Project project = a2_0_0_ReleaseProject();
		project.rawOverview = "new overview";
		project.rawBootConfig = "new boot";
		given(this.saganClient.updateRelease(BDDMockito.anyString(),
				BDDMockito.anyList())).willReturn(project);

		Path tmp = Files.createTempDirectory("releaser-test");
		createFile(tmp, "sagan-index.adoc", "new overview");
		createFile(tmp, "sagan-boot.adoc", "new boot");
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient, this.properties) {
			@Override
			File docsModule(File projectFile) {
				return tmp.toFile();
			}
		};

		saganUpdater.updateSagan(new File("."), "master", version("3.0.0.RC1"),
				version("3.0.0.RC1"), projects);

		then(this.saganClient).should(BDDMockito.never())
				.patchProject(BDDMockito.any(Project.class));
	}

	@Test
	public void should_update_docs_for_sagan_when_current_version_newer_and_only_overview_adoc_exists()
			throws IOException {
		given(this.saganClient.updateRelease(BDDMockito.anyString(),
				BDDMockito.anyList())).willReturn(a2_0_0_ReleaseProject());

		Path tmp = Files.createTempDirectory("releaser-test");
		createFile(tmp, "sagan-index.adoc", "new text");
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient, this.properties) {
			@Override
			File docsModule(File projectFile) {
				return tmp.toFile();
			}
		};

		saganUpdater.updateSagan(new File("."), "master", version("3.0.0.RC1"),
				version("3.0.0.RC1"), projects);

		then(this.saganClient).should().patchProject(
				BDDMockito.argThat(argument -> "new text".equals(argument.rawOverview)));
	}

	@Test
	public void should_update_docs_for_sagan_when_current_version_newer_and_only_boot_adoc_exists()
			throws IOException {
		given(this.saganClient.updateRelease(BDDMockito.anyString(),
				BDDMockito.anyList())).willReturn(a2_0_0_ReleaseProject());

		Path tmp = Files.createTempDirectory("releaser-test");
		createFile(tmp, "sagan-boot.adoc", "new text");
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient, this.properties) {
			@Override
			File docsModule(File projectFile) {
				return tmp.toFile();
			}
		};

		saganUpdater.updateSagan(new File("."), "master", version("3.0.0.RC1"),
				version("3.0.0.RC1"), projects);

		then(this.saganClient).should().patchProject(BDDMockito
				.argThat(argument -> "new text".equals(argument.rawBootConfig)));
	}

	private void createFile(Path tmp, String filename, String text) throws IOException {
		File overviewAdoc = new File(tmp.toString(), filename);
		overviewAdoc.createNewFile();
		Files.write(overviewAdoc.toPath(), text.getBytes());
	}

	private ProjectVersion version(String version) {
		return new ProjectVersion("foo", version);
	}

	@Test
	public void should_update_sagan_from_master() {
		ProjectVersion projectVersion = version("2.2.0.BUILD-SNAPSHOT");

		this.saganUpdater.updateSagan(new File("."), "master", projectVersion,
				projectVersion, projects);

		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("2.2.0.BUILD-SNAPSHOT",
						"https://cloud.spring.io/foo/reference/html/", "SNAPSHOT")));
	}

	@Test
	public void should_update_sagan_from_release_version() {
		ProjectVersion projectVersion = version("2.2.0.RELEASE");

		this.saganUpdater.updateSagan(new File("."), "master", projectVersion,
				projectVersion, projects);

		then(this.saganClient).should().deleteRelease("foo", "2.2.0.RC1");
		then(this.saganClient).should().deleteRelease("foo", "2.2.0.BUILD-SNAPSHOT");
		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("2.2.0.RELEASE",
						"https://cloud.spring.io/spring-cloud-static/foo/{version}/reference/html/",
						"GENERAL_AVAILABILITY")));
		then(this.saganClient).should().deleteRelease("foo", "2.2.0.BUILD-SNAPSHOT");
		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("2.2.1.BUILD-SNAPSHOT",
						"https://cloud.spring.io/foo/reference/html/", "SNAPSHOT")));
	}

	@Test
	public void should_update_sagan_from_non_master() {
		ProjectVersion projectVersion = version("2.3.0.BUILD-SNAPSHOT");

		this.saganUpdater.updateSagan(new File("."), "2.3.x", projectVersion,
				projectVersion, projects);

		then(this.saganClient).should(never()).deleteRelease(anyString(), anyString());
		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("2.3.0.BUILD-SNAPSHOT",
						"https://cloud.spring.io/foo/2.3.x/reference/html/",
						"SNAPSHOT")));
	}

	private ArgumentMatcher<List<ReleaseUpdate>> withReleaseUpdate(final String version,
			final String refDocUrl, final String releaseStatus) {
		return argument -> {
			ReleaseUpdate item = argument.get(0);
			return "foo".equals(item.artifactId)
					&& releaseStatus.equals(item.releaseStatus)
					&& version.equals(item.version) && refDocUrl.equals(item.apiDocUrl)
					&& refDocUrl.equals(item.refDocUrl) && item.current;
		};
	}

}
