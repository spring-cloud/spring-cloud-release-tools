package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Iterator;

import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.release.internal.git.GitRepoTests;
import org.springframework.cloud.release.internal.git.ProjectGitUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.TestPomReader;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.cloud.release.internal.project.Project;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.release.internal.git.GitTestUtils.clonedProject;
import static org.springframework.cloud.release.internal.git.GitTestUtils.openGitProject;
import static org.springframework.cloud.release.internal.git.GitTestUtils.setOriginOnProjectToTmp;

/**
 * @author Marcin Grzejszczak
 */
public class AcceptanceTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	TestPomReader testPomReader = new TestPomReader();
	File springCloudConsulProject;
	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(GitRepoTests.class.getResource("/projects/spring-cloud-consul").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_perform_a_release_of_consul() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudConsulProject);
		File project = clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		setOriginOnProjectToTmp(origin, project);
		Releaser releaser = releaser(project);

		releaser.release();

		then(this.temporaryFolder).exists();
		File afterProcessing = new File(project, "bumped");
		then(afterProcessing).exists();
		Iterable<RevCommit> commits = openGitProject(project).log().call();
		Iterator<RevCommit> iterator = commits.iterator();
		RevCommit afterRelease = iterator.next();
		RevCommit goingBackToSnapshots = iterator.next();
		RevCommit bumping = iterator.next();
		then(openGitProject(origin).tagList().call().iterator().next().getName()).endsWith("v1.1.2.RELEASE");
		then(afterRelease.getShortMessage()).isEqualTo("Bumping versions after release");
		then(goingBackToSnapshots.getShortMessage()).isEqualTo("Going back to snapshots");
		then(bumping.getShortMessage()).isEqualTo("Bumping versions before release");
	}

	private ReleaserProperties releaserProperties(File project) throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release/").toURI().getPath());
		releaserProperties.getPom().setBranch("vCamden.SR5");
		releaserProperties.setWorkingDir(project.getPath());
		releaserProperties.getMaven().setBuildCommand("touch build");
		releaserProperties.getMaven().setDeployCommand("touch deploy");
		releaserProperties.getMaven().setPublishDocsCommands(new String[] { "touch docs"} );
		releaserProperties.getMaven().setBumpVersionsCommand("touch bumped");
		return releaserProperties;
	}

	private Releaser releaser(File projectFile) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile);
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties);
		Project project = new Project(properties);
		ProjectGitUpdater gitUpdater = new ProjectGitUpdater(properties);
		return new Releaser(properties, pomUpdater, project, gitUpdater) {
			@Override boolean skipStep() {
				return false;
			}
		};
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(AcceptanceTests.class.getResource(relativePath).toURI());
	}

	private File pom(String relativePath) throws URISyntaxException {
		return new File(new File(AcceptanceTests.class.getResource(relativePath).toURI()), "pom.xml");
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}
}
