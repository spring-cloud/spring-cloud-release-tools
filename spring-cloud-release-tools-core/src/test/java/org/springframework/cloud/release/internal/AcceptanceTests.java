package org.springframework.cloud.release.internal;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.maven.model.Model;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import org.springframework.cloud.release.internal.project.ProjectBuilder;
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
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		File project = clonedProject(this.tmp.newFolder(), tmpFile("spring-cloud-consul"));
		setOriginOnProjectToTmp(origin, project);
		Releaser releaser = releaser(project);

		releaser.release();

		Iterable<RevCommit> commits = listOfCommits(project);
		Iterator<RevCommit> iterator = commits.iterator();
		tagIsPresentInOrigin(origin);
		commitIsPresent(iterator, "Bumping versions to 1.2.1.BUILD-SNAPSHOT after release");
		commitIsPresent(iterator, "Going back to snapshots");
		commitIsPresent(iterator, "Update SNAPSHOT to 1.1.2.RELEASE");
		pomVersionIsEqualTo(project, "1.2.1.BUILD-SNAPSHOT");
		pomParentVersionIsEqualTo(project, "1.2.1.BUILD-SNAPSHOT");
	}

	private Iterable<RevCommit> listOfCommits(File project) throws GitAPIException {
		return openGitProject(project).log().call();
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

	private void tagIsPresentInOrigin(File origin) throws GitAPIException {
		then(openGitProject(origin).tagList().call().iterator().next().getName()).endsWith("v1.1.2.RELEASE");
	}

	private Model pom(File dir) {
		return this.testPomReader.readPom(new File(dir, "pom.xml"));
	}

	private ReleaserProperties releaserProperties(File project) throws URISyntaxException {
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGit().setSpringCloudReleaseGitUrl(file("/projects/spring-cloud-release/").toURI().getPath());
		releaserProperties.getPom().setBranch("vCamden.SR5");
		releaserProperties.setWorkingDir(project.getPath());
		releaserProperties.getMaven().setBuildCommand("touch build");
		releaserProperties.getMaven().setDeployCommand("touch deploy");
		releaserProperties.getMaven().setPublishDocsCommands(new String[] { "touch docs"} );
		return releaserProperties;
	}

	private Releaser releaser(File projectFile) throws Exception {
		ReleaserProperties properties = releaserProperties(projectFile);
		ProjectPomUpdater pomUpdater = new ProjectPomUpdater(properties);
		ProjectBuilder projectBuilder = new ProjectBuilder(properties, pomUpdater);
		ProjectGitUpdater gitUpdater = new ProjectGitUpdater(properties);
		return new Releaser(properties, pomUpdater, projectBuilder, gitUpdater) {
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
}
