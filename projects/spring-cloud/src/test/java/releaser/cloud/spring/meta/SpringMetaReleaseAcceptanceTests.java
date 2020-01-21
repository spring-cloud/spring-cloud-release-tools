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

package releaser.cloud.spring.meta;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.BDDMockito;
import releaser.internal.Releaser;
import releaser.internal.ReleaserProperties;
import releaser.internal.docs.CustomProjectDocumentationUpdater;
import releaser.internal.docs.DocumentationUpdater;
import releaser.internal.git.GitTestUtils;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.options.OptionsBuilder;
import releaser.internal.postrelease.PostReleaseActions;
import releaser.internal.project.Projects;
import releaser.internal.sagan.SaganClient;
import releaser.internal.sagan.SaganUpdater;
import releaser.internal.spring.Arguments;
import releaser.internal.spring.SpringReleaser;
import releaser.internal.tasks.ReleaseReleaserTask;
import releaser.internal.tasks.composite.MetaReleaseCompositeTask;
import releaser.internal.tasks.composite.ReleaseCompositeTask;
import releaser.internal.tasks.release.BuildProjectReleaseTask;
import releaser.internal.tech.BuildUnstableException;
import releaser.internal.tech.ExecutionResult;
import releaser.internal.template.TemplateGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
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
		extends AbstractSpringCloudMetaAcceptanceTests {

	@Test
	public void should_perform_a_meta_release_of_sc_release_and_consul()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		run(defaultRunner(),
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

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();

					then(result.isFailureOrUnstable()).isFalse();
					// consul, release, documentation
					then(nonAssertingTestProjectGitHandler.clonedProjects).hasSize(3);
					// don't want to verify the docs
					thenAllStepsWereExecutedForEachProject(
							nonAssertingTestProjectGitHandler);
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					then(clonedProject(nonAssertingTestProjectGitHandler,
							"spring-cloud-consul").tagList().call()).extracting("name")
									.contains("refs/tags/v5.3.5.RELEASE");
					thenRunUpdatedTestsWereCalled(postReleaseActions);
					thenUpdateReleaseTrainDocsWasCalled(postReleaseActions);
				});
	}

	@Test
	public void should_perform_a_meta_release_of_sc_release_and_consul_in_parallel()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		run(defaultRunner(), properties("debug=true").properties("test.metarelease=true")
				.properties(metaReleaseArgsForParallel(project)
						.bomBranch("vGreenwich.SR2").addFixedVersions(edgwareSr10())
						.metaReleaseGroups("example1,example2",
								"spring-cloud-build,spring-cloud-consul,spring-cloud-release")
						.build()),
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

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();

					then(result.isFailureOrUnstable()).isFalse();
					// TODO: Assert the steps
					// build, consul, release, documentation
					// then(nonAssertingTestProjectGitHandler.clonedProjects).hasSize(4);
					// don't want to verify the docs
					// thenAllStepsWereExecutedForEachProject(
					// nonAssertingTestProjectGitHandler);
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					then(clonedProject(nonAssertingTestProjectGitHandler,
							"spring-cloud-consul").tagList().call()).extracting("name")
									.contains("refs/tags/v5.3.5.RELEASE");
					thenRunUpdatedTestsWereCalled(postReleaseActions);
					thenUpdateReleaseTrainDocsWasCalled(postReleaseActions);
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

		run(defaultRunner(),
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

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();

					then(result.isFailureOrUnstable()).isFalse();
					// consul, release
					then(nonAssertingTestProjectGitHandler.clonedProjects).hasSize(2);
					// only dry run tasks were called
					thenAllDryRunStepsWereExecutedForEachProject(
							nonAssertingTestProjectGitHandler);
					thenSaganWasNotCalled(saganUpdater);
					thenDocumentationWasNotUpdated(testDocumentationUpdater);
					then(clonedProject(nonAssertingTestProjectGitHandler,
							"spring-cloud-consul").tagList().call()).extracting("name")
									.doesNotContain("refs/tags/v5.3.5.RELEASE");
					thenRunUpdatedTestsWereNotCalled(postReleaseActions);
					thenUpdateReleaseTrainDocsWasNotCalled(postReleaseActions);
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

		run(defaultRunner(),
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

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();

					then(result.isFailureOrUnstable()).isFalse();
					thenBuildWasNeverCalledFor(build, "spring-cloud-consul");
					thenBuildWasCalledFor(build, "spring-cloud-release");
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

		run(defaultRunner(),
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

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();

					// release
					then(result.isFailureOrUnstable()).isFalse();
					thenBuildWasNeverCalledFor(build, "spring-cloud-build");
					thenBuildWasCalledFor(build, "spring-cloud-consul");
					thenBuildWasCalledFor(build, "spring-cloud-release");

					// post release
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					thenWikiPageWasUpdated(testDocumentationUpdater);
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

		run(defaultRunner(),
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

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedSuccessOrUnstable).isTrue();

					// release
					then(result.isFailureOrUnstable()).isFalse();
					thenBuildWasNeverCalledFor(build, "spring-cloud-release");
					thenBuildWasNeverCalledFor(build, "spring-cloud-build");
					thenBuildWasCalledFor(build, "spring-cloud-consul");

					// post release
					thenSaganWasCalled(saganUpdater);
					thenDocumentationWasUpdated(testDocumentationUpdater);
					thenWikiPageWasUpdated(testDocumentationUpdater);
				});
	}

	@Test
	public void should_not_execute_any_subsequent_task_when_first_one_fails()
			throws Exception {
		checkoutReleaseTrainBranch("/projects/spring-cloud-release/", "Greenwich");
		File origin = cloneToTemporaryDirectory(this.springCloudConsulProject);
		assertThatClonedConsulProjectIsInSnapshots(origin);
		File project = cloneToTemporaryDirectory(tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);

		run(failingBuildRunner(), properties("debug=true")
				.properties("test.metarelease=true", "test.metarelease.failing=true",
						"releaser.flow.default-enabled=false")
				.properties(metaReleaseArgs(project).bomBranch("vGreenwich.SR2")
						.addFixedVersions(edgwareSr10()).build()),
				context -> {
					SpringReleaser releaser = context.getBean(SpringReleaser.class);
					NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler = context
							.getBean(NonAssertingTestProjectGitHandler.class);
					TestExecutionResultHandler testExecutionResultHandler = context
							.getBean(TestExecutionResultHandler.class);
					FirstTask firstTask = context.getBean(FirstTask.class);
					SecondTask secondTask = context.getBean(SecondTask.class);

					ExecutionResult result = releaser
							.release(new OptionsBuilder().metaRelease(true).options());

					// print results
					testExecutionResultHandler.accept(result);
					then(testExecutionResultHandler.exitedWithException).isTrue();

					then(result.isFailureOrUnstable()).isTrue();
					// consul
					then(nonAssertingTestProjectGitHandler.clonedProjects).hasSize(1);
					BDDMockito.then(firstTask).should().runTask(BDDMockito.argThat(
							arg -> arg.project.getName().equals("spring-cloud-consul")));
					BDDMockito.then(firstTask).should(BDDMockito.never())
							.runTask(BDDMockito.argThat(arg -> arg.project.getName()
									.equals("spring-cloud-release")));
					BDDMockito.then(secondTask).should(BDDMockito.never())
							.runTask(BDDMockito.any(Arguments.class));
				});
	}

	private SpringApplicationBuilder defaultRunner() {
		return new SpringApplicationBuilder(MetaReleaseConfig.class,
				MetaReleaseScanningConfiguration.class).web(WebApplicationType.NONE)
						.properties("spring.jmx.enabled=false");
	}

	private SpringApplicationBuilder failingBuildRunner() {
		return new SpringApplicationBuilder(
				SpringMetaReleaseAcceptanceTests.MetaReleaseConfig.class,
				SpringMetaReleaseAcceptanceTests.MetaReleaseFailingTasksScanningConfiguration.class)
						.web(WebApplicationType.NONE)
						.properties("spring.jmx.enabled=false");
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
			return BDDMockito
					.mock(PostReleaseActions.class,
							invocation -> invocation.getMethod().getReturnType()
									.equals(ExecutionResult.class)
											? ExecutionResult.success() : null);
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
	@ComponentScan({ "releaser.internal", "releaser.cloud" })
	static class MetaReleaseScanningConfiguration {

	}

	@Configuration
	@ConditionalOnProperty(value = "test.metarelease.failing", havingValue = "true")
	@ComponentScan({ "releaser.internal", "releaser.cloud" })
	static class MetaReleaseFailingTasksScanningConfiguration {

		@Bean
		FirstTask firstTask() {
			return BDDMockito.spy(new FirstTask());
		}

		@Bean
		SecondTask secondTask() {
			return BDDMockito.spy(new SecondTask());
		}

		@Bean
		MetaReleaseCompositeTask metaReleaseCompositeTask(ApplicationContext context) {
			return new MetaReleaseCompositeTask(context);
		}

		@Bean
		ReleaseCompositeTask releaseCompositeTask(ApplicationContext context) {
			return new ReleaseCompositeTask(context);
		}

	}

}

class FirstTask implements ReleaseReleaserTask {

	@Override
	public String name() {
		return "first";
	}

	@Override
	public String shortName() {
		return "1";
	}

	@Override
	public String header() {
		return "FIRST";
	}

	@Override
	public String description() {
		return name();
	}

	@Override
	public ExecutionResult runTask(Arguments args)
			throws BuildUnstableException, RuntimeException {
		return ExecutionResult.failure(new IllegalStateException("Failure"));
	}

	@Override
	public int getOrder() {
		return 0;
	}

}

class SecondTask implements ReleaseReleaserTask {

	@Override
	public String name() {
		return "second";
	}

	@Override
	public String shortName() {
		return "2";
	}

	@Override
	public String header() {
		return "SECOND";
	}

	@Override
	public String description() {
		return name();
	}

	@Override
	public ExecutionResult runTask(Arguments args)
			throws BuildUnstableException, RuntimeException {
		return ExecutionResult.success();
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
