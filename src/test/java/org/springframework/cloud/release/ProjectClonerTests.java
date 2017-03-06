package org.springframework.cloud.release;

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
public class ProjectClonerTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File springCloudReleaseProject;
	File tmpFolder;
	ProjectRepo projectRepo;

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.springCloudReleaseProject = new File(ProjectClonerTests.class.getResource("/projects/spring-cloud-release").toURI());
		TestUtils.prepareLocalRepo();
		this.projectRepo = new ProjectRepo(this.tmpFolder);
	}

	@Test
	public void should_clone_the_project_from_a_given_location() throws IOException {
		this.projectRepo.cloneProject(this.springCloudReleaseProject.toURI());

		then(new File(this.tmpFolder, ".git")).exists();
	}

	@Test
	public void should_throw_exception_when_there_is_no_repo() throws IOException, URISyntaxException {
		thenThrownBy(() -> this.projectRepo.cloneProject(ProjectClonerTests.class.getResource("/projects/").toURI()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Exception occurred while cloning repo");
	}

	@Test
	public void should_throw_an_exception_when_failed_to_initialize_the_repo() throws IOException {
		thenThrownBy(() ->  new ProjectRepo(this.tmpFolder, new ExceptionThrowingJGitFactory()).cloneProject(this.springCloudReleaseProject.toURI()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Exception occurred while cloning repo")
				.hasCauseInstanceOf(CustomException.class);
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo() throws IOException {
		File project = this.projectRepo.cloneProject(this.springCloudReleaseProject.toURI());
		this.projectRepo.checkout(project, "vCamden.SR3");

		File pom = new File(this.tmpFolder, "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath()).anyMatch(s -> s.contains("<version>Camden.SR3</version>"))).isTrue();
	}

	@Test
	public void should_throw_an_exception_when_checking_out_nonexisting_branch() throws IOException {
		File project = this.projectRepo.cloneProject(this.springCloudReleaseProject.toURI());
		try {
			this.projectRepo.checkout(project, "nonExistingBranch");
			fail("should throw an exception");
		} catch (IllegalStateException e) {
			then(e).hasMessageContaining("Ref nonExistingBranch can not be resolved");
		}
	}

}

class ExceptionThrowingJGitFactory extends ProjectRepo.JGitFactory {
	@Override CloneCommand getCloneCommandByCloneRepository() {
		throw new CustomException("foo");
	}
}

class CustomException extends RuntimeException {
	public CustomException(String message) {
		super(message);
	}
}