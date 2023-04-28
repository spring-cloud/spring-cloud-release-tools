/*
 * Copyright 2013-2022 the original author or authors.
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

package releaser.cloud.docs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.BDDMockito;
import releaser.cloud.SpringCloudReleaserProperties;
import releaser.cloud.github.SpringCloudGithubIssuesAccessor;
import releaser.internal.ReleaserProperties;
import releaser.internal.docs.DocumentationUpdater;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.template.TemplateGenerator;

import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class SpringCloudCustomProjectDocumentationUpdaterTests {

	File project;

	@TempDir
	File tmpFolder;

	ProjectGitHandler handler;

	ProjectGitHubHandler gitHubHandler;

	File clonedDocProject;

	ReleaserProperties properties = SpringCloudReleaserProperties.get();

	@BeforeEach
	public void setup() throws IOException, URISyntaxException {
		this.project = new File(SpringCloudCustomProjectDocumentationUpdater.class
				.getResource("/projects/spring-cloud-static").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.tmpFolder);
		this.properties.getGit().setDocumentationUrl(file("/projects/spring-cloud-static/").toURI().toString());
		this.handler = new ProjectGitHandler(this.properties);
		this.clonedDocProject = this.handler.cloneDocumentationProject();
		this.gitHubHandler = new ProjectGitHubHandler(this.properties,
				Collections.singletonList(SpringCloudGithubIssuesAccessor.springCloud(this.properties)));
	}

	private DocumentationUpdater projectDocumentationUpdater(ReleaserProperties properties) {
		return new DocumentationUpdater(this.handler, properties, templateGenerator(properties),
				Collections.singletonList(new SpringCloudCustomProjectDocumentationUpdater(this.handler, properties)));
	}

	private TemplateGenerator templateGenerator(ReleaserProperties properties) {
		return new TemplateGenerator(properties, this.gitHubHandler);
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_starts_with_v_and_then_lower_letter_than_the_stored_release()
			throws URISyntaxException, IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release", "Finchley.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(file("/projects/spring-cloud-static/").toURI().toString());

		File updatedDocs = new SpringCloudCustomProjectDocumentationUpdater(new ProjectGitHandler(properties),
				properties).updateDocsRepoForReleaseTrain(this.clonedDocProject, releaseTrainVersion, projects(),
						"vFinchley.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath()).doesNotExist();
		Path current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString()).isEqualTo("Finchley.SR33");

		releaseTrainVersion = new ProjectVersion("spring-cloud-release", "Angel.SR33");
		properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(file("/projects/spring-cloud-static/").toURI().toString());

		updatedDocs = new SpringCloudCustomProjectDocumentationUpdater(new ProjectGitHandler(properties), properties)
				.updateDocsRepoForReleaseTrain(this.clonedDocProject, releaseTrainVersion, projects(), "vAngel.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath()).doesNotExist();
		current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString()).isNotEqualTo("Angel.SR33");
	}

	@Test
	public void should_not_commit_if_the_same_version_is_already_there() {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release", "Dalston.SR3");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		ProjectGitHandler handler = BDDMockito.spy(new ProjectGitHandler(properties));

		new SpringCloudCustomProjectDocumentationUpdater(handler, properties)
				.updateDocsRepoForReleaseTrain(this.clonedDocProject, releaseTrainVersion, projects(), "vDalston.SR3");

		BDDMockito.then(handler).should(BDDMockito.never()).commit(BDDMockito.any(File.class), BDDMockito.anyString());
	}

	@Test
	public void should_do_nothing_when_release_train_docs_update_happen_for_a_project_that_does_not_start_with_spring_cloud() {
		ProjectVersion springBootVersion = new ProjectVersion("spring-boot", "2.2.5");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		ProjectGitHandler handler = BDDMockito.spy(new ProjectGitHandler(properties));

		new SpringCloudCustomProjectDocumentationUpdater(handler, properties)
				.updateDocsRepoForReleaseTrain(this.clonedDocProject, springBootVersion, bootProject(), "vDalston.SR3");

		BDDMockito.then(handler).should(BDDMockito.never()).commit(BDDMockito.any(File.class), BDDMockito.anyString());
	}

	@Test
	public void should_do_nothing_when_single_project_docs_update_happen_for_a_project_that_does_not_start_with_spring_cloud() {
		ProjectVersion springBootVersion = new ProjectVersion("spring-boot", "2.2.5");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());
		ProjectGitHandler handler = BDDMockito.spy(new ProjectGitHandler(properties));

		new SpringCloudCustomProjectDocumentationUpdater(handler, properties)
				.updateDocsRepoForSingleProject(this.clonedDocProject, springBootVersion, bootProject());

		BDDMockito.then(handler).should(BDDMockito.never()).commit(BDDMockito.any(File.class), BDDMockito.anyString());
	}

	@Test
	public void should_not_update_current_version_in_the_docs_if_current_release_starts_with_lower_letter_than_the_stored_release()
			throws IOException {
		ProjectVersion releaseTrainVersion = new ProjectVersion("spring-cloud-release", "Angel.SR33");
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setDocumentationUrl(this.clonedDocProject.toURI().toString());

		File updatedDocs = new SpringCloudCustomProjectDocumentationUpdater(new ProjectGitHandler(properties),
				properties).updateDocsRepoForReleaseTrain(this.clonedDocProject, releaseTrainVersion, projects(),
						"Angel.SR33");

		BDDAssertions.then(new File(updatedDocs, "current/index.html").toPath()).doesNotExist();
		Path current = new File(updatedDocs, "current/").toPath();
		BDDAssertions.then(current).isSymbolicLink();
		BDDAssertions.then(Files.readSymbolicLink(current).toString()).isNotEqualTo("Angel.SR33");
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(SpringCloudCustomProjectDocumentationUpdater.class.getResource(relativePath).toURI());
	}

	private Projects projects() {
		return new Projects(new ProjectVersion("spring-cloud-sleuth", "1.0.0.RELEASE"));
	}

	private Projects bootProject() {
		return new Projects(new ProjectVersion("spring-boot", "2.2.5.RELEASE"));
	}

}
