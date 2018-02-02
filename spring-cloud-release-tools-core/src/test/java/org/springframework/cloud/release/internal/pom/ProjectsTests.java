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
	public void should_find_projects_starting_with_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-boot-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forNameStartingWith("spring-boot").get(0).version).isEqualTo("1.0.0");
	}

	@Test
	public void should_create_project_with_bumped_original_version_and_original_parent_versions() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		ProjectVersion build = new ProjectVersion("spring-cloud-build", "1.0.0");
		projectVersions.add(build);
		ProjectVersion boot = new ProjectVersion("spring-boot-starter", "2.0.0");
		projectVersions.add(boot);
		ProjectVersion bootDeps = new ProjectVersion("spring-boot-dependencies", "2.0.0");
		projectVersions.add(bootDeps);
		ProjectVersion original = new ProjectVersion("spring-cloud-starter-foo", "3.0.0.BUILD-SNAPSHOT");
		projectVersions.add(original);
		Projects projects = new Projects(projectVersions);

		Projects forRollback = Projects.forRollback(projects, original);

		then(forRollback.forName("spring-cloud-build").version).isEqualTo("1.0.0");
		then(forRollback.forName("spring-boot-starter").version).isEqualTo("2.0.0");
		then(forRollback.forName("spring-boot-dependencies").version).isEqualTo("2.0.0");
		then(forRollback.forName("spring-cloud-starter-foo").version).isEqualTo("3.0.1.BUILD-SNAPSHOT");
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

	@Test
	public void should_return_empty_list_when_project_is_not_present_when_searching_by_starting_with_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo-bar-baz", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forNameStartingWith("asd")).isEmpty();
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