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

package releaser.internal.sagan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.tech.ExecutionResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
public class SaganUpdaterTest {

	SaganClient saganClient = mock(SaganClient.class);

	ReleaserProperties properties = new ReleaserProperties();

	SaganUpdater saganUpdater;

	Projects projects = new Projects();

	private Project project;

	@Before
	public void setup() {
		project = new Project();
		project.setReleases(Arrays.asList(release("2.2.0-RC1"), release("2.3.0-SNAPSHOT"), release("2.2.0-M4")));
		this.properties.getSagan().setUpdateSagan(true);
		this.saganUpdater = new SaganUpdater(this.saganClient, this.properties);
	}

	private Release release(String version) {
		Release release = new Release();
		release.setVersion(version);
		// release.setCurrent(true);
		return release;
	}

	@Test
	public void should_not_update_sagan_when_switch_is_off() {
		this.properties.getSagan().setUpdateSagan(false);

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "main", version("2.2.0-M1"),
				version("2.2.0-M1"), projects);
		assertThat(result.isSkipped()).isTrue();

		then(this.saganClient).shouldHaveNoInteractions();
	}

	@Test
	public void should_update_sagan_releases_for_milestone() {
		given(this.saganClient.addRelease(eq("foo"), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("2.2.0-M1"));

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "main", version("2.2.0-M1"),
				version("2.2.0-M1"), projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should().addRelease(eq("foo"), argThat(withReleaseUpdate("2.2.0-M1",
				"https://cloud.spring.io/spring-cloud-static/foo/{version}/reference/html/")));
	}

	@Test
	public void should_update_sagan_releases_for_rc() {
		given(this.saganClient.addRelease(anyString(), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("2.2.0-RC1"));

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "main", version("2.2.0-RC1"),
				version("2.2.0-RC1"), projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should().addRelease(eq("foo"), argThat(withReleaseUpdate("2.2.0-RC1",
				"https://cloud.spring.io/spring-cloud-static/foo/{version}/reference/html/")));
	}

	@Test
	public void should_not_update_docs_for_sagan_when_current_version_older() {
		given(this.saganClient.addRelease(anyString(), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("2.2.0-RC1"));

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "main", version("2.2.0-RC1"),
				version("2.2.0-RC1"), projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should(never()).patchProject(any(Project.class));

	}

	@Test
	public void should_not_update_docs_for_sagan_when_files_exist_but_content_does_not_differ() throws IOException {
		given(this.saganClient.addRelease(anyString(), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("3.0.0-RC1"));

		Path tmp = Files.createTempDirectory("releaser-test");
		createFile(tmp, "sagan-index.adoc", "new overview");
		createFile(tmp, "sagan-boot.adoc", "new boot");
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient, this.properties) {
			@Override
			File docsModule(File projectFile) {
				return tmp.toFile();
			}
		};

		ExecutionResult result = saganUpdater.updateSagan(new File("."), "main", version("3.0.0-RC1"),
				version("3.0.0-RC1"), projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should(never()).patchProject(any(Project.class));
	}

	@Test
	public void should_update_docs_for_sagan_when_current_version_newer_and_only_overview_adoc_exists()
			throws IOException {
		given(this.saganClient.addRelease(anyString(), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("3.0.0-RC1"));

		Path tmp = Files.createTempDirectory("releaser-test");
		createFile(tmp, "sagan-index.adoc", "new text");
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient, this.properties) {
			@Override
			File docsModule(File projectFile) {
				return tmp.toFile();
			}
		};

		ExecutionResult result = saganUpdater.updateSagan(new File("."), "main", version("3.0.0-RC1"),
				version("3.0.0-RC1"), projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		// FIXME: then(this.saganClient).should().patchProject(argThat(argument -> "new
		// text".equals(argument.rawOverview)));
	}

	@Test
	public void should_update_docs_for_sagan_when_current_version_newer_and_only_boot_adoc_exists() throws IOException {
		given(this.saganClient.addRelease(anyString(), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("3.0.0-RC1"));

		Path tmp = Files.createTempDirectory("releaser-test");
		createFile(tmp, "sagan-boot.adoc", "new text");
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient, this.properties) {
			@Override
			File docsModule(File projectFile) {
				return tmp.toFile();
			}
		};

		ExecutionResult result = saganUpdater.updateSagan(new File("."), "main", version("3.0.0-RC1"),
				version("3.0.0-RC1"), projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		// FIXME: then(this.saganClient).should().patchProject(argThat(argument -> "new
		// text".equals(argument.rawBootConfig)));
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
	public void should_update_sagan_from_main() {
		ProjectVersion projectVersion = version("2.4.0-SNAPSHOT");
		given(this.saganClient.addRelease(anyString(), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("2.4.0-SNAPSHOT"));

		Project updated = new Project();
		updated.setReleases(new ArrayList<>(project.getReleases()));
		updated.getReleases().add(release(projectVersion.version));
		given(this.saganClient.getProject(anyString())).willReturn(updated);

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "main", projectVersion, projectVersion,
				projects);
		// FIXME: assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should().addRelease(eq("foo"),
				argThat(withReleaseUpdate("2.4.0-SNAPSHOT", "https://cloud.spring.io/foo/reference/html/")));
	}

	@Test
	public void should_update_sagan_from_release_version() {
		ProjectVersion projectVersion = version("2.2.0");
		given(this.saganClient.addRelease(eq("foo"), any())).willReturn(true);
		given(this.saganClient.deleteRelease("foo", "2.2.0-RC1")).willReturn(true);
		given(this.saganClient.deleteRelease("foo", "2.2.0-SNAPSHOT")).willReturn(true);
		given(this.saganClient.deleteRelease("foo", "2.2.0-M4")).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("2.2.0"));

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "main", projectVersion, projectVersion,
				projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should().deleteRelease("foo", "2.2.0-RC1");
		then(this.saganClient).should().deleteRelease("foo", "2.2.0-SNAPSHOT");
		then(this.saganClient).should().addRelease(eq("foo"), argThat(withReleaseUpdate("2.2.0",
				"https://cloud.spring.io/spring-cloud-static/foo/{version}/reference/html/")));
		then(this.saganClient).should().deleteRelease("foo", "2.2.0-SNAPSHOT");
		then(this.saganClient).should().addRelease(eq("foo"),
				argThat(withReleaseUpdate("2.2.1-SNAPSHOT", "https://cloud.spring.io/foo/reference/html/")));
	}

	@Test
	public void should_update_sagan_from_non_main() {
		ProjectVersion projectVersion = version("2.3.0-SNAPSHOT");
		given(this.saganClient.addRelease(eq("foo"), any())).willReturn(true);
		given(this.saganClient.getProject("foo")).willReturn(projectWithNewRelease("2.3.0-SNAPSHOT"));

		ExecutionResult result = this.saganUpdater.updateSagan(new File("."), "2.3.x", projectVersion, projectVersion,
				projects);
		assertThat(result.isSkipped()).isFalse();
		assertThat(result.isSuccess()).isTrue();

		then(this.saganClient).should(never()).deleteRelease(anyString(), anyString());
		then(this.saganClient).should().addRelease(eq("foo"),
				argThat(withReleaseUpdate("2.3.0-SNAPSHOT", "https://cloud.spring.io/foo/2.3.x/reference/html/")));
	}

	private Project projectWithNewRelease(String version) {
		Project p = new Project();
		p.setName(project.getName());
		p.setSlug(project.getSlug());
		p.setRepositoryUrl(project.getRepositoryUrl());
		p.setReleases(new ArrayList<>(project.getReleases()));
		Release release = release(version);
		release.setCurrent(true);
		p.getReleases().add(release);
		return p;
	}

	private ArgumentMatcher<ReleaseInput> withReleaseUpdate(final String version, final String refDocUrl) {
		return argument -> {
			ReleaseInput item = argument;
			return version.equals(item.getVersion()) && item.getApiDocUrl() == null
					&& refDocUrl.equals(item.getReferenceDocUrl());
		};
	}

}
