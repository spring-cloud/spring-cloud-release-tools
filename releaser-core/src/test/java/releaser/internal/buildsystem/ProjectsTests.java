/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package releaser.internal.buildsystem;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import releaser.SpringCloudReleaserProperties;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

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

		then(projects.forFile(this.springCloudReleasePom).version).isEqualTo("1.0.0");
	}

	@Test
	public void should_find_a_project_by_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-cloud-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forName("spring-cloud-starter-build").version).isEqualTo("1.0.0");
	}

	@Test
	public void should_return_true_when_a_project_by_name_exists() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-cloud-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.containsProject("spring-cloud-starter-build")).isTrue();
	}

	@Test
	public void should_find_projects_starting_with_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("spring-boot-starter-build", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forNameStartingWith("spring-boot").get(0).version)
				.isEqualTo("1.0.0");
	}

	@Test
	public void should_create_project_with_bumped_original_version_and_original_parent_versions() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		ProjectVersion build = new ProjectVersion("spring-cloud-build", "1.0.0-RELEASE");
		projectVersions.add(build);
		ProjectVersion boot = new ProjectVersion("spring-boot-starter", "2.0.0-RELEASE");
		projectVersions.add(boot);
		ProjectVersion bootDeps = new ProjectVersion("spring-boot-dependencies",
				"2.0.0.RELEASE");
		projectVersions.add(bootDeps);
		ProjectVersion original = new ProjectVersion("spring-cloud-starter-foo",
				"3.0.0.BUILD-SNAPSHOT");
		projectVersions.add(original);
		Projects projects = new Projects(projectVersions);

		Projects forRollback = Projects.forRollback(SpringCloudReleaserProperties.get(),
				projects);

		then(forRollback.forName("spring-cloud-build").version)
				.isEqualTo("1.0.1-SNAPSHOT");
		then(forRollback.forName("spring-boot-starter").version)
				.isEqualTo("2.0.0-RELEASE");
		then(forRollback.forName("spring-boot-dependencies").version)
				.isEqualTo("2.0.0.RELEASE");
		then(forRollback.forName("spring-cloud-starter-foo").version)
				.isEqualTo("3.0.0.BUILD-SNAPSHOT");
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
	public void should_return_filtered_project() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		projectVersions.add(new ProjectVersion("bar", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		Projects filtered = projects.filter(Collections.singletonList("foo"));
		then(filtered).hasSize(1);
		then(filtered.forName("bar").version).isEqualTo("1.0.0");
	}

	@Test
	public void should_return_projects_with_bumped_versions() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions
				.add(new ProjectVersion("spring-boot-dependencies", "2.0.0.RELEASE"));
		projectVersions.add(new ProjectVersion("spring-boot-starter", "2.0.0.RELEASE"));
		projectVersions.add(new ProjectVersion("foo", "1.0.0.RELEASE"));
		projectVersions.add(new ProjectVersion("bar", "1.0.1.M1"));
		projectVersions.add(new ProjectVersion("baz", "1.0.2.BUILD-SNAPSHOT"));
		projectVersions.add(new ProjectVersion("foo2", "1.0.0.SR1"));
		projectVersions.add(new ProjectVersion("foo3", "Finchley.BUILD-SNAPSHOT"));
		projectVersions.add(new ProjectVersion("foo4", "Finchley.SR4"));
		Projects projects = new Projects(projectVersions);

		Projects bumped = projects
				.postReleaseSnapshotVersion(Collections.singletonList("spring-boot"));

		then(bumped.forName("spring-boot-dependencies").version)
				.isEqualTo("2.0.0.RELEASE");
		then(bumped.forName("spring-boot-starter").version).isEqualTo("2.0.0.RELEASE");
		then(bumped.forName("foo").version).isEqualTo("1.0.1.SNAPSHOT");
		then(bumped.forName("bar").version).isEqualTo("1.0.1.SNAPSHOT");
		then(bumped.forName("baz").version).isEqualTo("1.0.2.BUILD-SNAPSHOT");
		then(bumped.forName("foo2").version).isEqualTo("1.0.1.SNAPSHOT");
		then(bumped.forName("foo3").version).isEqualTo("Finchley.BUILD-SNAPSHOT");
		then(bumped.forName("foo4").version).isEqualTo("Finchley.SNAPSHOT");
	}

	@Test
	public void should_throw_exception_when_project_is_not_present_when_searching_by_file() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		thenThrownBy(() -> projects.forFile(this.springCloudReleasePom))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(
						"Project with name [spring-cloud-starter-build] is not present");
	}

	@Test
	public void should_throw_exception_when_project_is_not_present() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		thenThrownBy(() -> projects.forName("spring-cloud-starter-build"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(
						"Project with name [spring-cloud-starter-build] is not present");
	}

	@Test
	public void should_return_empty_list_when_project_is_not_present_when_searching_by_starting_with_name() {
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo-bar-baz", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.forNameStartingWith("asd")).isEmpty();
	}

	@Test
	public void should_return_version_for_release_train() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMetaRelease().setReleaseTrainProjectName("release-train");
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("release-train", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		then(projects.releaseTrain(properties).version).isEqualTo("1.0.0");
	}

	@Test
	public void should_throw_exception_when_release_train_project() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMetaRelease().setReleaseTrainProjectName("release-train");
		Set<ProjectVersion> projectVersions = new HashSet<>();
		projectVersions.add(new ProjectVersion("foo", "1.0.0"));
		Projects projects = new Projects(projectVersions);

		thenThrownBy(() -> projects.releaseTrain(properties))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining(
						"don't contain any of the following release train names");
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
