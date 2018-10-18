package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.net.URISyntaxException;

import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseTrainContentsUpdaterTests {

	ReleaserProperties properties = new ReleaserProperties();
	ProjectGitHandler projectGitHandler = new ProjectGitHandler(this.properties);
	ReleaseTrainContentsUpdater updater = new ReleaseTrainContentsUpdater(this.properties,
			this.projectGitHandler);
	File springCloudRepo;
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/spring-cloud"), this.temporaryFolder);
		this.springCloudRepo = this.temporaryFolder;
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(ReleaseTrainContentsUpdaterTests.class.getResource(relativePath).toURI());
	}

	@Test
	public void should_do_nothing_when_switch_is_off() {
		this.properties.getGit().setUpdateSpringProject(false);

		File file = this.updater.updateProjectRepo(oldReleaseTrain());

		BDDAssertions.then(file).isNull();
	}

	@Test
	public void should_not_update_the_contents_of_spring_project_repo_when_release_train_smaller() throws GitAPIException {
		this.properties.getGit().setSpringProjectUrl(this.springCloudRepo.getAbsolutePath() + "/");

		File file = this.updater.updateProjectRepo(oldReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions.then(GitTestUtils.openGitProject(file).log().call().iterator().next().getShortMessage())
				.doesNotContain("Updating project page to release train");
	}

	@Test
	public void should_update_the_contents_of_spring_project_repo_when_release_train_greater() throws GitAPIException {
		this.properties.getGit().setSpringProjectUrl(this.springCloudRepo.getAbsolutePath() + "/");

		File file = this.updater.updateProjectRepo(newReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions.then(GitTestUtils.openGitProject(file).log().call().iterator().next().getShortMessage())
				.contains("Updating project page to release train [Edgware.SR7]");
	}

	Projects oldReleaseTrain() {
		return new Projects(
				new ProjectVersion("spring-cloud-aws", "3.0.0.RELEASE"),
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
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				// old release train
				new ProjectVersion("spring-cloud-release", "Dalston.SR1"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects newReleaseTrain() {
		return new Projects(
				new ProjectVersion("spring-cloud-aws", "3.0.0.RELEASE"),
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
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				// newer release train
				new ProjectVersion("spring-cloud-release", "Edgware.SR7"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}
}