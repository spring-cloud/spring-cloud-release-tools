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

package org.springframework.cloud.release.cloud.docs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import javax.validation.constraints.NotNull;

import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;

import org.springframework.cloud.release.cloud.github.SpringCloudGithubIssuesAccessor;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class SpringCloudCustomProjectDocumentationUpdaterTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File project;

	File tmpFolder;

	ProjectGitHandler handler;

	ProjectGitHubHandler gitHubHandler;

	File clonedDocProject;

	ReleaserProperties properties = new ReleaserProperties();

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.project = new File(SpringCloudCustomProjectDocumentationUpdater.class
				.getResource("/projects/spring-cloud-static").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.tmpFolder);
		this.properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());
		this.handler = new ProjectGitHandler(this.properties);
		this.clonedDocProject = this.handler.cloneDocumentationProject();
		this.gitHubHandler = new ProjectGitHubHandler(this.properties,
				Collections.singletonList(
						SpringCloudGithubIssuesAccessor.springCloud(this.properties)));
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_is_not_ga_or_sr() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Angel.M7");
		ReleaserProperties properties = new ReleaserProperties();

		File updatedDocs = projectDocumentationUpdater(properties)
				.updateDocsRepo(projects(), releaseTrainVersion, "vAngel.M7");

		then(updatedDocs).isNull();
	}

	@NotNull
	private DocumentationUpdater projectDocumentationUpdater(
			ReleaserProperties properties) {
		return new DocumentationUpdater(this.handler, properties,
				templateGenerator(properties),
				Collections.singletonList(
						new SpringCloudCustomProjectDocumentationUpdater(this.handler,
								properties)));
	}

	@NotNull
	private TemplateGenerator templateGenerator(ReleaserProperties properties) {
		return new TemplateGenerator(properties, this.gitHubHandler);
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_starts_with_v_and_then_lower_letter_than_the_stored_release()
			throws URISyntaxException, IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Finchley.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());

		File updatedDocs = new SpringCloudCustomProjectDocumentationUpdater(
				new ProjectGitHandler(properties), properties)
						.updateDocsRepoForReleaseTrain(this.clonedDocProject,
								releaseTrainVersion, projects(), "vFinchley.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath())
				.doesNotExist();
		Path current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString())
				.isEqualTo("Finchley.SR33");

		releaseTrainVersion = new ProjectVersion("spring-cloud-release", "Angel.SR33");
		properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());

		updatedDocs = new SpringCloudCustomProjectDocumentationUpdater(
				new ProjectGitHandler(properties), properties)
						.updateDocsRepoForReleaseTrain(this.clonedDocProject,
								releaseTrainVersion, projects(), "vAngel.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath())
				.doesNotExist();
		current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString())
				.isNotEqualTo("Angel.SR33");
	}

	@Test
	public void should_not_commit_if_the_same_version_is_already_there() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Dalston.SR3");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		ProjectGitHandler handler = BDDMockito.spy(new ProjectGitHandler(properties));

		new SpringCloudCustomProjectDocumentationUpdater(handler, properties)
				.updateDocsRepoForReleaseTrain(this.clonedDocProject, releaseTrainVersion,
						projects(), "vDalston.SR3");

		BDDMockito.then(handler).should(BDDMockito.never())
				.commit(BDDMockito.any(File.class), BDDMockito.anyString());
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_starts_with_lower_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Angel.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		File updatedDocs = new SpringCloudCustomProjectDocumentationUpdater(
				new ProjectGitHandler(properties), properties)
						.updateDocsRepoForReleaseTrain(this.clonedDocProject,
								releaseTrainVersion, projects(), "Angel.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath())
				.doesNotExist();
		Path current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString())
				.isNotEqualTo("Angel.SR33");
	}

	@Test
	public void should_update_current_version_in_the_docs_if_current_release_starts_with_v_and_then_higher_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Finchley.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateDocumentationRepo(true);
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		File updatedDocs = projectDocumentationUpdater(properties)
				.updateDocsRepo(projects(), releaseTrainVersion, "vFinchley.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath())
				.doesNotExist();
		Path current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString())
				.isEqualTo("Finchley.SR33");
	}

	@Test
	public void should_update_current_version_in_the_docs_if_current_release_starts_with_higher_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Finchley.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setUpdateDocumentationRepo(true);
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		DocumentationUpdater updater = projectDocumentationUpdater(properties);
		ProjectVersion sleuthVersion = new ProjectVersion("spring-cloud-sleuth",
				"2.0.0.RELEASE");
		Projects bom = new Projects(sleuthVersion);
		File updatedDocs = updater.updateDocsRepo(bom, releaseTrainVersion,
				"vFinchley.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath())
				.doesNotExist();
		Path current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString())
				.isEqualTo("Finchley.SR33");

		updatedDocs = updater.updateDocsRepoForSingleProject(bom, sleuthVersion);

		BDDAssertions.then(
				new File(updatedDocs, "spring-cloud-sleuth/current/index.html").toPath())
				.doesNotExist();
		current = new File(updatedDocs, "spring-cloud-sleuth/current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString())
				.isEqualTo("2.0.0.RELEASE");
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_switch_is_off() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release",
				"Finchley.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		properties.getGit().setUpdateDocumentationRepo(false);

		File updatedDocs = projectDocumentationUpdater(properties)
				.updateDocsRepo(projects(), releaseTrainVersion, "Finchley.SR33");

		then(updatedDocs).isNull();
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(SpringCloudCustomProjectDocumentationUpdater.class
				.getResource(relativePath).toURI());
	}

	private Projects projects() {
		return new Projects(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RELEASE"));
	}

}
