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

package org.springframework.cloud.release.internal.spring.single;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.mockito.BDDMockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.release.cloud.github.SpringCloudGithubIssuesAccessor;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.spring.AbstractSpringAcceptanceTests;
import org.springframework.cloud.release.internal.spring.SpringReleaser;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
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
			.withUserConfiguration(
					SpringSingleProjectAcceptanceTests.SingleProjectReleaseConfig.class,
					SpringSingleProjectAcceptanceTests.SingleProjectScanningConfiguration.class);

	@Test
	public void should_fail_to_perform_a_release_of_consul_when_sc_release_contains_snapshots()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release-with-snapshot/",
				"vCamden.SR5.BROKEN");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		this.runner.withSystemProperties("debug=true")
				.withPropertyValues(new ArgsBuilder(project, this.tmp)
						.releaseTrainUrl("/projects/spring-cloud-release-with-snapshot/")
						.bomBranch("vCamden.SR5.BROKEN").expectedVersion("1.1.2.RELEASE")
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

		this.runner.withSystemProperties("debug=true")
				.withPropertyValues(new ArgsBuilder(project, this.tmp)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.bomBranch("vGreenwich.SR2").expectedVersion("2.1.2.RELEASE")
						.build())
				.run(context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					TestProjectGitHubHandler gitHubHandler = context
							.getBean(TestProjectGitHubHandler.class);
					SaganClient saganClient = context.getBean(SaganClient.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					PostReleaseActions postReleaseActions = context
							.getBean(PostReleaseActions.class);

					releaser.release(new OptionsBuilder().interactive(true).options());

					Iterable<RevCommit> commits = listOfCommits(project);
					Iterator<RevCommit> iterator = commits.iterator();
					tagIsPresentInOrigin(origin, "v2.1.2.RELEASE");
					commitIsPresent(iterator,
							"Bumping versions to 2.1.3.BUILD-SNAPSHOT after release");
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
							.updateRelease(BDDMockito.eq("spring-cloud-consul"),
									BDDMockito.anyList());
					BDDMockito.then(saganClient).should()
							.deleteRelease("spring-cloud-consul", "2.1.2.BUILD-SNAPSHOT");
					then(gitHubHandler.issueCreatedInSpringGuides).isFalse();
					then(gitHubHandler.issueCreatedInStartSpringIo).isFalse();
					then(Files.readSymbolicLink(
							new File(testDocumentationUpdater.getDocumentationRepo(),
									"spring-cloud-consul/current").toPath())
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

		this.runner.withSystemProperties("debug=true")
				.withPropertyValues(new ArgsBuilder(project, this.tmp)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.bomBranch("vGreenwich.SR2").projectName("spring-cloud-build")
						.expectedVersion("2.1.6.RELEASE").build())
				.run(context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					TestProjectGitHubHandler gitHubHandler = context
							.getBean(TestProjectGitHubHandler.class);
					SaganClient saganClient = context.getBean(SaganClient.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					PostReleaseActions postReleaseActions = context
							.getBean(PostReleaseActions.class);

					releaser.release(new OptionsBuilder().interactive(true).options());

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
							.updateRelease(BDDMockito.eq("spring-cloud-build"),
									BDDMockito.anyList());
					BDDMockito.then(saganClient).should()
							.deleteRelease("spring-cloud-build", "2.1.6.BUILD-SNAPSHOT");
					then(gitHubHandler.issueCreatedInSpringGuides).isFalse();
					then(gitHubHandler.issueCreatedInStartSpringIo).isFalse();
					then(Files.readSymbolicLink(
							new File(testDocumentationUpdater.getDocumentationRepo(),
									"spring-cloud-build/current").toPath())
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

		this.runner.withSystemProperties("debug=true")
				.withPropertyValues(new ArgsBuilder(project, this.tmp)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.bomBranch("vDalston.RC1").expectedVersion("1.2.0.RC1").build())
				.run(context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					TestProjectGitHubHandler gitHubHandler = context
							.getBean(TestProjectGitHubHandler.class);
					SaganClient saganClient = context.getBean(SaganClient.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					PostReleaseActions postReleaseActions = context
							.getBean(PostReleaseActions.class);

					releaser.release(new OptionsBuilder().interactive(true).options());

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
					BDDMockito.then(saganClient).should()
							.deleteRelease("spring-cloud-consul", "1.2.0.M8");
					BDDMockito.then(saganClient).should()
							.deleteRelease("spring-cloud-consul", "1.2.0.RC1");
					// we update guides only for SR / RELEASE
					then(gitHubHandler.issueCreatedInSpringGuides).isFalse();
					then(gitHubHandler.issueCreatedInStartSpringIo).isFalse();
					// haven't even checked out the branch
					then(new File(testDocumentationUpdater.getDocumentationRepo(),
							"current/index.html")).doesNotExist();
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

		this.runner.withSystemProperties("debug=true")
				.withPropertyValues(new ArgsBuilder(project, this.tmp)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.bomBranch("vCamden.SR5").expectedVersion("1.1.2.RELEASE")
						// just build
						.chosenOption("6").fetchVersionsFromGit(false)
						.cloneDestinationDirectory(temporaryDestination)
						.addFixedVersion("spring-cloud-release", "Finchley.RELEASE")
						.addFixedVersion("spring-cloud-consul", "2.3.4.RELEASE").build())
				.run(context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);

					releaser.release(new OptionsBuilder().interactive(true).options());

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

	private String[] springCloudConsulArgs(File project, String bomBranch)
			throws URISyntaxException, IOException {
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

		TestProjectGitHubHandler(ReleaserProperties properties, String expectedVersion,
				String projectName) {
			super(properties, Collections.singletonList(
					SpringCloudGithubIssuesAccessor.springCloud(properties)));
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
		public void createIssueInStartSpringIo(Projects projects,
				ProjectVersion version) {
			this.issueCreatedInStartSpringIo = true;
		}

		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "https://foo.bar.com/" + releaseVersion.toString();
		}

	}

	@Configuration
	@EnableAutoConfiguration
	@ConditionalOnProperty(value = "test.metarelease", havingValue = "false",
			matchIfMissing = true)
	static class SingleProjectReleaseConfig extends DefaultTestConfiguration {

		@Bean
		SaganClient testSaganClient() {
			SaganClient saganClient = BDDMockito.mock(SaganClient.class);
			BDDMockito.given(saganClient.getProject(anyString()))
					.willReturn(newProject());
			return saganClient;
		}

		@Bean
		PostReleaseActions myPostReleaseActions() {
			return BDDMockito.mock(PostReleaseActions.class);
		}

		@Bean
		TestProjectGitHubHandler testProjectGitHubHandler(
				ReleaserProperties releaserProperties,
				@Value("${test.expectedVersion}") String expectedVersion,
				@Value("${test.projectName}") String projectName) {
			return new TestProjectGitHubHandler(releaserProperties, expectedVersion,
					projectName);
		}

		@Bean
		NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler(
				ReleaserProperties releaserProperties,
				@Value("${test.projectName}") String projectName) {
			return new NonAssertingTestProjectGitHandler(releaserProperties,
					file -> FileSystemUtils
							.deleteRecursively(new File(file, projectName)));
		}

		@Bean
		TestDocumentationUpdater testDocumentationUpdater(
				ProjectGitHandler projectGitHandler,
				ReleaserProperties releaserProperties,
				TemplateGenerator templateGenerator, @Autowired(
						required = false) List<CustomProjectDocumentationUpdater> updaters) {
			return new TestDocumentationUpdater(projectGitHandler, releaserProperties,
					templateGenerator, updaters);
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "test.metarelease", havingValue = "false",
			matchIfMissing = true)
	@ComponentScan({ "org.springframework.cloud.release.internal",
			"org.springframework.cloud.release.cloud" })
	static class SingleProjectScanningConfiguration {

	}

}
