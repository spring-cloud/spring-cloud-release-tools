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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.GradleUpdater;
import org.springframework.cloud.release.internal.buildsystem.ProjectPomUpdater;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.buildsystem.TestPomReader;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.docs.TestDocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectCommandExecutor;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.sagan.Project;
import org.springframework.cloud.release.internal.sagan.Release;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author Marcin Grzejszczak
 */
public class AcceptanceTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Rule
	public OutputCapture capture = new OutputCapture();

	TestPomReader testPomReader = new TestPomReader();

	File springCloudConsulProject;

	File temporaryFolder;

	File documentationFolder;

	File cloudProjectFolder;

	TestProjectGitHubHandler gitHandler;

	NonAssertingTestProjectGitHandler nonAssertingGitHandler;

	NonAssertingTestProjectGitHubHandler nonAssertingGitHubHandler;

	SaganClient saganClient = Mockito.mock(SaganClient.class);

	ReleaserProperties releaserProperties;

	TemplateGenerator templateGenerator;

	SaganUpdater saganUpdater;

	DocumentationUpdater documentationUpdater;

	ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);

	ReleaserPropertiesUpdater updater = new ReleaserPropertiesUpdater(
			this.applicationContext);

	PostReleaseActions postReleaseActions = Mockito.mock(PostReleaseActions.class);

	ApplicationEventPublisher applicationEventPublisher = Mockito
			.mock(ApplicationEventPublisher.class);

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(AcceptanceTests.class
				.getResource("/projects/spring-cloud-consul").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
		BDDMockito.given(this.saganClient.getProject(anyString()))
				.willReturn(newProject());
		Task.stepSkipper = () -> false;
	}

	@After
	public void clean() {
		Task.stepSkipper = new ConsoleInputStepSkipper();
	}

	private Project newProject() {
		Project project = new Project();
		project.projectReleases.addAll(Arrays.asList(release("1.0.0.M8"),
				release("1.1.0.M8"), release("1.2.0.M8"), release("2.0.0.M8")));
		return project;
	}

	private Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}

	@Test
	public void should_fail_to_perform_a_release_of_consul_when_sc_release_contains_snapshots()
			throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(),
				tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaserWithSnapshotScRelease(project,
				"spring-cloud-consul", "vCamden.SR5.BROKEN", "1.1.2.RELEASE");

		BDDAssertions.thenThrownBy(releaser::release).hasMessageContaining(
				"there is at least one SNAPSHOT library version in the Spring Cloud Release project");
	}

	@Test
	public void should_not_clone_when_option_not_to_clone_was_switched_on()
			throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(),
				tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = templateOnlyReleaser(project, "spring-cloud-consul",
				"vCamden.SR5", "1.1.2.RELEASE");
		this.releaserProperties.getGit().setFetchVersionsFromGit(false);
		this.releaserProperties.getFixedVersions().put("spring-cloud-release",
				"Finchley.RELEASE");
		this.releaserProperties.getFixedVersions().put("spring-cloud-consul",
				"2.3.4.RELEASE");
		File temporaryDestination = this.tmp.newFolder();
		this.releaserProperties.getGit()
				.setCloneDestinationDir(temporaryDestination.getAbsolutePath());

		releaser.release();

		then(temporaryDestination.list()).isEmpty();
	}

	@Test
	public void should_perform_a_release_of_consul() throws Exception {
		GitTestUtils.openGitProject(file("/projects/spring-cloud-release/")).checkout()
				.setName("Greenwich").call();
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(),
				tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "spring-cloud-consul",
				"vGreenwich.SR2", "2.1.2.RELEASE");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		Iterator<RevCommit> iterator = commits.iterator();
		tagIsPresentInOrigin(origin, "v2.1.2.RELEASE");
		commitIsPresent(iterator,
				"Bumping versions to 2.1.3.BUILD-SNAPSHOT after release");
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 2.1.2.RELEASE");
		pomVersionIsEqualTo(project, "2.1.3.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(project, "2.1.3.BUILD-SNAPSHOT");
		then(this.gitHandler.closedMilestones).isTrue();
		then(emailTemplate()).exists();
		then(emailTemplateContents()).contains("Spring Cloud Greenwich.SR2 available")
				.contains("Spring Cloud Greenwich SR2 Train release");
		then(blogTemplate()).exists();
		then(blogTemplateContents())
				.contains("I am pleased to announce that the Service Release 2 (SR2)");
		then(releaseNotesTemplate()).exists();
		then(releaseNotesTemplateContents()).contains("Greenwich.SR2").contains(
				"- Spring Cloud Config `2.1.3.RELEASE` ([issues](https://foo.bar.com/2.1.3.RELEASE))")
				.contains(
						"- Spring Cloud Aws `2.1.2.RELEASE` ([issues](https://foo.bar.com/2.1.2.RELEASE))");
		// once for updating GA
		// second time to update SNAPSHOT
		BDDMockito.then(this.saganClient).should(BDDMockito.times(2)).updateRelease(
				BDDMockito.eq("spring-cloud-consul"), BDDMockito.anyList());
		BDDMockito.then(this.saganClient).should().deleteRelease("spring-cloud-consul",
				"2.1.2.BUILD-SNAPSHOT");
		then(this.gitHandler.issueCreatedInSpringGuides).isTrue();
		then(this.gitHandler.issueCreatedInStartSpringIo).isTrue();
		then(text(new File(this.documentationFolder, "current/index.html")))
				.doesNotContain("Angel.SR3").contains("Greenwich.SR2");
		thenRunUpdatedTestsWereCalled();
	}

	@Test
	public void should_perform_a_meta_release_of_sc_release_and_consul()
			throws Exception {
		// simulates an org
		GitTestUtils.openGitProject(file("/projects/spring-cloud-release/")).checkout()
				.setName("Edgware").call();
		SpringReleaser releaser = metaReleaser(edgwareSr10());

		releaser.release(new OptionsBuilder().metaRelease(true).options());

		// consul, release, documentation
		then(this.nonAssertingGitHandler.clonedProjects).hasSize(3);
		// don't want to verify the docs
		thenAllStepsWereExecutedForEachProject();
		thenSaganWasCalled();
		thenDocumentationWasUpdated();
		BDDAssertions.then(clonedProject("spring-cloud-consul").tagList().call())
				.extracting("name").contains("refs/tags/v5.3.5.RELEASE");
		thenRunUpdatedTestsWereCalled();
		thenUpdateReleaseTrainDocsWasCalled();
	}

	private void thenRunUpdatedTestsWereCalled() {
		BDDMockito.then(this.postReleaseActions).should()
				.runUpdatedTests(BDDMockito.any(Projects.class));
	}

	private void thenUpdateReleaseTrainDocsWasCalled() {
		BDDMockito.then(this.postReleaseActions).should()
				.generateReleaseTrainDocumentation(BDDMockito.any(Projects.class));
	}

	private Map<String, String> edgwareSr10() {
		Map<String, String> versions = new LinkedHashMap<>();
		versions.put("spring-boot", "5.5.16.RELEASE");
		versions.put("spring-cloud-build", "5.3.11.RELEASE");
		versions.put("spring-cloud-commons", "5.3.5.RELEASE");
		versions.put("spring-cloud-stream", "Xitmars.SR4");
		versions.put("spring-cloud-task", "5.2.3.RELEASE");
		versions.put("spring-cloud-function", "5.0.1.RELEASE");
		versions.put("spring-cloud-aws", "5.2.3.RELEASE");
		versions.put("spring-cloud-bus", "5.3.4.RELEASE");
		versions.put("spring-cloud-config", "5.4.5.RELEASE");
		versions.put("spring-cloud-netflix", "5.4.6.RELEASE");
		versions.put("spring-cloud-cloudfoundry", "5.1.2.RELEASE");
		versions.put("spring-cloud-gateway", "5.0.2.RELEASE");
		versions.put("spring-cloud-security", "5.2.3.RELEASE");
		versions.put("spring-cloud-consul", "5.3.5.RELEASE");
		versions.put("spring-cloud-zookeeper", "5.2.2.RELEASE");
		versions.put("spring-cloud-sleuth", "5.3.5.RELEASE");
		versions.put("spring-cloud-contract", "5.2.6.RELEASE");
		versions.put("spring-cloud-vault", "5.1.2.RELEASE");
		versions.put("spring-cloud-release", "Edgware.SR10");
		return versions;
	}

	private Git clonedProject(String name) {
		return GitTestUtils.openGitProject(this.nonAssertingGitHandler.clonedProjects
				.stream().filter(file -> file.getName().equals(name)).findFirst().get());
	}

	private Git gitProject(File file) {
		return GitTestUtils.openGitProject(file);
	}

	private void thenSaganWasCalled() {
		BDDMockito.then(this.saganUpdater).should(BDDMockito.atLeastOnce()).updateSagan(
				BDDMockito.any(File.class), BDDMockito.anyString(),
				BDDMockito.any(ProjectVersion.class),
				BDDMockito.any(ProjectVersion.class));
	}

	private void thenAllStepsWereExecutedForEachProject() {
		this.nonAssertingGitHandler.clonedProjects.stream()
				.filter(f -> !f.getName().contains("angel")
						&& !f.getName().equals("spring-cloud"))
				.forEach(project -> {
					then(Arrays.asList("spring-cloud-starter-build",
							"spring-cloud-consul"))
									.contains(pom(project).getArtifactId());
					then(this.capture.toString()).contains("executed_build",
							"executed_deploy", "executed_docs");
				});
	}

	@Test
	public void should_not_clone_any_projects_when_they_are_on_list_of_projects_to_skip()
			throws Exception {
		Map<String, String> versions = new HashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		SpringReleaser releaser = metaReleaser(versions);
		this.releaserProperties.getMetaRelease().getProjectsToSkip()
				.add("spring-cloud-release");
		this.releaserProperties.getMetaRelease().getProjectsToSkip()
				.add("spring-cloud-consul");
		this.releaserProperties.getGit().setUpdateReleaseTrainWiki(false);
		File temporaryDestination = this.tmp.newFolder();
		this.releaserProperties.getGit()
				.setCloneDestinationDir(temporaryDestination.getAbsolutePath());

		releaser.release(new OptionsBuilder().metaRelease(true).options());

		then(temporaryDestination.list()).isEmpty();
	}

	@Test
	public void should_perform_a_meta_release_of_consul_only_when_run_from_got_passed()
			throws Exception {
		// simulates an org
		Map<String, String> versions = new HashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-build", "1.1.2.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		SpringReleaser releaser = metaReleaser(versions);

		releaser.release(new OptionsBuilder().metaRelease(true)
				.startFrom("spring-cloud-consul").options());

		// consul
		then(this.nonAssertingGitHandler.clonedProjects).hasSize(1);
		this.nonAssertingGitHandler.clonedProjects.stream()
				.filter(file -> file.getName().equals("spring-cloud-consul"))
				.forEach(project -> {
					then(pom(project).getArtifactId()).isEqualTo("spring-cloud-consul");
					then(this.capture.toString()).contains("executed_build",
							"executed_deploy", "executed_docs");
				});
		thenSaganWasCalled();
		thenDocumentationWasUpdated();
		thenWikiPageWasUpdated();
	}

	@Test
	public void should_perform_a_meta_release_of_consul_only_when_task_names_got_passed()
			throws Exception {
		// simulates an org
		Map<String, String> versions = new HashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-build", "1.1.2.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		SpringReleaser releaser = metaReleaser(versions);

		releaser.release(new OptionsBuilder().metaRelease(true)
				.taskNames(Collections.singletonList("spring-cloud-consul")).options());

		// consul
		then(this.nonAssertingGitHandler.clonedProjects).hasSize(1);
		this.nonAssertingGitHandler.clonedProjects.stream()
				.filter(file -> !file.getName().equals("spring-cloud"))
				.forEach(project -> {
					then(Collections.singletonList("spring-cloud-consul"))
							.contains(pom(project).getArtifactId());
					then(this.capture.toString()).contains("executed_build",
							"executed_deploy", "executed_docs");
				});
		thenSaganWasCalled();
		thenDocumentationWasUpdated();
		thenWikiPageWasUpdated();
	}

	private void thenDocumentationWasUpdated() {
		BDDMockito.then(this.documentationUpdater).should().updateDocsRepo(
				BDDMockito.any(ProjectVersion.class), BDDMockito.anyString());
	}

	private void thenWikiPageWasUpdated() {
		BDDMockito.then(this.documentationUpdater).should()
				.updateReleaseTrainWiki(BDDMockito.any(Projects.class));
	}

	// issue #74
	@Test
	public void should_perform_a_release_of_sc_build() throws Exception {
		GitTestUtils.openGitProject(file("/projects/spring-cloud-release/")).checkout()
				.setName("vGreenwich.SR2").call();
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				new File(AcceptanceTests.class.getResource("/projects/spring-cloud-build")
						.toURI()));
		pomVersionIsEqualTo(origin, "1.3.7.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "spring-cloud-build-dependencies",
				"1.5.9.RELEASE");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(),
				tmpFile("spring-cloud-build"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "spring-cloud-build",
				"vGreenwich.SR2", "2.1.6.RELEASE");

		releaser.release();

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
		then(this.gitHandler.closedMilestones).isTrue();
		then(emailTemplate()).exists();
		then(blogTemplate()).exists();
		then(releaseNotesTemplate()).exists();
		// once for updating GA
		// second time to update SNAPSHOT
		BDDMockito.then(this.saganClient).should(BDDMockito.times(2))
				.updateRelease(BDDMockito.eq("spring-cloud-build"), BDDMockito.anyList());
		BDDMockito.then(this.saganClient).should().deleteRelease("spring-cloud-build",
				"2.1.6.BUILD-SNAPSHOT");
		then(this.gitHandler.issueCreatedInSpringGuides).isTrue();
		then(this.gitHandler.issueCreatedInStartSpringIo).isTrue();
		then(text(new File(this.documentationFolder, "current/index.html")))
				.doesNotContain("Angel.SR3").contains("Greenwich.SR2");
	}

	@Test
	public void should_perform_a_release_of_consul_rc1() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(),
				tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		Git git = GitTestUtils.openGitProject(file("/projects/spring-cloud-release/"));
		git.reset().setMode(ResetCommand.ResetType.HARD).setRef("vDalston.RC1").call();
		git.checkout().setName("vDalston.RC1").call();
		SpringReleaser releaser = releaser(project, "spring-cloud-consul", "vDalston.RC1",
				"1.2.0.RC1");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		tagIsPresentInOrigin(origin, "v1.2.0.RC1");
		commitIsNotPresent(commits,
				"Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
		Iterator<RevCommit> iterator = listOfCommits(project).iterator();
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.2.0.RC1");
		pomVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
		then(this.gitHandler.closedMilestones).isTrue();
		then(emailTemplate()).exists();
		then(emailTemplateContents()).contains("Spring Cloud Dalston.RC1 available")
				.contains("Spring Cloud Dalston RC1 Train release");
		then(blogTemplate()).exists();
		then(blogTemplateContents())
				.contains("I am pleased to announce that the Release Candidate 1 (RC1)");
		then(tweetTemplate()).exists();
		then(tweetTemplateContents())
				.contains("The Dalston.RC1 version of @springcloud has been released!");
		then(releaseNotesTemplate()).exists();
		then(releaseNotesTemplateContents()).contains("Dalston.RC1").contains(
				"- Spring Cloud Build `1.3.1.RELEASE` ([issues](https://foo.bar.com/1.3.1.RELEASE))")
				.contains(
						"- Spring Cloud Bus `1.3.0.M1` ([issues](https://foo.bar.com/1.3.0.M1))");
		BDDMockito.then(this.saganClient).should().updateRelease(
				BDDMockito.eq("spring-cloud-consul"), BDDMockito.anyList());
		BDDMockito.then(this.saganClient).should().deleteRelease("spring-cloud-consul",
				"1.2.0.M8");
		BDDMockito.then(this.saganClient).should().deleteRelease("spring-cloud-consul",
				"1.2.0.RC1");
		// we update guides only for SR / RELEASE
		then(this.gitHandler.issueCreatedInSpringGuides).isFalse();
		then(this.gitHandler.issueCreatedInStartSpringIo).isFalse();
		// haven't even checked out the branch
		then(new File(this.documentationFolder, "current/index.html")).doesNotExist();
	}

	@Test
	public void should_generate_templates_only() throws Exception {
		GitTestUtils.openGitProject(file("/projects/spring-cloud-release/")).checkout()
				.setName("vDalston.RC1").call();
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(),
				tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = templateOnlyReleaser(project, "spring-cloud-consul",
				"vDalston.RC1", "1.2.0.RC1");

		releaser.release();

		then(this.gitHandler.closedMilestones).isFalse();
		then(emailTemplate()).exists();
		then(emailTemplateContents()).contains("Spring Cloud Dalston.RC1 available")
				.contains("Spring Cloud Dalston RC1 Train release");
		then(blogTemplate()).exists();
		then(blogTemplateContents())
				.contains("I am pleased to announce that the Release Candidate 1 (RC1)");
		then(tweetTemplate()).exists();
		then(tweetTemplateContents())
				.contains("The Dalston.RC1 version of @springcloud has been released!");
		then(releaseNotesTemplate()).exists();
		then(releaseNotesTemplateContents()).contains("Dalston.RC1").contains(
				"- Spring Cloud Build `1.3.1.RELEASE` ([issues](https://foo.bar.com/1.3.1.RELEASE))")
				.contains(
						"- Spring Cloud Bus `1.3.0.M1` ([issues](https://foo.bar.com/1.3.0.M1)");
		BDDMockito.then(this.saganClient).should(BDDMockito.never())
				.updateRelease(BDDMockito.anyString(), BDDMockito.anyList());
		then(this.gitHandler.issueCreatedInSpringGuides).isFalse();
		then(this.gitHandler.issueCreatedInStartSpringIo).isFalse();
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

	private void tagIsPresentInOrigin(File origin, String expectedTag)
			throws GitAPIException {
		then(GitTestUtils.openGitProject(origin).tagList().call().iterator().next()
				.getName()).endsWith(expectedTag);
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

	private SpringReleaser releaser(File projectFile, String projectName, String branch,
			String expectedVersion) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile, branch);
		return releaserWithFullDeployment(expectedVersion, projectName, properties);
	}

	private SpringReleaser metaReleaser(Map<String, String> versions) throws Exception {
		ReleaserProperties properties = metaReleaserProperties(versions);
		return metaReleaserWithFullDeployment(properties);
	}

	private SpringReleaser releaserWithFullDeployment(String expectedVersion,
			String projectName, ReleaserProperties properties) throws Exception {
		Releaser releaser = defaultReleaser(expectedVersion, projectName, properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser,
				properties, this.applicationEventPublisher) {
			@Override
			String chosenOption() {
				return "0";
			}

			@Override
			void postReleaseOptions(Options options, Args defaultArgs) {
				options.interactive = false;
				super.postReleaseOptions(options, defaultArgs);
			}
		}, this.updater, this.applicationEventPublisher);
	}

	private SpringReleaser metaReleaserWithFullDeployment(ReleaserProperties properties)
			throws Exception {
		Releaser releaser = defaultMetaReleaser(properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser,
				properties, this.applicationEventPublisher) {
			@Override
			String chosenOption() {
				return "0";
			}

			@Override
			void postReleaseOptions(Options options, Args defaultArgs) {
				options.interactive = false;
				super.postReleaseOptions(options, defaultArgs);
			}
		}, this.updater, this.applicationEventPublisher);
	}

	private SpringReleaser releaserWithSnapshotScRelease(File projectFile,
			String projectName, String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = snapshotScReleaseReleaserProperties(projectFile,
				branch);
		return releaserWithFullDeployment(expectedVersion, projectName, properties);
	}

	private SpringReleaser templateOnlyReleaser(File projectFile, String projectName,
			String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile, branch);
		Releaser releaser = defaultReleaser(expectedVersion, projectName, properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser,
				properties, this.applicationEventPublisher) {
			@Override
			String chosenOption() {
				return "13";
			}

			@Override
			void postReleaseOptions(Options options, Args defaultArgs) {
				options.interactive = true;
				super.postReleaseOptions(options, defaultArgs);
			}
		}, this.updater, this.applicationEventPublisher);
	}

	private Releaser defaultReleaser(String expectedVersion, String projectName,
			ReleaserProperties properties) {
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties, bomParsers);
		ProjectCommandExecutor projectCommandExecutor = new ProjectCommandExecutor(
				properties);
		ProjectGitHandler gitHandler = new ProjectGitHandler(properties);
		TestProjectGitHubHandler githubHandler = new TestProjectGitHubHandler(properties,
				expectedVersion, projectName);
		TemplateGenerator templateGenerator = new TemplateGenerator(properties,
				githubHandler);
		GradleUpdater gradleUpdater = new GradleUpdater(properties);
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient,
				this.releaserProperties);
		DocumentationUpdater documentationUpdater = new TestDocumentationUpdater(
				properties,
				new TestDocumentationUpdater.TestCustomProjectDocumentationUpdater(properties,
						gitHandler, "Brixton.SR1"),
				new TestDocumentationUpdater.TestReleaseContentsUpdater(properties,
						gitHandler, templateGenerator)) {
			@Override
			public File updateDocsRepo(ProjectVersion currentProject,
					String bomReleaseBranch) {
				File file = super.updateDocsRepo(currentProject, bomReleaseBranch);
				AcceptanceTests.this.documentationFolder = file;
				return file;
			}
		};
		Releaser releaser = new Releaser(releaserProperties, pomUpdater,
				projectCommandExecutor, gitHandler, githubHandler, templateGenerator,
				gradleUpdater, saganUpdater, documentationUpdater,
				this.postReleaseActions);
		this.gitHandler = githubHandler;
		return releaser;
	}

	private Releaser defaultMetaReleaser(ReleaserProperties properties) {
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties, bomParsers);
		ProjectCommandExecutor projectCommandExecutor = new ProjectCommandExecutor(
				properties);
		NonAssertingTestProjectGitHubHandler handler = new NonAssertingTestProjectGitHubHandler(
				properties);
		NonAssertingTestProjectGitHandler nonAssertingGitHandler = new NonAssertingTestProjectGitHandler(
				properties);
		TemplateGenerator templateGenerator = Mockito
				.spy(new TemplateGenerator(properties, handler));
		GradleUpdater gradleUpdater = new GradleUpdater(properties);
		SaganUpdater saganUpdater = Mockito
				.spy(new SaganUpdater(this.saganClient, this.releaserProperties));
		DocumentationUpdater documentationUpdater = Mockito
				.spy(new TestDocumentationUpdater(properties,
						new TestDocumentationUpdater.TestCustomProjectDocumentationUpdater(
								properties, nonAssertingGitHandler, "Brixton.SR1"),
						new TestDocumentationUpdater.TestReleaseContentsUpdater(
								properties, nonAssertingGitHandler, templateGenerator) {
							@Override
							public File updateProjectRepo(Projects projects) {
								File file = super.updateProjectRepo(projects);
								AcceptanceTests.this.cloudProjectFolder = file;
								return file;
							}
						}) {
					@Override
					public File updateDocsRepo(ProjectVersion currentProject,
							String bomReleaseBranch) {
						File file = super.updateDocsRepo(currentProject,
								bomReleaseBranch);
						AcceptanceTests.this.documentationFolder = file;
						return file;
					}
				});
		Releaser releaser = Mockito
				.spy(new Releaser(releaserProperties, pomUpdater, projectCommandExecutor,
						nonAssertingGitHandler, handler, templateGenerator, gradleUpdater,
						saganUpdater, documentationUpdater, this.postReleaseActions));
		this.nonAssertingGitHubHandler = handler;
		this.nonAssertingGitHandler = nonAssertingGitHandler;
		this.templateGenerator = templateGenerator;
		this.saganUpdater = saganUpdater;
		this.documentationUpdater = documentationUpdater;
		return releaser;
	}

	private ReleaserProperties releaserProperties(File project, String branch)
			throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release/").toURI().toString());
		releaserProperties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static-angel/").toURI().toString());
		releaserProperties.getMaven().setBuildCommand("echo build");
		releaserProperties.getMaven().setDeployCommand("echo deploy");
		releaserProperties.getMaven()
				.setPublishDocsCommands(new String[] { "echo docs" });
		releaserProperties.setWorkingDir(project.getPath());
		releaserProperties.getPom().setBranch(branch);
		releaserProperties.getGit()
				.setSpringProjectUrl(tmpFile("spring-cloud").getAbsolutePath() + "/");
		releaserProperties.getGit().setReleaseTrainWikiUrl(
				tmpFile("spring-cloud-wiki").getAbsolutePath() + "/");
		URI initilizrUri = AcceptanceTests.class.getResource("/raw/initializr.yml")
				.toURI();
		releaserProperties.getVersions().setAllVersionsFileUrl(initilizrUri.toString());
		this.releaserProperties = releaserProperties;
		return releaserProperties;
	}

	private ReleaserProperties metaReleaserProperties(Map<String, String> versions)
			throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		Arrays.asList("spring-cloud-build", "spring-cloud-commons", "spring-cloud-stream",
				"spring-cloud-task", "spring-cloud-function", "spring-cloud-aws",
				"spring-cloud-bus", "spring-cloud-config", "spring-cloud-netflix",
				"spring-cloud-cloudfoundry", "spring-cloud-gateway",
				"spring-cloud-security", "spring-cloud-zookeeper", "spring-cloud-sleuth",
				"spring-cloud-contract", "spring-cloud-vault")
				.forEach(s -> releaserProperties.getMetaRelease().getProjectsToSkip()
						.add(s));
		releaserProperties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static-angel/").toURI().toString());
		releaserProperties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release/").toURI().toString());
		releaserProperties.getMaven().setBuildCommand("echo executed_build");
		releaserProperties.getMaven().setDeployCommand("echo executed_deploy");
		releaserProperties.getMaven()
				.setPublishDocsCommands(new String[] { "echo executed_docs" });
		releaserProperties.getMetaRelease()
				.setGitOrgUrl("file://" + this.temporaryFolder.getAbsolutePath());
		releaserProperties.getMetaRelease().setEnabled(true);
		releaserProperties.getGit()
				.setSpringProjectUrl(tmpFile("spring-cloud").getAbsolutePath() + "/");
		releaserProperties.getGit().setReleaseTrainWikiUrl(
				tmpFile("spring-cloud-wiki").getAbsolutePath() + "/");
		releaserProperties.setFixedVersions(versions);
		URI initilizrUri = AcceptanceTests.class.getResource("/raw/initializr.yml")
				.toURI();
		releaserProperties.getVersions().setAllVersionsFileUrl(initilizrUri.toString());
		this.releaserProperties = releaserProperties;
		return releaserProperties;
	}

	private ReleaserProperties snapshotScReleaseReleaserProperties(File project,
			String branch) throws URISyntaxException {
		ReleaserProperties releaserProperties = releaserProperties(project, branch);
		releaserProperties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release-with-snapshot/").toURI().toString());
		releaserProperties.getGit().setDocumentationUrl(
				file("/projects/spring-cloud-static/").toURI().toString());
		URI initilizrUri = AcceptanceTests.class.getResource("/raw/initializr.yml")
				.toURI();
		releaserProperties.getVersions().setAllVersionsFileUrl(initilizrUri.toString());
		this.releaserProperties = releaserProperties;
		return releaserProperties;
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(AcceptanceTests.class.getResource(relativePath).toURI());
	}

	private String text(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	class TestProjectGitHubHandler extends ProjectGitHubHandler {

		final String expectedVersion;

		final String projectName;

		boolean closedMilestones = false;

		boolean issueCreatedInSpringGuides = false;

		boolean issueCreatedInStartSpringIo = false;

		TestProjectGitHubHandler(ReleaserProperties properties, String expectedVersion,
				String projectName) {
			super(properties);
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

	class NonAssertingTestProjectGitHubHandler extends ProjectGitHubHandler {

		boolean closedMilestones = false;

		boolean issueCreatedInSpringGuides = false;

		boolean issueCreatedInStartSpringIo = false;

		NonAssertingTestProjectGitHubHandler(ReleaserProperties properties) {
			super(properties);
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
		public void createIssueInStartSpringIo(Projects projects,
				ProjectVersion version) {
			this.issueCreatedInStartSpringIo = true;
		}

		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "https://foo.bar.com/" + releaseVersion.toString();
		}

	}

	class NonAssertingTestProjectGitHandler extends ProjectGitHandler {

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

}
