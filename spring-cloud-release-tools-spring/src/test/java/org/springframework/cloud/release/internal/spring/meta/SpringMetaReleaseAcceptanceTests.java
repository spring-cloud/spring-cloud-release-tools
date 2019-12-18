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

package org.springframework.cloud.release.internal.spring.meta;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.mockito.BDDMockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.spring.ExecutionResult;
import org.springframework.cloud.release.internal.spring.SpringReleaser;
import org.springframework.cloud.release.internal.tasks.release.BuildProjectReleaseTask;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * @author Marcin Grzejszczak
 */
public class SpringMetaReleaseAcceptanceTests
		extends AbstractSpringMetaReleaseAcceptanceTests {

	SpringApplicationBuilder runner = new SpringApplicationBuilder(
			SpringMetaReleaseAcceptanceTests.MetaReleaseConfig.class,
			SpringMetaReleaseAcceptanceTests.MetaReleaseScanningConfiguration.class)
					.web(WebApplicationType.NONE).properties("spring.jmx.enabled=false");

	@Test
	public void should_perform_a_meta_release_of_sc_release_and_consul()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		run(this.runner,
				properties("debug=true").properties("test.metarelease=true")
						.properties(metaReleaseArgs(project).bomBranch("vGreenwich.SR2")
								.addFixedVersions(edgwareSr10()).build()),
				context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler = context
							.getBean(NonAssertingTestProjectGitHandler.class);
					SaganUpdater saganUpdater = context.getBean(SaganUpdater.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					PostReleaseActions postReleaseActions = context
							.getBean(PostReleaseActions.class);
					TestExecutionResultHandler testExecutionResultHandler = context
							.getBean(TestExecutionResultHandler.class);

					ExecutionResult result = releaser
							.release(new OptionsBuilder().metaRelease(true).options());

					then(result.isFailureOrUnstable()).isFalse();
					// consul, release, documentation
					then(nonAssertingTestProjectGitHandler.clonedProjects).hasSize(3);
					// don't want to verify the docs
					thenAllStepsWereExecutedForEachProject(
							nonAssertingTestProjectGitHandler);
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					BDDAssertions
							.then(clonedProject(nonAssertingTestProjectGitHandler,
									"spring-cloud-consul").tagList().call())
							.extracting("name").contains("refs/tags/v5.3.5.RELEASE");
					thenRunUpdatedTestsWereCalled(postReleaseActions);
					thenUpdateReleaseTrainDocsWasCalled(postReleaseActions);

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();
				});
	}

	@Test
	public void should_perform_a_meta_release_dry_run_of_sc_release_and_consul()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		run(this.runner,
				properties("debug=true").properties("test.metarelease=true")
						.properties(metaReleaseArgs(project).bomBranch("vGreenwich.SR2")
								.addFixedVersions(edgwareSr10()).build()),
				context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler = context
							.getBean(NonAssertingTestProjectGitHandler.class);
					SaganUpdater saganUpdater = context.getBean(SaganUpdater.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					PostReleaseActions postReleaseActions = context
							.getBean(PostReleaseActions.class);
					TestExecutionResultHandler testExecutionResultHandler = context
							.getBean(TestExecutionResultHandler.class);

					ExecutionResult result = releaser.release(new OptionsBuilder()
							.metaRelease(true).dryRun(true).options());

					then(result.isFailureOrUnstable()).isFalse();
					// consul, release
					then(nonAssertingTestProjectGitHandler.clonedProjects).hasSize(2);
					// only dry run tasks were called
					thenAllDryRunStepsWereExecutedForEachProject(
							nonAssertingTestProjectGitHandler);
					thenSaganWasNotCalled(saganUpdater);
					thenDocumentationWasNotUpdated(testDocumentationUpdater);
					BDDAssertions
							.then(clonedProject(nonAssertingTestProjectGitHandler,
									"spring-cloud-consul").tagList().call())
							.extracting("name")
							.doesNotContain("refs/tags/v5.3.5.RELEASE");
					thenRunUpdatedTestsWereNotCalled(postReleaseActions);
					thenUpdateReleaseTrainDocsWasNotCalled(postReleaseActions);

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();
				});
	}

	@Test
	public void should_not_release_any_projects_when_they_are_on_list_of_projects_to_skip()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		File temporaryDestination = this.tmp.newFolder();

		run(this.runner,
				properties("debug=true")
						.properties("test.metarelease=true", "test.mockBuild=true")
						.properties(metaReleaseArgs(project).bomBranch("Greenwich")
								.addFixedVersions(consulAndReleaseSnapshots())
								.updateReleaseTrainWiki(false)
								.cloneDestinationDirectory(temporaryDestination)
								.projectsToSkip("spring-cloud-consul").build()),
				context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					BuildProjectReleaseTask build = context
							.getBean(BuildProjectReleaseTask.class);
					TestExecutionResultHandler testExecutionResultHandler = context
							.getBean(TestExecutionResultHandler.class);

					ExecutionResult result = releaser
							.release(new OptionsBuilder().metaRelease(true).options());

					then(result.isFailureOrUnstable()).isFalse();
					thenBuildWasNeverCalledFor(build, "spring-cloud-consul");
					thenBuildWasCalledFor(build, "spring-cloud-release");

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();
				});
	}

	@Test
	public void should_perform_a_meta_release_of_consul_only_when_run_from_got_passed()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		File temporaryDestination = this.tmp.newFolder();

		run(this.runner,
				properties("debug=true")
						.properties("test.metarelease=true", "test.mockBuild=true")
						.properties(metaReleaseArgs(project).bomBranch("Greenwich")
								.addFixedVersions(releaseConsulBuildSnapshots())
								.cloneDestinationDirectory(temporaryDestination).build()),
				context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					BuildProjectReleaseTask build = context
							.getBean(BuildProjectReleaseTask.class);
					SaganUpdater saganUpdater = context.getBean(SaganUpdater.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					TestExecutionResultHandler testExecutionResultHandler = context
							.getBean(TestExecutionResultHandler.class);

					ExecutionResult result = releaser
							.release(new OptionsBuilder().startFrom("spring-cloud-consul")
									.metaRelease(true).options());

					// release
					then(result.isFailureOrUnstable()).isFalse();
					thenBuildWasNeverCalledFor(build, "spring-cloud-build");
					thenBuildWasCalledFor(build, "spring-cloud-consul");
					thenBuildWasCalledFor(build, "spring-cloud-release");

					// post release
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					thenWikiPageWasUpdated(testDocumentationUpdater);

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();
				});
	}

	@Test
	public void should_perform_a_meta_release_of_consul_only_when_task_names_got_passed()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		File temporaryDestination = this.tmp.newFolder();

		run(this.runner,
				properties("debug=true")
						.properties("test.metarelease=true", "test.mockBuild=true")
						.properties(metaReleaseArgs(project).bomBranch("Greenwich")
								.addFixedVersions(releaseConsulBuildSnapshots())
								.cloneDestinationDirectory(temporaryDestination).build()),
				context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					BuildProjectReleaseTask build = context
							.getBean(BuildProjectReleaseTask.class);
					SaganUpdater saganUpdater = context.getBean(SaganUpdater.class);
					TestDocumentationUpdater testDocumentationUpdater = context
							.getBean(TestDocumentationUpdater.class);
					TestExecutionResultHandler testExecutionResultHandler = context
							.getBean(TestExecutionResultHandler.class);

					ExecutionResult result = releaser.release(new OptionsBuilder()
							.taskNames(Collections.singletonList("spring-cloud-consul"))
							.metaRelease(true).options());

					// release
					then(result.isFailureOrUnstable()).isFalse();
					thenBuildWasNeverCalledFor(build, "spring-cloud-release");
					thenBuildWasNeverCalledFor(build, "spring-cloud-build");
					thenBuildWasCalledFor(build, "spring-cloud-consul");

					// post release
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					thenWikiPageWasUpdated(testDocumentationUpdater);

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();
				});
	}

	private void thenWikiPageWasUpdated(DocumentationUpdater documentationUpdater) {
		BDDMockito.then(documentationUpdater).should()
				.updateReleaseTrainWiki(BDDMockito.any(Projects.class));
	}

	private void thenBuildWasCalledFor(BuildProjectReleaseTask build,
			String projectName) {
		BDDMockito.then(build).should().apply(argThat(
				argument -> argument.originalVersion.projectName.equals(projectName)
						|| argument.project.getAbsolutePath().endsWith(projectName)));
	}

	private void thenBuildWasNeverCalledFor(BuildProjectReleaseTask build,
			String projectName) {
		BDDMockito.then(build).should(BDDMockito.never()).apply(argThat(
				argument -> argument.originalVersion.projectName.equals(projectName)
						|| argument.project.getAbsolutePath().endsWith(projectName)));
	}

	private Map<String, String> consulAndReleaseSnapshots() {
		Map<String, String> versions = new LinkedHashMap<>();
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		return versions;
	}

	private Map<String, String> releaseConsulBuildSnapshots() {
		Map<String, String> versions = new LinkedHashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-build", "1.1.2.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		return versions;
	}

	@Configuration
	@ConditionalOnProperty(value = "test.metarelease", havingValue = "true")
	@EnableAutoConfiguration
	static class MetaReleaseConfig extends DefaultTestConfiguration {

		@Bean
		SaganClient testSaganClient() {
			SaganClient saganClient = BDDMockito.mock(SaganClient.class);
			BDDMockito.given(saganClient.getProject(anyString()))
					.willReturn(newProject());
			return saganClient;
		}

		@Bean
		@ConditionalOnProperty(value = "test.mockBuild", havingValue = "true")
		BuildProjectReleaseTask mockedBuildProjectReleaseTask(Releaser releaser) {
			return BDDMockito.spy(new BuildProjectReleaseTask(releaser));
		}

		@Bean
		SaganUpdater testSaganUpdater(SaganClient saganClient,
				ReleaserProperties properties) {
			return BDDMockito.spy(new SaganUpdater(saganClient, properties));
		}

		@Bean
		PostReleaseActions myPostReleaseActions() {
			return BDDMockito.mock(PostReleaseActions.class);
		}

		@Bean
		NonAssertingTestProjectGitHubHandler testProjectGitHubHandler(
				ReleaserProperties releaserProperties) {
			return new NonAssertingTestProjectGitHubHandler(releaserProperties);
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
			return BDDMockito.spy(new TestDocumentationUpdater(projectGitHandler,
					releaserProperties, templateGenerator, updaters));
		}

	}

	@Configuration
	@ConditionalOnProperty(value = "test.metarelease", havingValue = "true",
			matchIfMissing = true)
	@ComponentScan({ "org.springframework.cloud.release.internal",
			"org.springframework.cloud.release.cloud" })
	static class MetaReleaseScanningConfiguration {

	}

}
