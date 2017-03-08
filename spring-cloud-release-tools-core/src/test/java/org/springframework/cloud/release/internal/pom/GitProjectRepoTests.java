package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.eclipse.jgit.api.CloneCommand;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class GitProjectRepoTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File springCloudReleaseProject;
	File tmpFolder;
	GitProjectRepo gitProjectRepo;

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.springCloudReleaseProject = new File(GitProjectRepoTests.class.getResource("/projects/spring-cloud-release").toURI());
		TestUtils.prepareLocalRepo();
		this.gitProjectRepo = new GitProjectRepo(this.tmpFolder);
	}

	@Test
	public void should_clone_the_project_from_a_given_location() throws IOException {
		this.gitProjectRepo.cloneProject(this.springCloudReleaseProject.toURI());

		then(new File(this.tmpFolder, ".git")).exists();
	}

	@Test
	public void should_throw_exception_when_there_is_no_repo() throws IOException, URISyntaxException {
		thenThrownBy(() -> this.gitProjectRepo
				.cloneProject(GitProjectRepoTests.class.getResource("/projects/").toURI()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Exception occurred while cloning repo");
	}

	@Test
	public void should_throw_an_exception_when_failed_to_initialize_the_repo() throws IOException {
		thenThrownBy(() ->  new GitProjectRepo(this.tmpFolder, new ExceptionThrowingJGitFactory()).cloneProject(this.springCloudReleaseProject.toURI()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Exception occurred while cloning repo")
				.hasCauseInstanceOf(CustomException.class);
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo() throws IOException {
		File project = this.gitProjectRepo.cloneProject(this.springCloudReleaseProject.toURI());
		this.gitProjectRepo.checkout(project, "vCamden.SR3");

		File pom = new File(this.tmpFolder, "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath()).anyMatch(s -> s.contains("<version>Camden.SR3</version>"))).isTrue();
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo2() throws IOException {
		File project = this.gitProjectRepo.cloneProject(this.springCloudReleaseProject.toURI());
		this.gitProjectRepo.checkout(project, "Camden.x");

		File pom = new File(this.tmpFolder, "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath()).anyMatch(s -> s.contains("<version>Camden.BUILD-SNAPSHOT</version>"))).isTrue();
	}

	@Test
	public void should_throw_an_exception_when_checking_out_nonexisting_branch() throws IOException {
		File project = this.gitProjectRepo.cloneProject(this.springCloudReleaseProject.toURI());
		try {
			this.gitProjectRepo.checkout(project, "nonExistingBranch");
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			then(e).hasMessageContaining("Ref nonExistingBranch can not be resolved");
		}
	}

}

class ExceptionThrowingJGitFactory extends GitProjectRepo.JGitFactory {
	@Override CloneCommand getCloneCommandByCloneRepository() {
		throw new CustomException("foo");
	}
}

class CustomException extends RuntimeException {
	public CustomException(String message) {
		super(message);
	}
}