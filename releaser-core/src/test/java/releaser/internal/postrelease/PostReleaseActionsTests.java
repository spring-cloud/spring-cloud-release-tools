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

package releaser.internal.postrelease;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.BDDMockito;
import releaser.SpringCloudReleaserProperties;
import releaser.internal.PomUpdateAcceptanceTests;
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesUpdater;
import releaser.internal.buildsystem.GradleUpdater;
import releaser.internal.buildsystem.ProjectPomUpdater;
import releaser.internal.buildsystem.TestUtils;
import releaser.internal.git.GitTestUtils;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProcessedProject;
import releaser.internal.project.ProjectCommandExecutor;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.tech.PomReader;
import releaser.internal.versions.VersionsFetcher;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;

/**
 * @author Marcin Grzejszczak
 */
public class PostReleaseActionsTests {

	@TempDir
	File tmp;

	File temporaryFolder;

	GradleUpdater gradleUpdater = BDDMockito.mock(GradleUpdater.class);

	ReleaserProperties properties = SpringCloudReleaserProperties.get();

	File cloned;

	LinkedMultiValueMap<String, File> clonedTestProjects = new LinkedMultiValueMap<>();

	ProjectGitHandler projectGitHandler = projectGitHandler(this.properties);

	ProjectPomUpdater updater = projectPomUpdater(this.properties);

	VersionsFetcher versionsFetcher = fetcher(this.properties);

	ReleaserPropertiesUpdater releaserPropertiesUpdater = new ReleaserPropertiesUpdater();

	ProjectCommandExecutor commandExecutor = new ProjectCommandExecutor();

	private ProjectGitHandler projectGitHandler(ReleaserProperties properties) {
		return new ProjectGitHandler(properties) {
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
	}

	private ProjectPomUpdater projectPomUpdater(ReleaserProperties properties) {
		return new ProjectPomUpdater(properties, new ArrayList<>());
	}

	private VersionsFetcher fetcher(ReleaserProperties properties) {
		return new VersionsFetcher(properties, updater);
	}

	@BeforeEach
	public void setup() throws Exception {
		this.temporaryFolder = new File(tmp, "test.txt");
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_do_nothing_when_is_not_meta_release_and_update_test_is_called() {
		this.properties.getMetaRelease().setEnabled(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.runUpdatedTests(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveNoInteractions();
	}

	@Test
	public void should_do_nothing_when_the_switch_for_sample_check_is_off_and_update_test_is_called() {
		this.properties.getGit().setRunUpdatedSamples(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.runUpdatedTests(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveNoInteractions();
	}

	@Test
	public void should_update_project_and_run_tests_and_update_test_is_called() {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		properties.getMetaRelease().setEnabled(true);
		properties.getGit().setRunUpdatedSamples(true);
		properties.getGit().setUpdateAllTestSamples(true);
		properties.getGit().setTestSamplesProjectUrl(tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/");
		properties.getMaven().setBuildCommand("touch build.log");
		PostReleaseActions actions = new PostReleaseActions(projectGitHandler(properties),
				projectPomUpdater(properties), this.gradleUpdater, commandExecutor, properties, fetcher(properties),
				releaserPropertiesUpdater) {
			@Override
			ReleaserProperties projectProps(File file) {
				return properties;
			}
		};

		actions.runUpdatedTests(currentGa());

		Model rootPom = PomReader.readPom(new File(this.cloned, "pom.xml"));
		BDDAssertions.then(rootPom.getVersion()).isEqualTo("Finchley.SR1");
		BDDAssertions.then(rootPom.getParent().getVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(sleuthParentPomVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(new File(this.cloned, "build.log")).exists();
		thenGradleUpdaterWasCalled();
	}

	private void thenGradleUpdaterWasCalled() {
		BDDMockito.then(this.gradleUpdater).should().updateProjectFromReleaseTrain(
				BDDMockito.any(ReleaserProperties.class), BDDMockito.any(File.class), BDDMockito.any(Projects.class),
				BDDMockito.any(ProjectVersion.class), BDDMockito.eq(false));
	}

	@Test
	public void should_do_nothing_when_is_not_meta_release_and_release_train_docs_generation_is_called() {
		this.properties.getMetaRelease().setEnabled(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.generateReleaseTrainDocumentation(currentGa());

		BDDAssertions.then(this.cloned).isNull();
	}

	@Test
	public void should_do_nothing_when_the_switch_for_sample_check_is_off_and_release_train_docs_generation_is_called() {
		this.properties.getGit().setUpdateReleaseTrainDocs(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.generateReleaseTrainDocumentation(currentGa());

		BDDAssertions.then(this.cloned).isNull();
	}

	@Disabled("flakey on circle")
	@Test
	public void should_update_project_and_run_tests_and_release_train_docs_generation_is_called() {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit().setReleaseTrainDocsUrl(tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/");
		this.properties.getMaven().setGenerateReleaseTrainDocsCommand("./test.sh");
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

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
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.updateAllTestSamples(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveNoInteractions();
	}

	@Test
	public void should_do_nothing_when_the_switch_for_test_samples_update_check_is_off_and_update_is_called() {
		this.properties.getGit().setUpdateReleaseTrainDocs(false);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.updateAllTestSamples(currentGa());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(this.gradleUpdater).shouldHaveNoInteractions();
	}

	@Test
	public void should_update_test_sample_projects_when_test_samples_update_is_called() throws Exception {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		properties.getMetaRelease().setEnabled(true);
		properties.getGit().setUpdateAllTestSamples(true);
		properties.getGit().getAllTestSampleUrls().clear();
		properties.getGit().getAllTestSampleUrls().put("spring-cloud-sleuth",
				Collections.singletonList(tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/"));
		AtomicReference<Projects> postReleaseProjects = new AtomicReference<>();
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, properties, versionsFetcher, releaserPropertiesUpdater) {
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
		Model pomWithCloud = PomReader.readPom(new File(clonedFile, "zuul-proxy-eureka/pom.xml"));
		Git git = GitTestUtils.openGitProject(clonedFile);
		BDDAssertions.then(pomWithCloud.getProperties().getProperty("spring-cloud.version"))
				.isEqualTo("Finchley.SNAPSHOT");
		BDDAssertions.then(pomWithCloud.getParent().getVersion()).isEqualTo("2.0.4.RELEASE");
		Iterator<RevCommit> iterator = git.log().call().iterator();
		RevCommit commit = iterator.next();
		BDDAssertions.then(commit.getShortMessage()).isEqualTo(
				"Updated versions after [Finchley.SR1] release train and [2.0.1.RELEASE] [spring-cloud-sleuth] project release");
		thenGradleUpdaterWasCalled();
		BDDAssertions.then(postReleaseProjects.get().forName("spring-boot-dependencies").version)
				.isEqualTo("2.0.4.RELEASE");
	}

	@Test
	public void should_assume_that_project_version_is_snapshot_when_no_pom_is_present() throws Exception {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		properties.getMetaRelease().setEnabled(true);
		properties.getGit().setUpdateAllTestSamples(true);
		properties.getGit().getAllTestSampleUrls().clear();
		properties.getGit().getAllTestSampleUrls().put("spring-cloud-sleuth",
				Collections.singletonList(tmpFile("spring-cloud-static/").getAbsolutePath() + "/"));
		AtomicReference<Projects> postReleaseProjects = new AtomicReference<>();
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, properties, versionsFetcher, releaserPropertiesUpdater) {
			@Override
			Projects getPostReleaseProjects(Projects projects) {
				postReleaseProjects.set(super.getPostReleaseProjects(projects));
				return postReleaseProjects.get();
			}
		};

		actions.updateAllTestSamples(currentGa());

		this.clonedTestProjects.entrySet().stream().filter(s -> s.getKey().contains("spring-cloud-static")).findFirst()
				.orElseThrow(() -> new IllegalStateException("Not found"));
	}

	@Test
	public void should_do_nothing_when_guides_are_turned_off() {
		this.properties.getGit().setUpdateSpringGuides(false);
		VersionsFetcher versionsFetcher = BDDMockito.mock(VersionsFetcher.class);
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater);

		actions.deployGuides(Collections.emptyList());

		BDDAssertions.then(this.cloned).isNull();
		BDDMockito.then(versionsFetcher).shouldHaveNoInteractions();
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
		ProjectCommandExecutor projectCommandExecutor = BDDMockito.mock(ProjectCommandExecutor.class);
		PostReleaseActions actions = new PostReleaseActions(handler, this.updater, this.gradleUpdater,
				this.commandExecutor, this.properties, versionsFetcher, releaserPropertiesUpdater) {
			@Override
			ProjectCommandExecutor projectBuilder(ProcessedProject processedProject) {
				projectBuilderStub.set(projectCommandExecutor);
				return projectCommandExecutor;
			}
		};
		ProjectVersion projectVersion = new ProjectVersion(new File(projects, "spring-cloud-release"));

		actions.deployGuides(
				Collections.singletonList(new ProcessedProject(this.properties, projectVersion, projectVersion)));

		Awaitility.await().untilAsserted(() -> {
			BDDAssertions.then(projectBuilderStub.get()).isNotNull();
			BDDMockito.then(projectBuilderStub.get()).should().deployGuides(this.properties, projectVersion,
					projectVersion);
		});
	}

	private String sleuthParentPomVersion() {
		return PomReader.readPom(new File(this.cloned, "sleuth/pom.xml")).getParent().getVersion();
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
