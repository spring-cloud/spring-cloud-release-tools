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

package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectDocumentationUpdaterTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File project;

	File tmpFolder;

	ProjectGitHandler handler;

	File clonedDocProject;

	ReleaserProperties properties = new ReleaserProperties();

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.project = new File(ProjectDocumentationUpdaterTests.class
				.getResource("/projects/spring-cloud-static").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.tmpFolder);
		this.properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());
		this.handler = new ProjectGitHandler(this.properties);
		this.clonedDocProject = this.handler.cloneDocumentationProject();
	}

	@Test
	public void should_throw_exception_if_index_html_not_found()
			throws URISyntaxException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"1.3.4.SR10");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationBranch("master");
		properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-release/").toURI().toString());

		BDDAssertions
				.thenThrownBy(() -> new ProjectDocumentationUpdater(properties,
						new ProjectGitHandler(properties))
								.updateDocsRepo(releaseTrainVersion, "vAngel.SR33"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("index.html is not present");
	}

	@Test
	public void should_throw_exception_if_index_found_but_doc_repo_url_missing()
			throws URISyntaxException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"1.3.4.SR10");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());

		BDDAssertions.thenThrownBy(() -> new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)) {
			@Override
			String readIndexHtmlContents(File indexHtml) {
				return "";
			}
		}.updateDocsRepo(releaseTrainVersion, "vAngel.SR33"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("The URL to the documentation repo not found");
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_is_not_ga_or_sr() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"2.0.0.BUILD-SNAPSHOT");
		ReleaserProperties properties = new ReleaserProperties();

		File updatedDocs = new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)).updateDocsRepo(releaseTrainVersion,
						"vAngel.M7");

		then(updatedDocs).isNull();
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_starts_with_v_and_then_lower_letter_than_the_stored_release()
			throws URISyntaxException, IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"1.3.4.SR10");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());

		File updatedDocs = new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)).updateDocsRepo(releaseTrainVersion,
						"vAngel.SR33");

		String indexHtmlContent = new String(
				Files.readAllBytes(new File(updatedDocs, "current/index.html").toPath()));
		then(indexHtmlContent).doesNotContain(
				"https://cloud.spring.io/spring-cloud-static/Angel.SR33/");
	}

	@Test
	public void should_not_commit_if_the_same_version_is_already_there() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"1.3.4.SR10");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		ProjectGitHandler handler = BDDMockito.spy(new ProjectGitHandler(properties));

		new ProjectDocumentationUpdater(properties, handler)
				.updateDocsRepo(releaseTrainVersion, "vDalston.SR3");

		BDDMockito.then(handler).should(BDDMockito.never())
				.commit(BDDMockito.any(File.class), BDDMockito.anyString());
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_starts_with_lower_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"1.3.4.SR10");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		File updatedDocs = new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)).updateDocsRepo(releaseTrainVersion,
						"Angel.SR33");

		String indexHtmlContent = new String(
				Files.readAllBytes(new File(updatedDocs, "current/index.html").toPath()));
		then(indexHtmlContent).doesNotContain(
				"https://cloud.spring.io/spring-cloud-static/Angel.SR33/");
	}

	@Test
	public void should_update_current_version_in_the_docs_if_current_release_starts_with_v_and_then_higher_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"2.0.0.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		File updatedDocs = new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)).updateDocsRepo(releaseTrainVersion,
						"vFinchley.SR33");

		String indexHtmlContent = new String(
				Files.readAllBytes(new File(updatedDocs, "current/index.html").toPath()));
		then(indexHtmlContent)
				.contains("cloud.spring.io/spring-cloud-static/Finchley.SR33/");
	}

	@Test
	public void should_update_current_version_in_the_docs_if_current_release_starts_with_higher_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"2.0.0.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		File updatedDocs = new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)).updateDocsRepo(releaseTrainVersion,
						"Finchley.SR33");

		String indexHtmlContent = new String(
				Files.readAllBytes(new File(updatedDocs, "current/index.html").toPath()));
		then(indexHtmlContent)
				.contains("cloud.spring.io/spring-cloud-static/Finchley.SR33/");
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_switch_is_off() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-sleuth",
				"2.0.0.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		properties.getGit().setUpdateDocumentationRepo(false);

		File updatedDocs = new ProjectDocumentationUpdater(properties,
				new ProjectGitHandler(properties)).updateDocsRepo(releaseTrainVersion,
						"Finchley.SR33");

		then(updatedDocs).isNull();
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(
				ProjectDocumentationUpdaterTests.class.getResource(relativePath).toURI());
	}

}
