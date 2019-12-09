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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.model.Model;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;

import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.TestPomReader;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.sagan.Project;
import org.springframework.cloud.release.internal.sagan.Release;
import org.springframework.cloud.release.internal.spring.meta.SpringMetaReleaseAcceptanceTests;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public abstract class AbstractSpringAcceptanceTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	public File springCloudConsulProject;

	public File springCloudBuildProject;

	public File temporaryFolder;

	public TestPomReader testPomReader = new TestPomReader();

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(AbstractSpringAcceptanceTests.class
				.getResource("/projects/spring-cloud-consul").toURI());
		this.springCloudBuildProject = new File(AbstractSpringAcceptanceTests.class
				.getResource("/projects/spring-cloud-build").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
		clean();
	}

	@After
	public void cleanup() throws Exception {
		clean();
	}

	private void clean() {
		new File("/tmp/executed_build").delete();
		new File("/tmp/executed_deploy").delete();
		new File("/tmp/executed_docs").delete();
		blogTemplate().delete();
		emailTemplate().delete();
		releaseNotesTemplate().delete();
		tweetTemplate().delete();
		buildStatus().delete();
	}

	public void thenRunUpdatedTestsWereCalled(PostReleaseActions postReleaseActions) {
		BDDMockito.then(postReleaseActions).should()
				.runUpdatedTests(BDDMockito.any(Projects.class));
	}

	public void thenRunUpdatedTestsWereNotCalled(PostReleaseActions postReleaseActions) {
		BDDMockito.then(postReleaseActions).should(BDDMockito.never())
				.runUpdatedTests(BDDMockito.any(Projects.class));
	}

	public Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return GitTestUtils.openGitProject(project).log().call();
	}

	public void pomParentVersionIsEqualTo(File project, String child, String expected) {
		then(pom(new File(project, child)).getParent().getVersion()).isEqualTo(expected);
	}

	public void consulPomParentVersionIsEqualTo(File project, String expected) {
		pomParentVersionIsEqualTo(project, "spring-cloud-starter-consul", expected);
	}

	public void pomVersionIsEqualTo(File project, String expected) {
		then(pom(project).getVersion()).isEqualTo(expected);
	}

	public void commitIsPresent(Iterator<RevCommit> iterator, String expected) {
		RevCommit commit = iterator.next();
		then(commit.getShortMessage()).isEqualTo(expected);
	}

	public void commitIsNotPresent(Iterable<RevCommit> commits, String expected) {
		for (RevCommit commit : commits) {
			then(commit.getShortMessage()).isNotEqualTo(expected);
		}
	}

	public void tagIsPresentInOrigin(File origin, String expectedTag)
			throws GitAPIException {
		then(GitTestUtils.openGitProject(origin).tagList().call().iterator().next()
				.getName()).endsWith(expectedTag);
	}

	public Model pom(File dir) {
		return this.testPomReader.readPom(new File(dir, "pom.xml"));
	}

	public File emailTemplate() {
		return new File("target/email.txt");
	}

	public String emailTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(emailTemplate().toPath()));
	}

	public File blogTemplate() {
		return new File("target/blog.md");
	}

	public File buildStatus() {
		return new File("build_status");
	}

	public File tweetTemplate() {
		return new File("target/tweet.txt");
	}

	public File releaseNotesTemplate() {
		return new File("target/notes.md");
	}

	public String blogTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(blogTemplate().toPath()));
	}

	public String tweetTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(tweetTemplate().toPath()));
	}

	public String releaseNotesTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(releaseNotesTemplate().toPath()));
	}

	public File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	public File file(String relativePath) throws URISyntaxException {
		return new File(
				AbstractSpringAcceptanceTests.class.getResource(relativePath).toURI());
	}

	public String text(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	public File cloneToTemporaryDirectory(File springCloudConsulProject)
			throws IOException {
		return GitTestUtils.clonedProject(this.tmp.newFolder(), springCloudConsulProject);
	}

	public void checkoutReleaseTrainBranch(String fileToRepo, String branch)
			throws GitAPIException, URISyntaxException {
		Git git = GitTestUtils.openGitProject(file(fileToRepo));
		git.reset().setMode(ResetCommand.ResetType.HARD).call();
		git.checkout().setName(branch).call();
	}

	public Git clonedProject(NonAssertingTestProjectGitHandler handler, String name) {
		return GitTestUtils.openGitProject(handler.clonedProjects.stream()
				.filter(file -> file.getName().equals(name)).findFirst().get());
	}

	public void thenUpdateReleaseTrainDocsWasCalled(PostReleaseActions actions) {
		BDDMockito.then(actions).should()
				.generateReleaseTrainDocumentation(BDDMockito.any(Projects.class));
	}

	public void thenUpdateReleaseTrainDocsWasNotCalled(PostReleaseActions actions) {
		BDDMockito.then(actions).should(BDDMockito.never())
				.generateReleaseTrainDocumentation(BDDMockito.any(Projects.class));
	}

	public static Project newProject() {
		Project project = new Project();
		project.projectReleases.addAll(Arrays.asList(release("1.0.0.M8"),
				release("1.1.0.M8"), release("1.2.0.M8"), release("2.0.0.M8")));
		return project;
	}

	public static Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}

	public class ArgsBuilder {

		List<String> args = new LinkedList<>();

		private final File project;

		private final TemporaryFolder tmp;

		public ArgsBuilder(File project, TemporaryFolder tmp) throws Exception {
			this.project = project;
			this.tmp = tmp;
			defaults();
		}

		public ArgsBuilder defaults() throws Exception {
			// @formatter:off
			this.args.addAll(Arrays.asList(
					"releaser.git.documentation-url=" + file("/projects/spring-cloud-static-angel/").toURI().toString(),
					"releaser.git.test-samples-project-url=" + this.tmp.newFolder().toString(),
					"releaser.git.release-train-docs-url=" + this.tmp.newFolder().toString(),
					"releaser.maven.build-command=echo build",
					"releaser.maven.deploy-command=echo deploy",
					"releaser.maven.deploy-guides-command=echo guides",
					"releaser.maven.publish-docs-commands=echo docs",
					"releaser.maven.generate-release-train-docs-command=echo releaseTrainDocs",
					"releaser.working-dir=" + project.getPath(),
					"releaser.git.spring-project-url=" + tmpFile("spring-cloud").getAbsolutePath() + "/",
					"releaser.git.release-train-wiki-url=" + tmpFile("spring-cloud-wiki").getAbsolutePath() + "/",
					"releaser.git.run-updated-samples=true",
					"releaser.git.update-spring-guides=true",
					"releaser.git.update-spring-project=true",
					"releaser.git.update-start-spring-io=true",
					"releaser.git.update-all-test-samples=true",
					"releaser.git.update-documentation-repo=true",
					"releaser.git.update-github-milestones=true",
					"releaser.git.update-release-train-docs=true",
					"releaser.git.all-test-sample-urls[spring-cloud-consul]=" + this.tmp.newFolder().toString(),
					"releaser.sagan.update-sagan=true",
					"releaser.template.enabled=true",
					"releaser.versions.all-versions-file-url="
							+ SpringMetaReleaseAcceptanceTests.class.getResource("/raw/initializr.yml").toURI().toString()
			));
			// @formatter:on
			return fetchVersionsFromGit(true).updateReleaseTrainWiki(true)
					.projectName("spring-cloud-consul");
		}

		public ArgsBuilder updateReleaseTrainWiki(boolean enabled) throws Exception {
			this.args.add("releaser.git.update-release-train-wiki=" + enabled);
			return this;
		}

		public ArgsBuilder projectsToSkip(String... toSkip) throws Exception {
			this.args.add(
					"releaser.meta-release.projects-to-skip=" + String.join(",", toSkip));
			return this;
		}

		public ArgsBuilder gitOrgUrl(String url) throws Exception {
			this.args.add("releaser.meta-release.git-org-url=" + url);
			return this;
		}

		public ArgsBuilder mavenBuildCommand(String command) throws Exception {
			this.args.add("releaser.maven.build-command=" + command);
			return this;
		}

		public ArgsBuilder mavenDeployCommand(String command) throws Exception {
			this.args.add("releaser.maven.deploy-command=" + command);
			return this;
		}

		public ArgsBuilder mavenPublishCommand(String command) throws Exception {
			this.args.add("releaser.maven.publish-docs-commands=" + command);
			return this;
		}

		public ArgsBuilder addFixedVersion(String projectName, String projectVersion)
				throws Exception {
			this.args.add(
					"releaser.fixed-versions[" + projectName + "]=" + projectVersion);
			return this;
		}

		public ArgsBuilder addFixedVersions(Map<String, String> versions)
				throws Exception {
			versions.forEach(
					(s, s2) -> this.args.add("releaser.fixed-versions[" + s + "]=" + s2));
			return this;
		}

		public ArgsBuilder releaseTrainBomUrl(String url) throws Exception {
			this.args.add("releaser.git.release-train-bom-url=" + url);
			return this;
		}

		public ArgsBuilder fetchVersionsFromGit(boolean fetch) throws Exception {
			this.args.add("releaser.git.fetch-versions-from-git=" + fetch);
			return this;
		}

		public ArgsBuilder cloneDestinationDirectory(File cloneDestinationDirectory)
				throws Exception {
			this.args.add("releaser.git.clone-destination-dir="
					+ cloneDestinationDirectory.toString());
			return this;
		}

		public ArgsBuilder releaseTrainUrl(String relativePath) throws Exception {
			this.args.add("releaser.git.release-train-bom-url="
					+ file(relativePath).toURI().toString());
			return this;
		}

		public ArgsBuilder projectName(String projectName) {
			this.args.add("test.projectName=" + projectName);
			return this;
		}

		public ArgsBuilder expectedVersion(String expectedVersion) {
			this.args.add("test.expectedVersion=" + expectedVersion);
			return this;
		}

		public ArgsBuilder chosenOption(String chosenOption) {
			this.args.add("test.chosenOption=" + chosenOption);
			return this;
		}

		public ArgsBuilder bomBranch(String bomBranch) {
			this.args.add("releaser.pom.branch=" + bomBranch);
			return this;
		}

		public String[] build() {
			return args.toArray(new String[0]);
		}

	}

	public static class NonAssertingTestProjectGitHandler extends ProjectGitHandler {

		public Set<File> clonedProjects = new HashSet<>();

		private final Consumer<File> docsConsumer;

		public NonAssertingTestProjectGitHandler(ReleaserProperties properties,
				Consumer<File> docsConsumer) {
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

	public static class TestDocumentationUpdater extends DocumentationUpdater {

		private File documentationRepo;

		public TestDocumentationUpdater(ProjectGitHandler gitHandler,
				ReleaserProperties properties, TemplateGenerator templateGenerator,
				List<CustomProjectDocumentationUpdater> updaters) {
			super(gitHandler, properties, templateGenerator, updaters);
		}

		@Override
		public File updateDocsRepo(Projects projects, ProjectVersion currentProject,
				String bomReleaseBranch) {
			File documentationRepo = super.updateDocsRepo(projects, currentProject,
					bomReleaseBranch);
			this.documentationRepo = documentationRepo;
			return documentationRepo;
		}

		@Override
		public File updateDocsRepoForSingleProject(Projects projects,
				ProjectVersion currentProject) {
			File documentationRepo = super.updateDocsRepoForSingleProject(projects,
					currentProject);
			this.documentationRepo = documentationRepo;
			return documentationRepo;
		}

		public File getDocumentationRepo() {
			return documentationRepo;
		}

	}

	public static class TestExecutionResultHandler
			extends SpringBatchExecutionResultHandler {

		public boolean exitedSuccessOrUnstable;

		public boolean exitedWithException;

		public TestExecutionResultHandler(JobExplorer jobExplorer) {
			super(jobExplorer);
		}

		@Override
		void exitSuccessfully() {
			this.exitedSuccessOrUnstable = true;
		}

		@Override
		void exitWithException() {
			this.exitedWithException = true;
		}

	}

	public static class DefaultTestConfiguration {

		@Bean
		SpringBatchFlowRunner mySpringBatchFlowRunner(
				StepBuilderFactory stepBuilderFactory,
				JobBuilderFactory jobBuilderFactory,
				ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher) {
			return new SpringBatchFlowRunner(stepBuilderFactory, jobBuilderFactory,
					projectsToRunFactory, jobLauncher) {
				@Override
				Decision decide(Options options, ReleaserTask task) {
					return Decision.CONTINUE;
				}
			};
		}

		@Bean
		TasksToRunFactory myTasksToRunFactory(ApplicationContext context,
				@Value("${test.chosenOption:0}") String chosenOption) {
			return new TasksToRunFactory(context) {
				@Override
				String chosenOption() {
					return chosenOption;
				}
			};
		}

		@Bean
		TestExecutionResultHandler testExecutionResultHandler(JobExplorer explorer) {
			return new TestExecutionResultHandler(explorer);
		}

	}

}
