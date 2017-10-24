package org.springframework.cloud.release.internal.gradle;

import static org.assertj.core.api.BDDAssertions.then;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class GradleUpdaterTests {
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_substitute_values_in_gradle_properties() throws IOException {
		File projectRoot = tmpFile("gradleproject");
		ReleaserProperties properties = new ReleaserProperties();
		Map<String, String> props = new HashMap<String, String>() {{
			put("foo", "spring-cloud-contract");
			put("bar", "spring-cloud-sleuth");
		}};
		properties.getGradle().setGradlePropsSubstitution(props);
		Projects projects = new Projects(
				new ProjectVersion("spring-cloud-contract", "1.0.0"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.0")
		);

		new GradleUpdater(properties).updateProjectFromSCRelease(projectRoot,
				projects, new ProjectVersion("spring-cloud-contract", "1.0.0"));

		then(asString(tmpFile("gradleproject/gradle.properties")))
				.contains("foo=1.0.0");
		then(asString(tmpFile("gradleproject/child/gradle.properties")))
				.contains("bar=2.0.0");
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(GradleUpdaterTests.class.getResource(relativePath).toURI());
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}
}