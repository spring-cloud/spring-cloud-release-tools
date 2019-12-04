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

package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.TestPomReader;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.sagan.Project;
import org.springframework.cloud.release.internal.sagan.Release;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public abstract class AbstractSpringAcceptanceTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File springCloudConsulProject;

	File springCloudBuildProject;

	File temporaryFolder;

	TestPomReader testPomReader = new TestPomReader();

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(
				AcceptanceTests.class.getResource("/projects/spring-cloud-consul").toURI());
		this.springCloudBuildProject = new File(
				AcceptanceTests.class.getResource("/projects/spring-cloud-build").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
		new File("/tmp/executed_build").delete();
		new File("/tmp/executed_deploy").delete();
		new File("/tmp/executed_docs").delete();
		blogTemplate().delete();
		emailTemplate().delete();
		releaseNotesTemplate().delete();
		tweetTemplate().delete();
	}

	void thenRunUpdatedTestsWereCalled(PostReleaseActions postReleaseActions) {
		BDDMockito.then(postReleaseActions).should().runUpdatedTests(BDDMockito.any(Projects.class));
	}

	void thenRunUpdatedTestsWereNotCalled(PostReleaseActions postReleaseActions) {
		BDDMockito.then(postReleaseActions).should(BDDMockito.never()).runUpdatedTests(BDDMockito.any(Projects.class));
	}

	Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return GitTestUtils.openGitProject(project).log().call();
	}

	void pomParentVersionIsEqualTo(File project, String child, String expected) {
		then(pom(new File(project, child)).getParent().getVersion()).isEqualTo(expected);
	}

	void consulPomParentVersionIsEqualTo(File project, String expected) {
		pomParentVersionIsEqualTo(project, "spring-cloud-starter-consul", expected);
	}

	void pomVersionIsEqualTo(File project, String expected) {
		then(pom(project).getVersion()).isEqualTo(expected);
	}

	void commitIsPresent(Iterator<RevCommit> iterator, String expected) {
		RevCommit commit = iterator.next();
		then(commit.getShortMessage()).isEqualTo(expected);
	}

	void commitIsNotPresent(Iterable<RevCommit> commits, String expected) {
		for (RevCommit commit : commits) {
			then(commit.getShortMessage()).isNotEqualTo(expected);
		}
	}

	void tagIsPresentInOrigin(File origin, String expectedTag) throws GitAPIException {
		then(GitTestUtils.openGitProject(origin).tagList().call().iterator().next().getName()).endsWith(expectedTag);
	}

	Model pom(File dir) {
		return this.testPomReader.readPom(new File(dir, "pom.xml"));
	}

	File emailTemplate() {
		return new File("target/email.txt");
	}

	String emailTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(emailTemplate().toPath()));
	}

	File blogTemplate() {
		return new File("target/blog.md");
	}

	File tweetTemplate() {
		return new File("target/tweet.txt");
	}

	File releaseNotesTemplate() {
		return new File("target/notes.md");
	}

	String blogTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(blogTemplate().toPath()));
	}

	String tweetTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(tweetTemplate().toPath()));
	}

	String releaseNotesTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(releaseNotesTemplate().toPath()));
	}

	File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	File file(String relativePath) throws URISyntaxException {
		return new File(AbstractSpringAcceptanceTests.class.getResource(relativePath).toURI());
	}

	String text(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	File cloneToTemporaryDirectory(File springCloudConsulProject) throws IOException {
		return GitTestUtils.clonedProject(this.tmp.newFolder(), springCloudConsulProject);
	}

	void checkoutReleaseTrainBranch(String fileToRepo, String branch) throws GitAPIException, URISyntaxException {
		GitTestUtils.openGitProject(file(fileToRepo)).checkout().setName(branch).call();
	}

	static class NonAssertingTestProjectGitHandler extends ProjectGitHandler {

		List<File> clonedProjects = new ArrayList<>();

		private final Consumer<File> docsConsumer;

		NonAssertingTestProjectGitHandler(ReleaserProperties properties, Consumer<File> docsConsumer) {
			super(properties);
			this.docsConsumer = docsConsumer;
		}

		@Override
		public File cloneReleaseTrainProject() {
			File file = super.cloneReleaseTrainProject();
			this.clonedProjects.add(file);
			return file;
		}

		@Override
		public File cloneDocumentationProject() {
			File file = super.cloneDocumentationProject();
			this.clonedProjects.add(file);
			this.docsConsumer.accept(file);
			return file;
		}

		@Override
		public File cloneSpringDocProject() {
			File file = super.cloneSpringDocProject();
			this.clonedProjects.add(file);
			return file;
		}

		@Override
		public File cloneReleaseTrainDocumentationProject() {
			File file = super.cloneReleaseTrainDocumentationProject();
			this.clonedProjects.add(file);
			return file;
		}

		@Override
		public File cloneProjectFromOrg(String projectName) {
			File file = super.cloneProjectFromOrg(projectName);
			this.clonedProjects.add(file);
			return file;
		}

	}

	static class TestDocumentationUpdater extends DocumentationUpdater {

		private File documentationRepo;

		public TestDocumentationUpdater(ProjectGitHandler gitHandler, ReleaserProperties properties,
				TemplateGenerator templateGenerator, List<CustomProjectDocumentationUpdater> updaters) {
			super(gitHandler, properties, templateGenerator, updaters);
		}

		@Override
		public File updateDocsRepo(Projects projects, ProjectVersion currentProject, String bomReleaseBranch) {
			File documentationRepo = super.updateDocsRepo(projects, currentProject, bomReleaseBranch);
			this.documentationRepo = documentationRepo;
			return documentationRepo;
		}

		@Override
		public File updateDocsRepoForSingleProject(Projects projects, ProjectVersion currentProject) {
			File documentationRepo = super.updateDocsRepoForSingleProject(projects, currentProject);
			this.documentationRepo = documentationRepo;
			return documentationRepo;
		}

		File getDocumentationRepo() {
			return documentationRepo;
		}

	}

	static Project newProject() {
		Project project = new Project();
		project.projectReleases.addAll(
				Arrays.asList(release("1.0.0.M8"), release("1.1.0.M8"), release("1.2.0.M8"), release("2.0.0.M8")));
		return project;
	}

	static Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}
}
