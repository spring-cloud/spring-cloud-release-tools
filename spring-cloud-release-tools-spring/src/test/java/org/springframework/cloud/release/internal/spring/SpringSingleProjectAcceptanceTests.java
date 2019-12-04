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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.revwalk.RevCommit;
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
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
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
public class SpringSingleProjectAcceptanceTests extends AbstractSpringAcceptanceTests {

	ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(SpringSingleProjectAcceptanceTests.SingleProjectReleaseConfig.class,
			SpringSingleProjectAcceptanceTests.SingleProjectScanningConfiguration.class);

	@Test
	public void should_fail_to_perform_a_release_of_consul_when_sc_release_contains_snapshots()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release-with-snapshot/", "vCamden.SR5.BROKEN");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		this.runner
				.withSystemProperties("debug=true")
				.withPropertyValues(
						new ArgsBuilder(project, this.tmp)
								.releaseTrainUrl("/projects/spring-cloud-release-with-snapshot/")
								.bomBranch("vCamden.SR5.BROKEN")
								.expectedVersion("1.1.2.RELEASE")
								.build())
				.run(context -> {
							SpringReleaser releaser = context.getBean(SpringReleaser.class);
					BDDAssertions.thenThrownBy(releaser::release).hasMessageContaining(
							"there is at least one SNAPSHOT library version in the Spring Cloud Release project");

						});
	}

	@Test
	public void should_perform_a_release_of_consul() throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		this.runner
				.withSystemProperties("debug=true")
				.withPropertyValues(
					new ArgsBuilder(project, this.tmp)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.bomBranch("vGreenwich.SR2")
						.expectedVersion("2.1.2.RELEASE")
					.build())
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
					then(emailTemplate()).doesNotExist();
					then(blogTemplate()).doesNotExist();
					then(tweetTemplate()).doesNotExist();
					then(releaseNotesTemplate()).doesNotExist();
					// once for updating GA
					// second time to update SNAPSHOT
					BDDMockito.then(saganClient).should(BDDMockito.times(2))
							.updateRelease(BDDMockito.eq("spring-cloud-consul"), BDDMockito.anyList());
					BDDMockito.then(saganClient).should().deleteRelease("spring-cloud-consul", "2.1.2.BUILD-SNAPSHOT");
					then(gitHubHandler.issueCreatedInSpringGuides).isFalse();
					then(gitHubHandler.issueCreatedInStartSpringIo).isFalse();
					then(Files.readSymbolicLink(
							new File(testDocumentationUpdater.getDocumentationRepo(), "spring-cloud-consul/current")
									.toPath())
							.toString()).isEqualTo("2.1.2.RELEASE");
					thenRunUpdatedTestsWereNotCalled(postReleaseActions);
				});
	}

	// issue #74
	@Test
	public void should_perform_a_release_of_sc_build() throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "vGreenwich.SR2");
		File origin = cloneToTemporaryDirectory(this.springCloudBuildProject);
		assertThatClonedBuildProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-build"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		this.runner
				.withSystemProperties("debug=true")
				.withPropertyValues(
						new ArgsBuilder(project, this.tmp)
								.releaseTrainUrl("/projects/spring-cloud-release/")
								.bomBranch("vGreenwich.SR2")
								.projectName("spring-cloud-build")
								.expectedVersion("2.1.6.RELEASE")
								.build())
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
					tagIsPresentInOrigin(origin, "v2.1.6.RELEASE");
					// we're running against camden sc-release
					commitIsPresent(iterator,
							"Bumping versions to 2.1.7.BUILD-SNAPSHOT after release");
					commitIsPresent(iterator, "Going back to snapshots");
					commitIsPresent(iterator, "Update SNAPSHOT to 2.1.6.RELEASE");
					pomVersionIsEqualTo(project, "2.1.7.BUILD-SNAPSHOT");
					pomParentVersionIsEqualTo(project, "spring-cloud-build-dependencies",
							"2.1.6.RELEASE");
					then(gitHubHandler.closedMilestones).isTrue();
					then(emailTemplate()).doesNotExist();
					then(blogTemplate()).doesNotExist();
					then(tweetTemplate()).doesNotExist();
					then(releaseNotesTemplate()).doesNotExist();
					// once for updating GA
					// second time to update SNAPSHOT
					BDDMockito.then(saganClient).should(BDDMockito.times(2))
							.updateRelease(BDDMockito.eq("spring-cloud-build"), BDDMockito.anyList());
					BDDMockito.then(saganClient).should().deleteRelease("spring-cloud-build",
							"2.1.6.BUILD-SNAPSHOT");
					then(gitHubHandler.issueCreatedInSpringGuides).isFalse();
					then(gitHubHandler.issueCreatedInStartSpringIo).isFalse();
					then(Files.readSymbolicLink(
							new File(testDocumentationUpdater.getDocumentationRepo(), "spring-cloud-build/current")
									.toPath())
							.toString()).isEqualTo("2.1.6.RELEASE");
					thenRunUpdatedTestsWereNotCalled(postReleaseActions);
				});
	}

	@Test
	public void should_perform_a_release_of_consul_rc1() throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "vDalston.RC1");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		this.runner
				.withSystemProperties("debug=true")
				.withPropertyValues(
						new ArgsBuilder(project, this.tmp)
								.releaseTrainUrl("/projects/spring-cloud-release/")
								.bomBranch("vDalston.RC1")
								.expectedVersion("1.2.0.RC1")
								.build())
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
					tagIsPresentInOrigin(origin, "v1.2.0.RC1");
					commitIsNotPresent(commits,
							"Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
					Iterator<RevCommit> iterator = listOfCommits(project).iterator();
					commitIsPresent(iterator, "Going back to snapshots");
					commitIsPresent(iterator, "Update SNAPSHOT to 1.2.0.RC1");
					pomVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
					consulPomParentVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
					then(gitHubHandler.closedMilestones).isTrue();
					then(emailTemplate()).doesNotExist();
					then(blogTemplate()).doesNotExist();
					then(tweetTemplate()).doesNotExist();
					then(releaseNotesTemplate()).doesNotExist();
					BDDMockito.then(saganClient).should().updateRelease(
							BDDMockito.eq("spring-cloud-consul"), BDDMockito.anyList());
					BDDMockito.then(saganClient).should().deleteRelease("spring-cloud-consul",
							"1.2.0.M8");
					BDDMockito.then(saganClient).should().deleteRelease("spring-cloud-consul",
							"1.2.0.RC1");
					// we update guides only for SR / RELEASE
					then(gitHubHandler.issueCreatedInSpringGuides).isFalse();
					then(gitHubHandler.issueCreatedInStartSpringIo).isFalse();
					// haven't even checked out the branch
					then(new File(testDocumentationUpdater.getDocumentationRepo(), "current/index.html")).doesNotExist();
					thenRunUpdatedTestsWereNotCalled(postReleaseActions);
				});
	}

	@Test
	public void should_not_clone_when_option_not_to_clone_was_switched_on()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "master");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		final File temporaryDestination = this.tmp.newFolder();

		this.runner
				.withSystemProperties("debug=true")
				.withPropertyValues(
						new ArgsBuilder(project, this.tmp)
								.releaseTrainUrl("/projects/spring-cloud-release/")
								.bomBranch("vCamden.SR5")
								.expectedVersion("1.1.2.RELEASE")
								// just build
								.chosenOption("6")
								.fetchVersionsFromGit(false)
								.cloneDestinationDirectory(temporaryDestination)
								.addFixedVersion("spring-cloud-release",
										"Finchley.RELEASE")
								.addFixedVersion("spring-cloud-consul",
										"2.3.4.RELEASE")
								.build())
				.run(context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);

					releaser.release(new OptionsBuilder()
							.interactive(true)
							.options());

					then(temporaryDestination.list()).isEmpty();
				});
	}

	private void assertThatClonedConsulProjectIsInSnapshots(File origin) {
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
	}

	private void assertThatClonedBuildProjectIsInSnapshots(File origin) {
		pomVersionIsEqualTo(origin, "1.3.7.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "spring-cloud-build-dependencies",
				"1.5.9.RELEASE");
	}

	class ArgsBuilder {

		List<String> args = new LinkedList<>();

		private final File project;

		private final TemporaryFolder tmp;

		ArgsBuilder(File project, TemporaryFolder tmp) throws Exception {
			this.project = project;
			this.tmp = tmp;
			defaults();
		}

		ArgsBuilder defaults() throws Exception {
			// @formatter:off
			this.args.addAll(Arrays.asList(
					"spring.datasource.url=jdbc:h2:mem:" + new Random().nextInt(),
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
					"releaser.git.update-release-train-wiki=true",
					"releaser.git.update-all-test-samples=true",
					"releaser.git.update-documentation-repo=true",
					"releaser.git.update-github-milestones=true",
					"releaser.git.update-release-train-docs=true",
					"releaser.git.all-test-sample-urls[spring-cloud-consul]=" + this.tmp.newFolder().toString(),
					"releaser.sagan.update-sagan=true",
					"releaser.versions.all-versions-file-url="
							+ SpringSingleProjectAcceptanceTests.class.getResource("/raw/initializr.yml").toURI().toString()
			));
			// @formatter:on
			return fetchVersionsFromGit(true)
					.projectName("spring-cloud-consul");
		}

		ArgsBuilder addFixedVersion(String projectName, String projectVersion) throws Exception {
			this.args.add(
					"releaser.fixed-versions[" + projectName + "]=" + projectVersion);
			return this;
		}

		ArgsBuilder fetchVersionsFromGit(boolean fetch) throws Exception {
			this.args.add(
					"releaser.git.fetch-versions-from-git=" + fetch);
			return this;
		}

		ArgsBuilder cloneDestinationDirectory(File cloneDestinationDirectory) throws Exception {
			this.args.add(
					"releaser.git.clone-destination-dir=" + cloneDestinationDirectory.toString());
			return this;
		}

		ArgsBuilder releaseTrainUrl(String relativePath) throws Exception {
			this.args.add(
					"releaser.git.release-train-bom-url=" + file(relativePath).toURI().toString());
			return this;
		}

		ArgsBuilder projectName(String projectName) {
			this.args.add("test.projectName=" + projectName);
			return this;
		}

		ArgsBuilder expectedVersion(String expectedVersion) {
			this.args.add("test.expectedVersion=" + expectedVersion);
			return this;
		}

		ArgsBuilder chosenOption(String chosenOption) {
			this.args.add("test.chosenOption=" + chosenOption);
			return this;
		}

		ArgsBuilder bomBranch(String bomBranch) {
			this.args.add("releaser.pom.branch=" + bomBranch);
			return this;
		}

		String[] build() {
			return args.toArray(new String[0]);
		}
	}

	private String[] springCloudConsulArgs(File project, String bomBranch) throws URISyntaxException, IOException {
		// @formatter:off
		return new String[] {
				};
	}
	// @formatter:on

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


	@Configuration
	@EnableAutoConfiguration
	static class SingleProjectReleaseConfig {

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
		TasksToRunFactory myTasksToRunFactory(ApplicationContext context, @Value("${test.chosenOption:0}") String chosenOption) {
			return new TasksToRunFactory(context) {
				@Override
				String chosenOption() {
					return chosenOption;
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
		NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler(ReleaserProperties releaserProperties, @Value("${test.projectName}") String projectName) {
			return new NonAssertingTestProjectGitHandler(releaserProperties, file -> FileSystemUtils.deleteRecursively(new File(file, projectName)));
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
	static class SingleProjectScanningConfiguration {

	}

}
