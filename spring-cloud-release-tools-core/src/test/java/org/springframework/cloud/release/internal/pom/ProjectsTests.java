package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectsTests {

	File springCloudReleasePom = file("/projects/spring-cloud-release");

	@Test
	public void should_find_a_project_by_name_from_file() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-cloud-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forFile(springCloudReleasePom).version).isEqualTo("1.0.0");
	}

	@Test
	public void should_throw_exception_when_project_is_not_present() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		thenThrownBy(() -> projects.forFile(springCloudReleasePom))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Project with name [spring-cloud-starter-build] is not present");
	}

	private File file(String relativePath) {
		try {
			return new File(ProjectsTests.class.getResource(relativePath).toURI());
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

}