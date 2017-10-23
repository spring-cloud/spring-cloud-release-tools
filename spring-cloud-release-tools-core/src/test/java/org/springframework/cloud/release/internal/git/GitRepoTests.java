package org.springframework.cloud.release.internal.git;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.springframework.cloud.release.internal.git.GitTestUtils.clonedProject;
import static org.springframework.cloud.release.internal.git.GitTestUtils.setOriginOnProjectToTmp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.release.internal.pom.TestUtils;

/**
 * @author Marcin Grzejszczak
 */
public class GitRepoTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File springCloudReleaseProject;
	File tmpFolder;
	GitRepo gitRepo;

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.springCloudReleaseProject = new File(GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI());
		TestUtils.prepareLocalRepo();
		this.gitRepo = new GitRepo(this.tmpFolder);
	}

	@Test
	public void should_clone_the_project_from_a_given_location() throws IOException {
		this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());

		then(new File(this.tmpFolder, ".git")).exists();
	}

	@Test
	public void should_throw_exception_when_there_is_no_repo() throws IOException, URISyntaxException {
		thenThrownBy(() -> this.gitRepo
				.cloneProject(GitRepoTests.class.getResource("/projects/").toURI()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Exception occurred while cloning repo");
	}

	@Test
	public void should_throw_an_exception_when_failed_to_initialize_the_repo() throws IOException {
		thenThrownBy(() ->  new GitRepo(this.tmpFolder, new ExceptionThrowingJGitFactory()).cloneProject(this.springCloudReleaseProject.toURI()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Exception occurred while cloning repo")
				.hasCauseInstanceOf(CustomException.class);
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo() throws IOException {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		this.gitRepo.checkout(project, "vCamden.SR3");

		File pom = new File(this.tmpFolder, "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath()).anyMatch(s -> s.contains("<version>Camden.SR3</version>"))).isTrue();
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo2() throws IOException {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		this.gitRepo.checkout(project, "Camden.x");

		File pom = new File(this.tmpFolder, "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath()).anyMatch(s -> s.contains("<version>Camden.BUILD-SNAPSHOT</version>"))).isTrue();
	}

	@Test
	public void should_throw_an_exception_when_checking_out_nonexisting_branch() throws IOException {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		try {
			this.gitRepo.checkout(project, "nonExistingBranch");
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			then(e).hasMessageContaining("Ref nonExistingBranch can not be resolved");
		}
	}

	@Test
	public void should_commit_changes() throws Exception {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		createNewFile(project);

		this.gitRepo.commit(project, "some message");

		try(Git git = openGitProject(project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	@Test
	public void should_not_commit_empty_changes() throws Exception {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		createNewFile(project);
		this.gitRepo.commit(project, "some message");

		this.gitRepo.commit(project, "empty commit");

		try(Git git = openGitProject(project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isNotEqualTo("empty commit");
		}
	}

	@Test
	public void should_create_a_tag() throws Exception {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		createNewFile(project);
		this.gitRepo.commit(project, "some message");

		this.gitRepo.tag(project, "v1.0.0");

		try(Git git = openGitProject(project)) {
			tagIsPresent(git, "v1.0.0");
		}
	}

	private void tagIsPresent(Git git, String tag) throws GitAPIException {
		List<Ref> refs = git.tagList().call();
		System.out.println("All tags" + refs);
		then(refs.stream().anyMatch(ref -> ref.getName().startsWith("refs/tags/" + tag))).isTrue();
	}

	@Test
	public void should_push_changes_to_master_branch() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);
		this.gitRepo.commit(project, "some message");

		this.gitRepo.pushBranch(project, "master");

		try(Git git = openGitProject(origin)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	@Test
	public void should_push_changes_to_current_branch() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);
		this.gitRepo.commit(project, "some message");

		this.gitRepo.pushCurrentBranch(project);

		try(Git git = openGitProject(origin)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	@Test
	public void should_return_the_branch_name() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);

		String branch = this.gitRepo.currentBranch(project);

		then(branch).isEqualTo("master");
	}

	@Test
	public void should_push_a_tag_to_new_branch_in_origin() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);
		this.gitRepo.commit(project, "some message");
		this.gitRepo.tag(project, "v5.6.7.RELEASE");

		this.gitRepo.pushTag(project, "v5.6.7.RELEASE");

		try(Git git = openGitProject(origin)) {
			tagIsPresent(git, "v5.6.7");
			git.checkout().setName("v5.6.7.RELEASE").call();
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}


	private Git openGitProject(File project) {
		return new GitRepo.JGitFactory().open(project);
	}

	private void createNewFile(File project) throws Exception {
		File newFile = new File(this.tmpFolder, "newFile");
		newFile.createNewFile();
		try (PrintStream out = new PrintStream(new FileOutputStream(newFile))) {
			out.print("foo");
		}
		try(Git git = openGitProject(project)) {
			git.add().addFilepattern("newFile").call();
		}
	}

	@Test
	public void should_revert_changes() throws Exception {
		File project = this.gitRepo.cloneProject(this.springCloudReleaseProject.toURI());

		this.gitRepo.revert(project, "some message");

		try(Git git = openGitProject(project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

}

class ExceptionThrowingJGitFactory extends GitRepo.JGitFactory {
	@Override CloneCommand getCloneCommandByCloneRepository() {
		throw new CustomException("foo");
	}
}

class CustomException extends RuntimeException {
	public CustomException(String message) {
		super(message);
	}
}