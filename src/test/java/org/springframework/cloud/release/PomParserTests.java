package org.springframework.cloud.release;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class PomParserTests {

	File springCloudReleaseProject;

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.springCloudReleaseProject = new File(ProjectClonerTests.class.getResource("/projects/spring-cloud-release").toURI());
	}

	@Test
	public void should_throw_exception_when_boot_pom_is_missing() {
		PomParser parser = new PomParser(new File("."));

		thenThrownBy(parser::bootVersion)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_boot() {
		PomParser parser = new PomParser(this.springCloudReleaseProject, null, null);

		thenThrownBy(parser::bootVersion)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_boot_version_is_missing_in_pom() {
		PomParser parser = new PomParser(this.springCloudReleaseProject, "pom.xml", null);

		thenThrownBy(parser::bootVersion)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("The pom doesn't have a [spring-boot-starter-parent] artifact id");
	}

	@Test
	public void should_populate_boot_version() {
		PomParser parser = new PomParser(this.springCloudReleaseProject);

		String bootVersion = parser.bootVersion().bootVersion;

		then(bootVersion).isEqualTo("1.5.1.BUILD-SNAPSHOT");
	}

	@Test
	public void should_throw_exception_when_cloud_pom_is_missing() {
		PomParser parser = new PomParser(new File("."));

		thenThrownBy(parser::springCloudVersions)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_null_is_passed_to_cloud() {
		PomParser parser = new PomParser(this.springCloudReleaseProject, null, null);

		thenThrownBy(parser::springCloudVersions)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Pom is not present");
	}

	@Test
	public void should_throw_exception_when_cloud_version_is_missing_in_pom() {
		PomParser parser = new PomParser(this.springCloudReleaseProject, null, "pom.xml");

		thenThrownBy(parser::springCloudVersions)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("The pom doesn't have a [spring-cloud-dependencies-parent] artifact id");
	}

	@Test
	public void should_populate_cloud_version() {
		PomParser parser = new PomParser(this.springCloudReleaseProject);

		Versions cloudVersions = parser.springCloudVersions();

		then(cloudVersions.scBuildVersion).isEqualTo("1.3.1.BUILD-SNAPSHOT");
		then(cloudVersions.projects).contains(allProjects());
	}

	@Test
	public void should_populate_boot_and_cloud_version() {
		PomParser parser = new PomParser(this.springCloudReleaseProject);

		Versions cloudVersions = parser.allVersions();

		then(cloudVersions.bootVersion).isEqualTo("1.5.1.BUILD-SNAPSHOT");
		then(cloudVersions.scBuildVersion).isEqualTo("1.3.1.BUILD-SNAPSHOT");
		then(cloudVersions.projects).contains(allProjects());
	}

	private Project[] allProjects() {
		return new Project[] { project("spring-cloud-aws", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-bus", "1.3.0.BUILD-SNAPSHOT"),
				project("spring-cloud-contract", "1.1.0.BUILD-SNAPSHOT"),
				project("spring-cloud-cloudfoundry", "1.1.0.BUILD-SNAPSHOT"),
				project("spring-cloud-commons", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-config", "1.3.0.BUILD-SNAPSHOT"),
				project("spring-cloud-netflix", "1.3.0.BUILD-SNAPSHOT"),
				project("spring-cloud-security", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-consul", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-sleuth", "1.2.0.BUILD-SNAPSHOT"),
				project("spring-cloud-stream", "Chelsea.BUILD-SNAPSHOT"),
				project("spring-cloud-task", "1.1.2.BUILD-SNAPSHOT"),
				project("spring-cloud-vault", "1.0.0.BUILD-SNAPSHOT"),
				project("spring-cloud-zookeeper", "1.1.0.BUILD-SNAPSHOT") };
	}

	Project project(String name, String value) {
		return new Project(name, value);
	}
}