package org.springframework.cloud.release.internal.post;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.release.internal.PomUpdateAcceptanceTests;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.pom.TestPomReader;
import org.springframework.cloud.release.internal.pom.TestUtils;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class PostReleaseActionsTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();
	File temporaryFolder;
	TestPomReader testPomReader = new TestPomReader();
	ReleaserProperties properties = new ReleaserProperties();
	File clonedTestSamples;
	ProjectGitHandler projectGitHandler = new ProjectGitHandler(this.properties) {
		@Override
		public File cloneTestSamplesProject() {
			clonedTestSamples = super.cloneTestSamplesProject();
			return clonedTestSamples;
		}
	};
	ProjectPomUpdater updater = new ProjectPomUpdater(this.properties);
	ProjectBuilder builder = new ProjectBuilder(this.properties);

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_update_project_and_run_tests() {
		this.properties.getGit().setTestSamplesProjectUrl(tmpFile("spring-cloud-core-tests/").getAbsolutePath() + "/");
		this.properties.getMaven().setBuildCommand("touch build.log");
		PostReleaseActions actions = new PostReleaseActions(this.projectGitHandler,
				this.updater, this.builder, this.properties);

		actions.runUpdatedTests(currentGa());

		Model rootPom = this.testPomReader.readPom(new File(clonedTestSamples, "pom.xml"));
		BDDAssertions.then(rootPom.getVersion()).isEqualTo("Finchley.SR1");
		BDDAssertions.then(rootPom.getParent().getVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(sleuthParentPomVersion()).isEqualTo("2.0.4.RELEASE");
		BDDAssertions.then(new File(clonedTestSamples, "build.log")).exists();
	}

	private String sleuthParentPomVersion() {
		return this.testPomReader.readPom(new File(clonedTestSamples, "sleuth/pom.xml"))
				.getParent().getVersion();
	}

	Projects currentGa() {
		return new Projects(
				new ProjectVersion("spring-cloud-aws", "2.0.0.RELEASE"),
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
				new ProjectVersion("spring-cloud-release", "Finchley.SR1"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI());
	}
}