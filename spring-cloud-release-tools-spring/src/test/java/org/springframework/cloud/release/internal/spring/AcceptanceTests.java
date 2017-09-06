package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Iterator;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.gradle.GradleUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.TestPomReader;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class AcceptanceTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	TestPomReader testPomReader = new TestPomReader();
	File springCloudConsulProject;
	File temporaryFolder;
	TestProjectGitHandler gitHandler;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(AcceptanceTests.class.getResource("/projects/spring-cloud-consul").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_fail_to_perform_a_release_of_consul_when_sc_release_contains_snapshots() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaserWithSnapshotScRelease(project, "vCamden.SR5.BROKEN", "1.1.2.RELEASE");

		BDDAssertions.thenThrownBy(releaser::release)
				.hasMessageContaining("there is at least one SNAPSHOT library version in the Spring Cloud Release project");
	}

	@Test
	public void should_perform_a_release_of_consul() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "vCamden.SR5", "1.1.2.RELEASE");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		Iterator<RevCommit> iterator = commits.iterator();
		tagIsPresentInOrigin(origin, "v1.1.2.RELEASE");
		commitIsPresent(iterator, "Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.1.2.RELEASE");
		pomVersionIsEqualTo(project, "1.2.1.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(project, "1.2.1.BUILD-SNAPSHOT");
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
	}

	@Test
	public void should_perform_a_release_of_consul_rc1() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = releaser(project, "Dalston.RC1", "1.2.0.RC1");

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		tagIsPresentInOrigin(origin, "v1.2.0.RC1");
		commitIsNotPresent(commits, "Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
		Iterator<RevCommit> iterator = listOfCommits(project).iterator();
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.2.0.RC1");
		pomVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(project, "1.2.0.BUILD-SNAPSHOT");
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
	}

	@Test
	public void should_generate_templates_only() throws Exception {
		File origin = GitTestUtils.clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = GitTestUtils.clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		GitTestUtils.setOriginOnProjectToTmp(origin, project);
		SpringReleaser releaser = templateOnlyReleaser(project, "Dalston.RC1", "1.2.0.RC1");

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
	}

	private Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return GitTestUtils.openGitProject(project).log().call();
	}

	private void pomParentVersionIsEqualTo(File project, String expected) {
		then(pom(new File(project, "spring-cloud-starter-consul")).getParent()
				.getVersion()).isEqualTo(expected);
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

	private SpringReleaser releaser(File projectFile, String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile, branch);
		return releaserWithFullDeployment(expectedVersion, properties);
	}

	private SpringReleaser releaserWithFullDeployment(String expectedVersion,
			ReleaserProperties properties) throws Exception {
		Releaser releaser = defaultReleaser(expectedVersion, properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser, properties) {
			@Override String chosenOption() {
				return "0";
			}
		});
	}

	private SpringReleaser releaserWithSnapshotScRelease(File projectFile, String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = snapshotScReleaseReleaserProperties(projectFile, branch);
		return releaserWithFullDeployment(expectedVersion, properties);
	}

	private SpringReleaser templateOnlyReleaser(File projectFile, String branch, String expectedVersion) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile, branch);
		Releaser releaser = defaultReleaser(expectedVersion, properties);
		return new SpringReleaser(releaser, properties, new OptionsProcessor(releaser, properties) {
			@Override String chosenOption() {
				return "10";
			}
		});
	}

	private Releaser defaultReleaser(String expectedVersion, ReleaserProperties properties) throws Exception {
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties);
		ProjectBuilder projectBuilder = new ProjectBuilder(properties, pomUpdater);
		TestProjectGitHandler handler = new TestProjectGitHandler(properties,
				expectedVersion);
		TemplateGenerator templateGenerator = new TemplateGenerator(properties, handler);
		GradleUpdater gradleUpdater = new GradleUpdater(properties);
		Releaser releaser = new Releaser(pomUpdater, projectBuilder, handler,
				templateGenerator, gradleUpdater);
		this.gitHandler = handler;
		return releaser;
	}

	private ReleaserProperties releaserProperties(File project, String branch) throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release/").toURI().getPath());
		releaserProperties.getPom().setBranch(branch);
		releaserProperties.setWorkingDir(project.getPath());
		releaserProperties.getMaven().setBuildCommand("touch build");
		releaserProperties.getMaven().setDeployCommand("touch deploy");
		releaserProperties.getMaven().setPublishDocsCommands(new String[] { "touch docs"} );
		return releaserProperties;
	}

	private ReleaserProperties snapshotScReleaseReleaserProperties(File project, String branch) throws URISyntaxException {
		ReleaserProperties releaserProperties = releaserProperties(project, branch);
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release-with-snapshot/").toURI().getPath());
		return releaserProperties;
	}

	class TestProjectGitHandler extends ProjectGitHandler {

		boolean closedMilestones = false;
		final String expectedVersion;

		public TestProjectGitHandler(ReleaserProperties properties,
				String expectedVersion) {
			super(properties);
			this.expectedVersion = expectedVersion;
		}

		@Override public void closeMilestone(ProjectVersion releaseVersion) {
			then(releaseVersion.projectName).isEqualTo("spring-cloud-consul");
			then(releaseVersion.version).isEqualTo(this.expectedVersion);
			this.closedMilestones = true;
		}

		@Override public String milestoneUrl(ProjectVersion releaseVersion) {
			return "http://foo.bar.com/" + releaseVersion.toString();
		}
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(AcceptanceTests.class.getResource(relativePath).toURI());
	}
}
