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

package releaser.internal.spring;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import releaser.internal.ReleaserProperties;
import releaser.internal.buildsystem.TestPomReader;
import releaser.internal.buildsystem.TestUtils;
import releaser.internal.git.GitTestUtils;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.options.Options;
import releaser.internal.postrelease.PostReleaseActions;
import releaser.internal.project.Projects;
import releaser.internal.sagan.Project;
import releaser.internal.sagan.Release;
import releaser.internal.tasks.ReleaserTask;

import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public abstract class AbstractSpringAcceptanceTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	public File temporaryFolder;

	public TestPomReader testPomReader = new TestPomReader();

	public static Project newProject() {
		Project project = new Project();
		project.setReleases(
				Arrays.asList(release("1.0.0.M8"), release("1.1.0.M8"), release("1.2.0.M8"), release("2.0.0.M8")));
		return project;
	}

	public static Release release(String version) {
		Release release = new Release();
		release.setVersion(version);
		release.setCurrent(true);
		return release;
	}

	@BeforeEach
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
		clean();
	}

	@After
	public void cleanup() throws Exception {
		clean();
	}

	public void clean() {
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
		BDDMockito.then(postReleaseActions).should().runUpdatedTests(BDDMockito.any(Projects.class));
	}

	public void thenRunUpdatedTestsWereNotCalled(PostReleaseActions postReleaseActions) {
		BDDMockito.then(postReleaseActions).should(BDDMockito.never()).runUpdatedTests(BDDMockito.any(Projects.class));
	}

	public Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return GitTestUtils.openGitProject(project).log().call();
	}

	public void pomParentVersionIsEqualTo(File project, String child, String expected) {
		then(pom(new File(project, child)).getParent().getVersion()).isEqualTo(expected);
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

	public void tagIsPresentInOrigin(File origin, String expectedTag) throws GitAPIException {
		then(GitTestUtils.openGitProject(origin).tagList().call().iterator().next().getName()).endsWith(expectedTag);
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
		return new File(AbstractSpringAcceptanceTests.class.getResource(relativePath).toURI());
	}

	public String text(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	public File cloneToTemporaryDirectory(File project) throws IOException {
		return GitTestUtils.clonedProject(this.tmp.newFolder(), project);
	}

	public void checkoutReleaseTrainBranch(String fileToRepo, String branch)
			throws GitAPIException, URISyntaxException {
		File file = file(fileToRepo);
		Git git = GitTestUtils.openGitProject(file);
		git.reset().setMode(ResetCommand.ResetType.HARD).call();
		if (new File(file, ".travis.yml").exists()) {
			new File(file, ".travis.yml").delete();
		}
		git.checkout().setForce(true).setName(branch).call();
	}

	public Git clonedProject(NonAssertingTestProjectGitHandler handler, String name) {
		return GitTestUtils.openGitProject(
				handler.clonedProjects.stream().filter(file -> file.getName().equals(name)).findFirst().get());
	}

	public void thenUpdateReleaseTrainDocsWasCalled(PostReleaseActions actions) {
		BDDMockito.then(actions).should().generateReleaseTrainDocumentation(BDDMockito.any(Projects.class));
	}

	public void thenUpdateReleaseTrainDocsWasNotCalled(PostReleaseActions actions) {
		BDDMockito.then(actions).should(BDDMockito.never())
				.generateReleaseTrainDocumentation(BDDMockito.any(Projects.class));
	}

	public Props properties(String... args) {
		return new Props(args);
	}

	public void run(SpringApplicationBuilder application, Props properties,
			CatchingConsumer<ConfigurableApplicationContext> consumer) {
		try (ConfigurableApplicationContext context = application.build().run(properties.toCommandLineProps())) {
			consumer.accept(context);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public interface CatchingConsumer<T> {

		void accept(T t) throws Exception;

	}

	public static class NonAssertingTestProjectGitHandler extends ProjectGitHandler {

		private final Consumer<File> docsConsumer;

		public Set<File> clonedProjects = new HashSet<>();

		public NonAssertingTestProjectGitHandler(ReleaserProperties properties, Consumer<File> docsConsumer) {
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

	public static class TestExecutionResultHandler extends SpringBatchExecutionResultHandler {

		public boolean exitedSuccessOrUnstable;

		public boolean exitedWithException;

		public TestExecutionResultHandler(BuildReportHandler buildReportHandler,
				ConfigurableApplicationContext context) {
			super(buildReportHandler, context);
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
		SpringBatchFlowRunner mySpringBatchFlowRunner(StepBuilderFactory stepBuilderFactory,
				JobBuilderFactory jobBuilderFactory, ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher,
				FlowRunnerTaskExecutorSupplier flowRunnerTaskExecutorSupplier, ConfigurableApplicationContext context,
				ReleaserProperties releaserProperties, BuildReportHandler reportHandler) {
			return new SpringBatchFlowRunner(stepBuilderFactory, jobBuilderFactory, projectsToRunFactory, jobLauncher,
					flowRunnerTaskExecutorSupplier, context, releaserProperties, reportHandler) {
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
		TestExecutionResultHandler testExecutionResultHandler(BuildReportHandler buildReportHandler,
				ConfigurableApplicationContext context) {
			return new TestExecutionResultHandler(buildReportHandler, context);
		}

	}

	public class Props {

		private List<String> args = new LinkedList<>();

		public Props(String... args) {
			properties(args);
		}

		public Props properties(String... args) {
			this.args.addAll(Arrays.asList(args));
			return this;
		}

		public String[] toCommandLineProps() {
			return this.args.stream().map(s -> "--" + s).collect(Collectors.toList()).toArray(new String[0]);
		}

	}

}
