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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.model.Model;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;

import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.release.cloud.github.SpringCloudGithubIssuesAccessor;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.TestPomReader;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.sagan.Project;
import org.springframework.cloud.release.internal.sagan.Release;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author Marcin Grzejszczak
 */
public class SpringAcceptanceTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File springCloudConsulProject;

	File temporaryFolder;

	TestPomReader testPomReader = new TestPomReader();

	ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(SpringAcceptanceTests.MetaReleaseConfig.class,
			SpringAcceptanceTests.MetaReleaseScanningConfiguration.class);

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(
				AcceptanceTests.class.getResource("/projects/spring-cloud-consul").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
		new File("/tmp/executed_build").delete();
		new File("/tmp/executed_deploy").delete();
		new File("/tmp/executed_docs").delete();
	}

	@Test
	public void should_perform_a_release_of_consul() throws Exception {
		GitTestUtils.openGitProject(file("/projects/spring-cloud-release/")).checkout().setName("Greenwich").call();
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		this.runner
		.withSystemProperties("debug=true")
		// @formatter:off
		.withPropertyValues(
				"releaser.git.release-train-bom-url=" + file("/projects/spring-cloud-release/").toURI().toString(),
				"releaser.git.documentation-url=" + file("/projects/spring-cloud-static-angel/").toURI().toString(),
				"releaser.git.test-samples-project-url=" + this.tmp.newFolder().toString(),
				"releaser.git.release-train-docs-url=" + this.tmp.newFolder().toString(),
				"releaser.maven.build-command=echo build",
				"releaser.maven.deploy-command=echo deploy",
				"releaser.maven.deploy-guides-command=echo guides",
				"releaser.maven.publish-docs-commands=echo docs",
				"releaser.maven.generate-release-train-docs-command=echo releaseTrainDocs",
				"releaser.working-dir=" + project.getPath(),
				"releaser.pom.branch=vGreenwich.SR2",
				"releaser.git.spring-project-url=" + tmpFile("spring-cloud").getAbsolutePath() + "/",
				"releaser.git.release-train-wiki-url=" + tmpFile("spring-cloud-wiki").getAbsolutePath() + "/",
				"releaser.git.run-updated-samples=true",
				"releaser.git.update-spring-guides=true",
				"releaser.git.update-spring-project=true",
				"releaser.git.update-start-spring-io=true",
				"releaser.git.update-release-train-wiki=true",
				"releaser.git.update-all-test-samples=true",
				"releaser.git.update-documentation-repo=true",
				"releaser.git.update-github-milestones=true",
				"releaser.git.update-release-train-docs=true",
				"releaser.git.all-test-sample-urls[spring-cloud-consul]=" + this.tmp.newFolder().toString(),
				"releaser.sagan.update-sagan=true",
				"releaser.versions.all-versions-file-url="
						+ SpringAcceptanceTests.class.getResource("/raw/initializr.yml").toURI().toString(),
				"test.expectedVersion=2.1.2.RELEASE",
				"test.projectName=spring-cloud-consul")
				// @formatter:on
				.run(context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					TestProjectGitHubHandler gitHubHandler = context.getBean(TestProjectGitHubHandler.class);
					SaganClient saganClient = context.getBean(SaganClient.class);
					TestDocumentationUpdater testDocumentationUpdater = context.getBean(TestDocumentationUpdater.class);
					PostReleaseActions postReleaseActions = context.getBean(PostReleaseActions.class);

					releaser.release(new OptionsBuilder()
					.interactive(true)
					.options());

					Iterable<RevCommit> commits = listOfCommits(project);
					Iterator<RevCommit> iterator = commits.iterator();
					tagIsPresentInOrigin(origin, "v2.1.2.RELEASE");
					commitIsPresent(iterator, "Bumping versions to 2.1.3.BUILD-SNAPSHOT after release");
					commitIsPresent(iterator, "Going back to snapshots");
					commitIsPresent(iterator, "Update SNAPSHOT to 2.1.2.RELEASE");
					pomVersionIsEqualTo(project, "2.1.3.BUILD-SNAPSHOT");
					consulPomParentVersionIsEqualTo(project, "2.1.3.BUILD-SNAPSHOT");
					then(gitHubHandler.closedMilestones).isTrue();
					then(emailTemplate()).exists();
					then(emailTemplateContents()).contains("Spring Cloud Greenwich.SR2 available")
							.contains("Spring Cloud Greenwich SR2 Train release");
					then(blogTemplate()).exists();
					then(blogTemplateContents()).contains("I am pleased to announce that the Service Release 2 (SR2)");
					then(releaseNotesTemplate()).exists();
					then(releaseNotesTemplateContents()).contains("Greenwich.SR2").contains(
							"- Spring Cloud Config `2.1.3.RELEASE` ([issues](https://foo.bar.com/2.1.3.RELEASE))")
							.contains(
									"- Spring Cloud Aws `2.1.2.RELEASE` ([issues](https://foo.bar.com/2.1.2.RELEASE))");
					// once for updating GA
					// second time to update SNAPSHOT
					BDDMockito.then(saganClient).should(BDDMockito.times(2))
							.updateRelease(BDDMockito.eq("spring-cloud-consul"), BDDMockito.anyList());
					BDDMockito.then(saganClient).should().deleteRelease("spring-cloud-consul", "2.1.2.BUILD-SNAPSHOT");
					then(gitHubHandler.issueCreatedInSpringGuides).isTrue();
					then(gitHubHandler.issueCreatedInStartSpringIo).isTrue();
					then(Files.readSymbolicLink(
							new File(testDocumentationUpdater.getDocumentationRepo(), "spring-cloud-consul/current")
									.toPath())
							.toString()).endsWith("5.3.5.RELEASE");
					then(Files
							.readSymbolicLink(
									new File(testDocumentationUpdater.getDocumentationRepo(), "current").toPath())
							.toString()).endsWith("Xitmars.SR4");
					thenRunUpdatedTestsWereCalled(postReleaseActions);
				});
	}

	private void thenRunUpdatedTestsWereCalled(PostReleaseActions postReleaseActions) {
		BDDMockito.then(postReleaseActions).should().runUpdatedTests(BDDMockito.any(Projects.class));
	}

	private Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return GitTestUtils.openGitProject(project).log().call();
	}

	private void pomParentVersionIsEqualTo(File project, String child, String expected) {
		then(pom(new File(project, child)).getParent().getVersion()).isEqualTo(expected);
	}

	private void consulPomParentVersionIsEqualTo(File project, String expected) {
		pomParentVersionIsEqualTo(project, "spring-cloud-starter-consul", expected);
	}

	private void pomVersionIsEqualTo(File project, String expected) {
		then(pom(project).getVersion()).isEqualTo(expected);
	}

	private void commitIsPresent(Iterator<RevCommit> iterator, String expected) {
		RevCommit commit = iterator.next();
		then(commit.getShortMessage()).isEqualTo(expected);
	}

	private void commitIsNotPresent(Iterable<RevCommit> commits, String expected) {
		for (RevCommit commit : commits) {
			then(commit.getShortMessage()).isNotEqualTo(expected);
		}
	}

	private void tagIsPresentInOrigin(File origin, String expectedTag) throws GitAPIException {
		then(GitTestUtils.openGitProject(origin).tagList().call().iterator().next().getName()).endsWith(expectedTag);
	}

	private Model pom(File dir) {
		return this.testPomReader.readPom(new File(dir, "pom.xml"));
	}

	private File emailTemplate() {
		return new File("target/email.txt");
	}

	private String emailTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(emailTemplate().toPath()));
	}

	private File blogTemplate() {
		return new File("target/blog.md");
	}

	private File tweetTemplate() {
		return new File("target/tweet.txt");
	}

	private File releaseNotesTemplate() {
		return new File("target/notes.md");
	}

	private String blogTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(blogTemplate().toPath()));
	}

	private String tweetTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(tweetTemplate().toPath()));
	}

	private String releaseNotesTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(releaseNotesTemplate().toPath()));
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(SpringAcceptanceTests.class.getResource(relativePath).toURI());
	}

	private String text(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	static class TestProjectGitHubHandler extends ProjectGitHubHandler {

		final String expectedVersion;

		final String projectName;

		boolean closedMilestones = false;

		boolean issueCreatedInSpringGuides = false;

		boolean issueCreatedInStartSpringIo = false;

		TestProjectGitHubHandler(ReleaserProperties properties, String expectedVersion, String projectName) {
			super(properties, Collections.singletonList(SpringCloudGithubIssuesAccessor.springCloud(properties)));
			this.expectedVersion = expectedVersion;
			this.projectName = projectName;
		}

		@Override
		public void closeMilestone(ProjectVersion releaseVersion) {
			then(releaseVersion.projectName).isEqualTo(this.projectName);
			then(releaseVersion.version).isEqualTo(this.expectedVersion);
			this.closedMilestones = true;
		}

		@Override
		public void createIssueInSpringGuides(Projects projects, ProjectVersion version) {
			this.issueCreatedInSpringGuides = true;
		}

		@Override
		public void createIssueInStartSpringIo(Projects projects, ProjectVersion version) {
			this.issueCreatedInStartSpringIo = true;
		}

		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "https://foo.bar.com/" + releaseVersion.toString();
		}

	}

	class NonAssertingTestProjectGitHubHandler extends ProjectGitHubHandler {

		boolean closedMilestones = false;

		boolean issueCreatedInSpringGuides = false;

		boolean issueCreatedInStartSpringIo = false;

		NonAssertingTestProjectGitHubHandler(ReleaserProperties properties) {
			super(properties, Collections.singletonList(SpringCloudGithubIssuesAccessor.springCloud(properties)));
		}

		@Override
		public void closeMilestone(ProjectVersion releaseVersion) {
			this.closedMilestones = true;
		}

		@Override
		public void createIssueInSpringGuides(Projects projects, ProjectVersion version) {
			this.issueCreatedInSpringGuides = true;
		}

		@Override
		public void createIssueInStartSpringIo(Projects projects, ProjectVersion version) {
			this.issueCreatedInStartSpringIo = true;
		}

		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "https://foo.bar.com/" + releaseVersion.toString();
		}

	}

	static class NonAssertingTestProjectGitHandler extends ProjectGitHandler {

		List<File> clonedProjects = new ArrayList<>();

		NonAssertingTestProjectGitHandler(ReleaserProperties properties) {
			super(properties);
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

		File getDocumentationRepo() {
			return documentationRepo;
		}

	}

	@Configuration
	@EnableAutoConfiguration
	static class MetaReleaseConfig {

		@Bean
		SpringBatchFlowRunner mySpringBatchFlowRunner(StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher) {
			return new SpringBatchFlowRunner(stepBuilderFactory, jobBuilderFactory, projectsToRunFactory, jobLauncher) {
				@Override
				Decision decide(Options options, ReleaserTask task) {
					return Decision.CONTINUE;
				}
			};
		}

		@Bean
		TasksToRunFactory myTasksToRunFactory(ApplicationContext context) {
			return new TasksToRunFactory(context) {
				@Override
				String chosenOption() {
					return "0";
				}
			};
		}

		@Bean
		SaganClient testSaganClient() {
			SaganClient saganClient = BDDMockito.mock(SaganClient.class);
			BDDMockito.given(saganClient.getProject(anyString())).willReturn(newProject());
			return saganClient;
		}

		@Bean
		PostReleaseActions myPostReleaseActions() {
			return BDDMockito.mock(PostReleaseActions.class);
		}

		@Bean
		TestProjectGitHubHandler testProjectGitHubHandler(ReleaserProperties releaserProperties,
				@Value("${test.expectedVersion}") String expectedVersion,
				@Value("${test.projectName}") String projectName) {
			return new TestProjectGitHubHandler(releaserProperties, expectedVersion, projectName);
		}

		@Bean
		NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler(ReleaserProperties releaserProperties) {
			return new NonAssertingTestProjectGitHandler(releaserProperties);
		}

		@Bean
		TestDocumentationUpdater testDocumentationUpdater(ProjectGitHandler projectGitHandler,
				ReleaserProperties releaserProperties, TemplateGenerator templateGenerator, @Autowired(required = false)
				List<CustomProjectDocumentationUpdater> updaters) {
			return new TestDocumentationUpdater(projectGitHandler, releaserProperties, templateGenerator, updaters);
		}

	}

	@Configuration
	@ComponentScan({ "org.springframework.cloud.release.internal", "org.springframework.cloud.release.cloud", })
	static class MetaReleaseScanningConfiguration {

	}

	private static Project newProject() {
		Project project = new Project();
		project.projectReleases.addAll(
				Arrays.asList(release("1.0.0.M8"), release("1.1.0.M8"), release("1.2.0.M8"), release("2.0.0.M8")));
		return project;
	}

	private static Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}

}
