package org.springframework.cloud.release.internal.pom;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

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
	public void should_find_a_project_by_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-cloud-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forName("spring-cloud-starter-build").version).isEqualTo("1.0.0");
	}

	@Test
	public void should_remove_a_project_by_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-cloud-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		projects.remove("spring-cloud-starter-build");

		then(projects).isEmpty();
	}

	@Test
	public void should_return_true_when_there_is_at_least_one_snapshot_project() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"));
		projectVersions.add(new ProjectVersion("bar", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.containsSnapshots()).isTrue();
	}

	@Test
	public void should_return_false_when_there_are_no_snapshot_versions() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		projectVersions.add(new ProjectVersion("bar", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.containsSnapshots()).isFalse();
	}

	@Test
	public void should_throw_exception_when_project_is_not_present_when_searching_by_file() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		thenThrownBy(() -> projects.forFile(springCloudReleasePom))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Project with name [spring-cloud-starter-build] is not present");
	}

	@Test
	public void should_throw_exception_when_project_is_not_present() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		thenThrownBy(() -> projects.forName("spring-cloud-starter-build"))
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