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
				.hasMessageContaining("Pom with boot version is not present");
	}

	@Test
	public void should_throw_exception_when_boot_version_is_missing_in_pom() {
		PomParser parser = new PomParser(this.springCloudReleaseProject, "pom.xml");

		thenThrownBy(parser::bootVersion)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("The pom doesn't have a boot version");
	}

	@Test
	public void should_populate_boot_version() {
		PomParser parser = new PomParser(this.springCloudReleaseProject);

		String bootVersion = parser.bootVersion();

		then(bootVersion).isEqualTo("1.5.1.BUILD-SNAPSHOT");
	}
}