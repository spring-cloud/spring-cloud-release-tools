package org.springframework.cloud.release.internal.spring;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Matchers.anyString;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.gradle.GradleUpdater;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.pom.TestPomReader;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.cloud.release.internal.sagan.Project;
import org.springframework.cloud.release.internal.sagan.Release;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class AcceptanceTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	@Rule public OutputCapture capture = new OutputCapture();
	TestPomReader testPomReader = new TestPomReader();
	File springCloudConsulProject;
	File temporaryFolder;
	File documentationFolder;
	TestProjectGitHandler gitHandler;
	NonAssertingTestProjectGitHandler nonAssertingGitHandler;
	SaganClient saganClient = Mockito.mock(SaganClient.class);
	ReleaserProperties releaserProperties;
	TemplateGenerator templateGenerator;
	SaganUpdater saganUpdater;
	DocumentationUpdater documentationUpdater;
	ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
	ReleaserPropertiesUpdater updater = new ReleaserPropertiesUpdater(this.applicationContext);

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(AcceptanceTests.class.getResource("/projects/spring-cloud-consul").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
		BDDMockito.given(this.saganClient.getProject(anyString()))
				.willReturn(newProject());
	}

	private Project newProject() {
		Project project = new Project();
		project.projectReleases.addAll(Arrays.asList(
				release("1.0.0.M8"),
				release("1.1.0.M8"),
				release("1.2.0.M8"),
				release("2.0.0.M8"))
		);
		return project;
	}

	private Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}

	@Test
	public void should_fail_to_perform_a_release_of_consul_when_sc_release_contains_snapshots() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaserWithSnapshotScRelease(project, "spring-cloud-consul",
				"vCamden.SR5.BROKEN", "1.1.2.RELEASE");

		BDDAssertions.thenThrownBy(releaser::release)
				.hasMessageContaining("there is at least one SNAPSHOT library version in the Spring Cloud Release project");
	}

	@Test
	public void should_not_clone_when_option_not_to_clone_was_switched_on() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = templateOnlyReleaser(project, "spring-cloud-consul",
				"vCamden.SR5", "1.1.2.RELEASE");
		this.releaserProperties.getGit().setFetchVersionsFromGit(false);
		this.releaserProperties.getFixedVersions().put("spring-cloud-release", "Finchley.RELEASE");
		this.releaserProperties.getFixedVersions().put("spring-cloud-consul", "2.3.4.RELEASE");
		File temporaryDestination = tmp.newFolder();
		this.releaserProperties.getGit().setCloneDestinationDir(temporaryDestination.getAbsolutePath());

		releaser.release();

		then(temporaryDestination.list()).isEmpty();
	}

	@Test
	public void should_perform_a_release_of_consul() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "spring-cloud-consul","vCamden.SR5", "1.1.2.RELEASE");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		Iterator<RevCommit> iterator = commits.iterator();
		tagIsPresentInOrigin(origin, "v1.1.2.RELEASE");
		commitIsPresent(iterator, "Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.1.2.RELEASE");
		pomVersionIsEqualTo(project, "1.2.1.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(project, "1.2.1.BUILD-SNAPSHOT");
		then(this.gitHandler.closedMilestones).isTrue();
		then(emailTemplate()).exists();
		then(emailTemplateContents())
				.contains("Spring Cloud Camden.SR5 available")
				.contains("Spring Cloud Camden SR5 Train release");
		then(blogTemplate()).exists();
		then(blogTemplateContents())
				.contains("I am pleased to announce that the Service Release 5 (SR5)");
		then(releaseNotesTemplate()).exists();
		then(releaseNotesTemplateContents())
				.contains("Camden.SR5")
				.contains("- Spring Cloud Config `1.2.2.RELEASE` ([issues](http://foo.bar.com/1.2.2.RELEASE))")
				.contains("- Spring Cloud Aws `1.1.3.RELEASE` ([issues](http://foo.bar.com/1.1.3.RELEASE))");
		// once for updating GA
		// second time to update SNAPSHOT
		BDDMockito.then(this.saganClient).should(BDDMockito.times(2)).updateRelease(BDDMockito.eq("spring-cloud-consul"),
				BDDMockito.anyList());
		BDDMockito.then(this.saganClient).should().deleteRelease("spring-cloud-consul",
				"1.1.2.BUILD-SNAPSHOT");
		BDDMockito.then(this.saganClient).should().deleteRelease("spring-cloud-consul",
				"1.1.0.M8");
		BDDMockito.then(this.saganClient).should(BDDMockito.never())
				.deleteRelease("spring-cloud-build", "1.0.0.M8");
		BDDMockito.then(this.saganClient).should(BDDMockito.never())
				.deleteRelease("spring-cloud-build", "2.0.0.M8");
		then(this.gitHandler.issueCreatedInSpringGuides).isTrue();
		then(text(new File(this.documentationFolder, "current/index.html")))
				.doesNotContain("Angel.SR3")
				.contains("Camden.SR5");
	}

	@Test
	public void should_perform_a_meta_release_of_sc_release_and_consul() throws Exception {
		// simulates an org
		Map<String, String> versions = new HashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		SpringReleaser releaser = metaReleaser(versions);

		releaser.release(new OptionsBuilder().metaRelease(true).options());

		then(this.nonAssertingGitHandler.clonedProjects).hasSize(2);
		this.nonAssertingGitHandler.clonedProjects
				.forEach(project -> {
					then(Arrays.asList("spring-cloud-starter-build",
							"spring-cloud-consul")).contains(pom(project).getArtifactId());
					then(capture.toString()).contains("executed_build", "executed_deploy", "executed_docs");
				});
		BDDMockito.then(saganUpdater).should(BDDMockito.atLeastOnce()).updateSagan(BDDMockito.anyString(),
				BDDMockito.any(ProjectVersion.class), BDDMockito.any(ProjectVersion.class));
		BDDMockito.then(documentationUpdater).should()
				.updateDocsRepo(BDDMockito.any(ProjectVersion.class), BDDMockito.anyString());
	}

	@Test
	public void should_perform_a_meta_release_of_consul_only_when_run_from_got_passed() throws Exception {
		// simulates an org
		Map<String, String> versions = new HashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-build", "1.1.2.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		SpringReleaser releaser = metaReleaser(versions);

		releaser.release(new OptionsBuilder().metaRelease(true)
				.startFrom("spring-cloud-consul")
				.options());

		then(this.nonAssertingGitHandler.clonedProjects).hasSize(1);
		this.nonAssertingGitHandler.clonedProjects
				.forEach(project -> {
					then(pom(project).getArtifactId()).isEqualTo("spring-cloud-consul");
					then(capture.toString()).contains("executed_build", "executed_deploy", "executed_docs");
				});
		BDDMockito.then(saganUpdater).should(BDDMockito.atLeastOnce()).updateSagan(BDDMockito.anyString(),
				BDDMockito.any(ProjectVersion.class), BDDMockito.any(ProjectVersion.class));
		BDDMockito.then(documentationUpdater).should()
				.updateDocsRepo(BDDMockito.any(ProjectVersion.class), BDDMockito.anyString());
	}

	@Test
	public void should_perform_a_meta_release_of_build_and_consul_only_when_task_names_got_passed() throws Exception {
		// simulates an org
		Map<String, String> versions = new HashMap<>();
		versions.put("spring-cloud-release", "Camden.BUILD-SNAPSHOT");
		versions.put("spring-cloud-build", "1.1.2.BUILD-SNAPSHOT");
		versions.put("spring-cloud-consul", "1.1.2.BUILD-SNAPSHOT");
		SpringReleaser releaser = metaReleaser(versions);

		releaser.release(new OptionsBuilder().metaRelease(true)
				.taskNames(Arrays.asList("spring-cloud-build", "spring-cloud-consul"))
				.options());

		then(this.nonAssertingGitHandler.clonedProjects).hasSize(2);
		this.nonAssertingGitHandler.clonedProjects
				.forEach(project -> {
					then(Arrays.asList("spring-cloud-build",
							"spring-cloud-consul")).contains(pom(project).getArtifactId());
					then(capture.toString()).contains("executed_build", "executed_deploy", "executed_docs");
				});
		BDDMockito.then(saganUpdater).should(BDDMockito.atLeastOnce()).updateSagan(BDDMockito.anyString(),
				BDDMockito.any(ProjectVersion.class), BDDMockito.any(ProjectVersion.class));
		BDDMockito.then(documentationUpdater).should()
				.updateDocsRepo(BDDMockito.any(ProjectVersion.class), BDDMockito.anyString());
	}

	// issue #74
	@Test
	public void should_perform_a_release_of_sc_build() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(),
				new File(AcceptanceTests.class.getResource("/projects/spring-cloud-build").toURI()));
		pomVersionIsEqualTo(origin, "1.3.7.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "spring-cloud-build-dependencies", "1.5.9.RELEASE");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-build"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "spring-cloud-build",
				"vCamden.SR5", "1.2.2.RELEASE");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		Iterator<RevCommit> iterator = commits.iterator();
		tagIsPresentInOrigin(origin, "v1.2.2.RELEASE");
		// we're running against camden sc-release
		commitIsPresent(iterator, "Bumping versions to 1.3.8.BUILD-SNAPSHOT after release");
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.2.2.RELEASE");
		pomVersionIsEqualTo(project, "1.3.8.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(project, "spring-cloud-build-dependencies", "1.4.4.RELEASE");
		then(this.gitHandler.closedMilestones).isTrue();
		then(emailTemplate()).exists();
		then(blogTemplate()).exists();
		then(releaseNotesTemplate()).exists();
		// once for updating GA
		// second time to update SNAPSHOT
		BDDMockito.then(this.saganClient).should(BDDMockito.times(2)).updateRelease(BDDMockito.eq("spring-cloud-build"),
				BDDMockito.anyList());
		BDDMockito.then(this.saganClient).should()
				.deleteRelease("spring-cloud-build", "1.2.2.BUILD-SNAPSHOT");
		BDDMockito.then(this.saganClient).should()
				.deleteRelease("spring-cloud-build", "1.2.0.M8");
		BDDMockito.then(this.saganClient).should(BDDMockito.never())
				.deleteRelease("spring-cloud-build", "1.1.0.M8");
		BDDMockito.then(this.saganClient).should(BDDMockito.never())
				.deleteRelease("spring-cloud-build", "2.0.0.M8");
		then(this.gitHandler.issueCreatedInSpringGuides).isTrue();
		then(text(new File(this.documentationFolder, "current/index.html")))
				.doesNotContain("Angel.SR3")
				.contains("Camden.SR5");
	}

	@Test
	public void should_perform_a_release_of_consul_rc1() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "spring-cloud-consul", "Dalston.RC1", "1.2.0.RC1");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		tagIsPresentInOrigin(origin, "v1.2.0.RC1");
		commitIsNotPresent(commits, "Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
		Iterator<RevCommit> iterator = listOfCommits(project).iterator();
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.2.0.RC1");
		pomVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
		then(this.gitHandler.closedMilestones).isTrue();
		then(emailTemplate()).exists();
		then(emailTemplateContents())
				.contains("Spring Cloud Dalston.RC1 available")
				.contains("Spring Cloud Dalston RC1 Train release");
		then(blogTemplate()).exists();
		then(blogTemplateContents())
				.contains("I am pleased to announce that the Release Candidate 1 (RC1)");
		then(tweetTemplate()).exists();
		then(tweetTemplateContents())
				.contains("The Dalston.RC1 version of @springcloud has been released!");
		then(releaseNotesTemplate()).exists();
		then(releaseNotesTemplateContents())
				.contains("Dalston.RC1")
				.contains("- Spring Cloud Build `1.3.1.RELEASE` ([issues](http://foo.bar.com/1.3.1.RELEASE))")
				.contains("- Spring Cloud Bus `1.3.0.M1` ([issues](http://foo.bar.com/1.3.0.M1))");
		BDDMockito.then(this.saganClient).should().updateRelease(BDDMockito.eq("spring-cloud-consul"),
				BDDMockito.anyList());
		BDDMockito.then(this.saganClient).should()
				.deleteRelease("spring-cloud-consul","1.2.0.M8");
		BDDMockito.then(this.saganClient).should()
				.deleteRelease("spring-cloud-consul","1.2.0.RC1");
		// we update guides only for SR / RELEASE
		then(this.gitHandler.issueCreatedInSpringGuides).isFalse();
		// haven't even checked out the branch
		then(new File(this.documentationFolder, "current/index.html"))
				.doesNotExist();
	}

	@Test
	public void should_generate_templates_only() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = templateOnlyReleaser(project, "spring-cloud-consul","Dalston.RC1", "1.2.0.RC1");

		releaser.release();

		then(this.gitHandler.closedMilestones).isFalse();
		then(emailTemplate()).exists();
		then(emailTemplateContents())
				.contains("Spring Cloud Dalston.RC1 available")
				.contains("Spring Cloud Dalston RC1 Train release");
		then(blogTemplate()).exists();
		then(blogTemplateContents())
				.contains("I am pleased to announce that the Release Candidate 1 (RC1)");
		then(tweetTemplate()).exists();
		then(tweetTemplateContents())
				.contains("The Dalston.RC1 version of @springcloud has been released!");
		then(releaseNotesTemplate()).exists();
		then(releaseNotesTemplateContents())
				.contains("Dalston.RC1")
				.contains("- Spring Cloud Build `1.3.1.RELEASE` ([issues](http://foo.bar.com/1.3.1.RELEASE))")
				.contains("- Spring Cloud Bus `1.3.0.M1` ([issues](http://foo.bar.com/1.3.0.M1)");
		BDDMockito.then(this.saganClient).should(BDDMockito.never()).updateRelease(
				BDDMockito.anyString(), BDDMockito.anyList());
		then(this.gitHandler.issueCreatedInSpringGuides).isFalse();
	}

	private Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return GitTestUtils.openGitProject(project).log().call();
	}

	private void pomParentVersionIsEqualTo(File project, String child, String expected) {
		then(pom(new File(project, child)).getParent()
				.getVersion()).isEqualTo(expected);
	}

	private void consulPomParentVersionIsEqualTo(File project, String expected) {
		pomParentVersionIsEqualTo(project, "spring-cloud-starter-consul", expected);
	}

	private void pomVersionIsEqualTo(File project, String expected) {
		then(pom(project).getVersion()).isEqualTo(expected);
	}

	private void commitIsPresent(Iterator<RevCommit> iterator,
			String expected) {
		RevCommit commit = iterator.next();
		then(commit.getShortMessage()).isEqualTo(expected);
	}

	private void commitIsNotPresent(Iterable<RevCommit> commits,
			String expected) {
		for (RevCommit commit : commits) {
			then(commit.getShortMessage()).isNotEqualTo(expected);
		}
	}

	private void tagIsPresentInOrigin(File origin, String expectedTag) throws GitAPIException {
		then(GitTestUtils.openGitProject(origin).tagList()
						.call().iterator().next().getName()).endsWith(expectedTag);
	}

	private Model pom(File dir) {
		return this.testPomReader.readPom(new File(dir, "pom.xml"));
	}

	private File emailTemplate() throws URISyntaxException {
		return new File("target/email.txt");
	}

	private String emailTemplateContents() throws URISyntaxException, IOException {
		return new String(Files.readAllBytes(emailTemplate().toPath()));
	}

	private File blogTemplate() throws URISyntaxException {
		return new File("target/blog.md");
	}

	private File tweetTemplate() throws URISyntaxException {
		return new File("target/tweet.txt");
	}

	private File releaseNotesTemplate() throws URISyntaxException {
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

	private SpringReleaser releaser(File projectFile, String projectName,
			String branch, String expectedVersion) throws Exception {
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
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser, properties) {
			@Override String chosenOption() {
				return "0";
			}

			@Override void postReleaseOptions(Options options, Args defaultArgs) {
				options.interactive = false;
				super.postReleaseOptions(options, defaultArgs);
			}
		}, updater);
	}

	private SpringReleaser metaReleaserWithFullDeployment(ReleaserProperties properties) throws Exception {
		Releaser releaser = defaultMetaReleaser(properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser, properties) {
			@Override String chosenOption() {
				return "0";
			}

			@Override void postReleaseOptions(Options options, Args defaultArgs) {
				options.interactive = false;
				super.postReleaseOptions(options, defaultArgs);
			}
		}, updater);
	}

	private SpringReleaser releaserWithSnapshotScRelease(File projectFile, String projectName,
			String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = snapshotScReleaseReleaserProperties(projectFile, branch);
		return releaserWithFullDeployment(expectedVersion, projectName, properties);
	}

	private SpringReleaser templateOnlyReleaser(File projectFile, String projectName, String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile, branch);
		Releaser releaser = defaultReleaser(expectedVersion, projectName, properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser, properties) {
			@Override String chosenOption() {
				return "12";
			}

			@Override void postReleaseOptions(Options options, Args defaultArgs) {
				options.interactive = true;
				super.postReleaseOptions(options, defaultArgs);
			}
		}, updater);
	}

	private Releaser defaultReleaser(String expectedVersion, String projectName,
			ReleaserProperties properties) throws Exception {
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties);
		ProjectBuilder projectBuilder = new ProjectBuilder(properties);
		TestProjectGitHandler handler = new TestProjectGitHandler(properties,
				expectedVersion, projectName);
		TemplateGenerator templateGenerator = new TemplateGenerator(properties, handler);
		GradleUpdater gradleUpdater = new GradleUpdater(properties);
		SaganUpdater saganUpdater = new SaganUpdater(this.saganClient);
		DocumentationUpdater documentationUpdater = new DocumentationUpdater(handler) {
			@Override public File updateDocsRepo(ProjectVersion currentProject,
					String springCloudReleaseBranch) {
				File file = super.updateDocsRepo(currentProject, springCloudReleaseBranch);
				documentationFolder = file;
				return file;
			}
		};
		Releaser releaser = new Releaser(pomUpdater, projectBuilder, handler,
				templateGenerator, gradleUpdater, saganUpdater, documentationUpdater);
		this.gitHandler = handler;
		return releaser;
	}

	private Releaser defaultMetaReleaser(ReleaserProperties properties) throws Exception {
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties);
		ProjectBuilder projectBuilder = new ProjectBuilder(properties);
		NonAssertingTestProjectGitHandler handler = new NonAssertingTestProjectGitHandler(properties);
		TemplateGenerator templateGenerator = Mockito.spy(new TemplateGenerator(properties, handler));
		GradleUpdater gradleUpdater = new GradleUpdater(properties);
		SaganUpdater saganUpdater = Mockito.spy(new SaganUpdater(this.saganClient));
		DocumentationUpdater documentationUpdater = Mockito.spy(new DocumentationUpdater(handler) {
			@Override public File updateDocsRepo(ProjectVersion currentProject,
					String springCloudReleaseBranch) {
				File file = super.updateDocsRepo(currentProject, springCloudReleaseBranch);
				documentationFolder = file;
				return file;
			}
		});
		Releaser releaser = Mockito.spy(new Releaser(pomUpdater, projectBuilder, handler,
				templateGenerator, gradleUpdater, saganUpdater, documentationUpdater));
		this.nonAssertingGitHandler = handler;
		this.templateGenerator = templateGenerator;
		this.saganUpdater = saganUpdater;
		this.documentationUpdater = documentationUpdater;
		return releaser;
	}

	private ReleaserProperties releaserProperties(File project, String branch) throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release/").toURI().getPath());
		releaserProperties.getGit().setDocumentationUrl(file("/projects/spring-cloud-static-angel/").toURI().getPath());
		releaserProperties.getMaven().setBuildCommand("echo build");
		releaserProperties.getMaven().setDeployCommand("echo deploy");
		releaserProperties.getMaven().setPublishDocsCommands(new String[] { "echo docs"} );
		releaserProperties.setWorkingDir(project.getPath());
		releaserProperties.getPom().setBranch(branch);
		this.releaserProperties = releaserProperties;
		return releaserProperties;
	}

	private ReleaserProperties metaReleaserProperties(Map<String, String> versions) throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setDocumentationUrl(file("/projects/spring-cloud-static-angel/").toURI().getPath());
		releaserProperties.getMaven().setBuildCommand("echo executed_build");
		releaserProperties.getMaven().setDeployCommand("echo executed_deploy");
		releaserProperties.getMaven().setPublishDocsCommands(new String[] { "echo executed_docs"} );
		releaserProperties.getMetaRelease().setGitOrgUrl("file://" + this.temporaryFolder.getAbsolutePath());
		releaserProperties.getMetaRelease().setEnabled(true);
		releaserProperties.setFixedVersions(versions);
		this.releaserProperties = releaserProperties;
		return releaserProperties;
	}

	private ReleaserProperties snapshotScReleaseReleaserProperties(File project, String branch) throws URISyntaxException {
		ReleaserProperties releaserProperties = releaserProperties(project, branch);
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release-with-snapshot/").toURI().getPath());
		releaserProperties.getGit().setDocumentationUrl(file("/projects/spring-cloud-static/").toURI().getPath());
		this.releaserProperties = releaserProperties;
		return releaserProperties;
	}

	class TestProjectGitHandler extends ProjectGitHandler {

		boolean closedMilestones = false;
		boolean issueCreatedInSpringGuides = false;
		final String expectedVersion;
		final String projectName;

		public TestProjectGitHandler(ReleaserProperties properties,
				String expectedVersion, String projectName) {
			super(properties);
			this.expectedVersion = expectedVersion;
			this.projectName = projectName;
		}

		@Override public void closeMilestone(ProjectVersion releaseVersion) {
			then(releaseVersion.projectName).isEqualTo(this.projectName);
			then(releaseVersion.version).isEqualTo(this.expectedVersion);
			this.closedMilestones = true;
		}

		@Override public void createIssueInSpringGuides(Projects projects,
				ProjectVersion version) {
			this.issueCreatedInSpringGuides = true;
		}

		@Override public String milestoneUrl(ProjectVersion releaseVersion) {
			return "http://foo.bar.com/" + releaseVersion.toString();
		}
	}

	class NonAssertingTestProjectGitHandler extends ProjectGitHandler {

		boolean closedMilestones = false;
		boolean issueCreatedInSpringGuides = false;
		List<File> clonedProjects = new ArrayList<>();

		public NonAssertingTestProjectGitHandler(ReleaserProperties properties) {
			super(properties);
		}

		@Override public void closeMilestone(ProjectVersion releaseVersion) {
			this.closedMilestones = true;
		}

		@Override public void createIssueInSpringGuides(Projects projects,
				ProjectVersion version) {
			this.issueCreatedInSpringGuides = true;
		}

		@Override public String milestoneUrl(ProjectVersion releaseVersion) {
			return "http://foo.bar.com/" + releaseVersion.toString();
		}

		@Override public File cloneScReleaseProject() {
			File file = super.cloneScReleaseProject();
			this.clonedProjects.add(file);
			return file;
		}

		@Override public File cloneDocumentationProject() {
			File file = super.cloneDocumentationProject();
			this.clonedProjects.add(file);
			return file;
		}

		@Override public File cloneProjectFromOrg(String projectName) {
			File file = super.cloneProjectFromOrg(projectName);
			this.clonedProjects.add(file);
			return file;
		}
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
}
