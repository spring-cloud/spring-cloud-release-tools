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

package org.springframework.cloud.release.internal.postrelease;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.awaitility.Awaitility;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;

import org.springframework.cloud.release.internal.PomUpdateAcceptanceTests;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.GradleUpdater;
import org.springframework.cloud.release.internal.buildsystem.ProjectPomUpdater;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.ProcessedProject;
import org.springframework.cloud.release.internal.project.ProjectCommandExecutor;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.tech.PomReader;
import org.springframework.cloud.release.internal.versions.VersionsFetcher;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;

/**
 * @author Marcin Grzejszczak
 */
public class PostReleaseActionsTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File temporaryFolder;

	GradleUpdater gradleUpdater = BDDMockito.mock(GradleUpdater.class);

	ReleaserProperties properties = new ReleaserProperties();

	File cloned;

	LinkedMultiValueMap<String, File> clonedTestProjects = new LinkedMultiValueMap<>();

	ProjectGitHandler projectGitHandler = new ProjectGitHandler(this.properties) {
		@Override
		public File cloneTestSamplesProject() {
			PostReleaseActionsTests.this.cloned = super.cloneTestSamplesProject();
			return PostReleaseActionsTests.this.cloned;
		}

		@Override
		public File cloneReleaseTrainDocumentationProject() {
			PostReleaseActionsTests.this.cloned = super.cloneReleaseTrainDocumentationProject();
			return PostReleaseActionsTests.this.cloned;
		}

		@Override
		public File cloneAndGuessBranch(String url, String... versions) {
			File file = super.cloneAndGuessBranch(url, versions);
			PostReleaseActionsTests.this.clonedTestProjects.add(url, file);
			return file;
		}
	};

	ProjectPomUpdater updater = new ProjectPomUpdater(this.properties, new ArrayList<>());

	VersionsFetcher versionsFetcher = new VersionsFetcher(properties, updater);

	ProjectCommandExecutor builder = new ProjectCommandExecutor(this.properties);

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_do_nothing_when_is_not_meta_release_and_update_test_is_called() {
		this.properties.getMetaRelease().setEnabled(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.runUpdatedTests(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveZeroInteractions();
	}

	@Test
	public void should_do_nothing_when_the_switch_for_sample_check_is_off_and_update_test_is_called() {
		this.properties.getGit().setRunUpdatedSamples(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.runUpdatedTests(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveZeroInteractions();
	}

	@Test
	public void should_update_project_and_run_tests_and_update_test_is_called() {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit().setTestSamplesProjectUrl(
				tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/");
		this.properties.getMaven().setBuildCommand("touch build.log");
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.runUpdatedTests(currentGa());

		Model rootPom = PomReader.readPom(new File(this.cloned, "pom.xml"));
		BDDAssertions.then(rootPom.getVersion()).isEqualTo("Finchley.SR1");
		BDDAssertions.then(rootPom.getParent().getVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(sleuthParentPomVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(new File(this.cloned, "build.log")).exists();
		thenGradleUpdaterWasCalled();
	}

	private void thenGradleUpdaterWasCalled() {
		BDDMockito.then(this.gradleUpdater).should().updateProjectFromBom(
				BDDMockito.any(File.class), BDDMockito.any(Projects.class),
				BDDMockito.any(ProjectVersion.class), BDDMockito.eq(false));
	}

	@Test
	public void should_do_nothing_when_is_not_meta_release_and_release_train_docs_generation_is_called() {
		this.properties.getMetaRelease().setEnabled(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.generateReleaseTrainDocumentation(currentGa());

		BDDAssertions.then(this.cloned).isNull();
	}

	@Test
	public void should_do_nothing_when_the_switch_for_sample_check_is_off_and_release_train_docs_generation_is_called() {
		this.properties.getGit().setUpdateReleaseTrainDocs(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.generateReleaseTrainDocumentation(currentGa());

		BDDAssertions.then(this.cloned).isNull();
	}

	@Ignore("flakey on circle")
	@Test
	public void should_update_project_and_run_tests_and_release_train_docs_generation_is_called() {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit().setReleaseTrainDocsUrl(
				tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/");
		this.properties.getMaven().setGenerateReleaseTrainDocsCommand("./test.sh");
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.generateReleaseTrainDocumentation(currentGa());

		Model rootPom = PomReader.readPom(new File(this.cloned, "pom.xml"));
		BDDAssertions.then(rootPom.getVersion()).isEqualTo("Finchley.SR1");
		BDDAssertions.then(rootPom.getParent().getVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(sleuthParentPomVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(new File(this.cloned, "generate.log")).exists();
		thenGradleUpdaterWasCalled();
	}

	@Test
	public void should_do_nothing_when_is_not_meta_release_and_test_samples_update_is_called() {
		this.properties.getMetaRelease().setEnabled(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.updateAllTestSamples(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveZeroInteractions();
	}

	@Test
	public void should_do_nothing_when_the_switch_for_test_samples_update_check_is_off_and_update_is_called() {
		this.properties.getGit().setUpdateReleaseTrainDocs(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.updateAllTestSamples(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveZeroInteractions();
	}

	@Test
	public void should_update_test_sample_projects_when_test_samples_update_is_called()
			throws Exception {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit().getAllTestSampleUrls().clear();
		this.properties.getGit().getAllTestSampleUrls().put("spring-cloud-sleuth",
				Collections.singletonList(
						tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/"));
		AtomicReference<Projects> postReleaseProjects = new AtomicReference<>();
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher) {
			@Override
			Projects getPostReleaseProjects(Projects projects) {
				postReleaseProjects.set(super.getPostReleaseProjects(projects));
				return postReleaseProjects.get();
			}
		};

		actions.updateAllTestSamples(currentGa());

		Map.Entry<String, List<File>> entry = this.clonedTestProjects.entrySet().stream()
				.filter(s -> s.getKey().contains("spring-cloud-core-tests")).findFirst()
				.orElseThrow(() -> new IllegalStateException("Not found"));
		File clonedFile = entry.getValue().get(0);
		Model pomWithCloud = PomReader
				.readPom(new File(clonedFile, "zuul-proxy-eureka/pom.xml"));
		Git git = GitTestUtils.openGitProject(clonedFile);
		BDDAssertions
				.then(pomWithCloud.getProperties().getProperty("spring-cloud.version"))
				.isEqualTo("Finchley.BUILD-SNAPSHOT");
		BDDAssertions.then(pomWithCloud.getParent().getVersion())
				.isEqualTo("2.0.4.RELEASE");
		Iterator<RevCommit> iterator = git.log().call().iterator();
		RevCommit commit = iterator.next();
		BDDAssertions.then(commit.getShortMessage()).isEqualTo(
				"Updated versions after [Finchley.SR1] release train and [2.0.1.RELEASE] [spring-cloud-sleuth] project release");
		thenGradleUpdaterWasCalled();
		BDDAssertions.then(
				postReleaseProjects.get().forName("spring-boot-dependencies").version)
				.isEqualTo("2.0.4.RELEASE");
	}

	@Test
	public void should_assume_that_project_version_is_snapshot_when_no_pom_is_present()
			throws Exception {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit().getAllTestSampleUrls().clear();
		this.properties.getGit().getAllTestSampleUrls().put("spring-cloud-sleuth",
				Collections.singletonList(
						tmpFile("spring-cloud-static/").getAbsolutePath() + "/"));
		AtomicReference<Projects> postReleaseProjects = new AtomicReference<>();
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher) {
			@Override
			Projects getPostReleaseProjects(Projects projects) {
				postReleaseProjects.set(super.getPostReleaseProjects(projects));
				return postReleaseProjects.get();
			}
		};

		actions.updateAllTestSamples(currentGa());

		Map.Entry<String, List<File>> entry = this.clonedTestProjects.entrySet().stream()
				.filter(s -> s.getKey().contains("spring-cloud-static")).findFirst()
				.orElseThrow(() -> new IllegalStateException("Not found"));
	}

	@Test
	public void should_do_nothing_when_guides_are_turned_off() {
		this.properties.getGit().setUpdateSpringGuides(false);
		VersionsFetcher versionsFetcher = BDDMockito.mock(VersionsFetcher.class);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.gradleUpdater, this.builder, this.properties,
				versionsFetcher);

		actions.deployGuides(Collections.emptyList());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(versionsFetcher).shouldHaveZeroInteractions();
	}

	@Test
	public void should_build_and_deploy_guides_when_switch_is_on() throws Exception {
		this.properties.getGit().setUpdateSpringGuides(true);
		String projects = this.temporaryFolder.getAbsolutePath();
		this.properties.getMetaRelease().setGitOrgUrl(projects);
		VersionsFetcher versionsFetcher = BDDMockito.mock(VersionsFetcher.class);
		BDDMockito.given(versionsFetcher.isLatestGa(BDDMockito.any())).willReturn(true);
		AtomicReference<ProjectCommandExecutor> projectBuilderStub = new AtomicReference<>();
		ProjectGitHandler handler = BDDMockito.mock(ProjectGitHandler.class);
		ProjectCommandExecutor projectCommandExecutor = BDDMockito
				.mock(ProjectCommandExecutor.class);
		PostReleaseActions actions = new PostReleaseActions(handler, this.updater,
				this.gradleUpdater, this.builder, this.properties, versionsFetcher) {
			@Override
			ProjectCommandExecutor projectBuilder(ProcessedProject processedProject) {
				projectBuilderStub.set(projectCommandExecutor);
				return projectCommandExecutor;
			}
		};
		ProjectVersion projectVersion = new ProjectVersion(
				new File(projects, "spring-cloud-release"));

		actions.deployGuides(Collections
				.singletonList(new ProcessedProject(this.properties, projectVersion)));

		Awaitility.await().untilAsserted(() -> {
			BDDAssertions.then(projectBuilderStub.get()).isNotNull();
			BDDMockito.then(projectBuilderStub.get()).should()
					.deployGuides(projectVersion);
		});
	}

	private String sleuthParentPomVersion() {
		return PomReader.readPom(new File(this.cloned, "sleuth/pom.xml")).getParent()
				.getVersion();
	}

	Projects currentGa() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "2.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "2.0.4.RELEASE"),
				new ProjectVersion("spring-boot-dependencies", "2.0.4.RELEASE"),
				new ProjectVersion("spring-boot-starter", "2.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-release", "Finchley.SR1"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI());
	}

}
