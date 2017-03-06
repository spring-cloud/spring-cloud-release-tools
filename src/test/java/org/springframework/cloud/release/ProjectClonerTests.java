package org.springframework.cloud.release;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.CloneCommand;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectClonerTests {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File springCloudReleaseProject;
	File tmpFolder;
	ProjectCloner projectCloner;

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.springCloudReleaseProject = new File(ProjectClonerTests.class.getResource("/projects/spring-cloud-release").toURI());
		TestUtils.prepareLocalRepo();
		this.projectCloner = new ProjectCloner(this.tmpFolder);
	}

	@Test
	public void should_clone_the_project_from_a_given_location() throws IOException {
		this.projectCloner.cloneProject(this.springCloudReleaseProject.toURI());

		then(new File(this.tmpFolder, ".git")).exists();
	}

	@Test(expected = IllegalStateException.class)
	public void should_throw_exception_when_there_is_no_repo()
			throws IOException, URISyntaxException {
		this.projectCloner.cloneProject(ProjectClonerTests.class.getResource("/projects/").toURI());
	}

	@Test(expected = IllegalStateException.class)
	public void should_throw_an_exception_when_failed_to_initialize_the_repo() throws IOException {
		new ProjectCloner(this.tmpFolder, new ExceptionThrowingJGitFactory()).cloneProject(this.springCloudReleaseProject.toURI());
	}

}

class ExceptionThrowingJGitFactory extends ProjectCloner.JGitFactory {
	@Override CloneCommand getCloneCommandByCloneRepository() {
		throw new RuntimeException("foo");
	}
}