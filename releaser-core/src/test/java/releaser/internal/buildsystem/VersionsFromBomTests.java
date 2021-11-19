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

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;
import releaser.SpringCloudReleaserProperties;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.Project;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class VersionsFromBomTests {

	VersionsFromBom versionsFromBom = new VersionsFromBomBuilder()
			.releaserProperties(SpringCloudReleaserProperties.get()).projects(projects()).retrieveFromBom();

	@Test
	public void should_return_true_when_project_is_on_the_list() {
		then(this.versionsFromBom.shouldBeUpdated("foo")).isTrue();
	}

	@Test
	public void should_return_true_when_project_has_a_parent_suffix_and_project_is_on_the_list() {
		then(this.versionsFromBom.shouldBeUpdated("foo-parent")).isTrue();
	}

	@Test
	public void should_return_true_when_project_has_a_dependencies_suffix_and_project_is_on_the_list() {
		then(this.versionsFromBom.shouldBeUpdated("foo-dependencies")).isTrue();
	}

	@Test
	public void should_return_false_when_project_is_not_on_the_list() {
		then(this.versionsFromBom.shouldBeUpdated("missing")).isFalse();
	}

	@Test
	public void should_return_version_for_present_project() {
		then(this.versionsFromBom.versionForProject("foo")).isEqualTo("bar");
	}

	@Test
	public void should_return_empty_string_for_missing_project() {
		then(this.versionsFromBom.versionForProject("missing")).isEmpty();
	}

	@Test
	public void should_return_true_if_properties_contains_project_key() {
		then(this.versionsFromBom.shouldSetProperty(validProps())).isTrue();
	}

	@Test
	public void should_return_false_if_properties_does_not_contain_project_key() {
		then(this.versionsFromBom.shouldSetProperty(missingProps())).isFalse();
	}

	@Test
	public void should_update_projects_for_spring_cloud_release() {
		VersionsFromBom versionsFromBom = mixedVersions().setVersion("spring-cloud", "3.0.0");

		then(versionsFromBom.versionForProject("spring-cloud")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-release")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("3.0.0");

		versionsFromBom = mixedVersions().setVersion("spring-cloud-release", "3.0.0");

		then(versionsFromBom.versionForProject("spring-cloud")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-release")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("3.0.0");

		versionsFromBom = mixedVersions().setVersion("spring-cloud-release", "Greenwich.SR8");

		then(versionsFromBom.versionForProject("spring-cloud")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-release")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.SR8");

		versionsFromBom = mixedVersions().setVersion("spring-cloud-dependencies", "Greenwich.SR8");

		then(versionsFromBom.versionForProject("spring-cloud")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-release")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.SR8");
	}

	@Test
	public void should_update_projects_for_custom_bom_only() {
		VersionsFromBom versionsFromBom = mixedVersions(customBom()).setVersion("spring-cloud", "3.0.0");

		then(versionsFromBom.versionForProject("spring-cloud")).isEqualTo("3.0.0");
		then(versionsFromBom.versionForProject("spring-cloud-stream-starters")).isEqualTo("Fishtown.RELEASE");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.RELEASE");

		versionsFromBom = mixedVersions(customBom()).setVersion("spring-cloud-stream-starters", "Fishtown.SR4");

		then(versionsFromBom.versionForProject("spring-cloud")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-stream-starters")).isEqualTo("Fishtown.SR4");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.RELEASE");

		versionsFromBom = mixedVersions(customBom()).setVersion("spring-cloud-release", "Greenwich.SR8");

		then(versionsFromBom.versionForProject("spring-cloud-release")).isEqualTo("Greenwich.SR8");
		then(versionsFromBom.versionForProject("spring-cloud-stream-starters")).isEqualTo("Fishtown.RELEASE");
		then(versionsFromBom.versionForProject("spring-cloud")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.RELEASE");

		versionsFromBom = mixedVersions(customBom()).setVersion("spring-cloud-dependencies", "Greenwich.SR8");

		then(versionsFromBom.versionForProject("spring-cloud")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-stream-starters")).isEqualTo("Fishtown.RELEASE");
		then(versionsFromBom.versionForProject("spring-cloud-starter")).isEmpty();
		then(versionsFromBom.versionForProject("spring-cloud-dependencies")).isEqualTo("Greenwich.SR8");
	}

	@Test
	public void should_update_projects_for_custom_project() {
		VersionsFromBom versionsFromBom = mixedVersions().setVersion("foo", "3.0.0");

		then(versionsFromBom.versionForProject("foo")).isEqualTo("3.0.0");
	}

	private VersionsFromBom mixedVersions() {
		return new VersionsFromBomBuilder().releaserProperties(SpringCloudReleaserProperties.get())
				.parsers(Collections.emptyList()).projects(mixedProjects()).merged();
	}

	private VersionsFromBom mixedVersions(ReleaserProperties properties) {
		return new VersionsFromBomBuilder().releaserProperties(properties).projects(mixedProjects()).retrieveFromBom();
	}

	private ReleaserProperties customBom() {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		properties.getMetaRelease().setReleaseTrainDependencyNames(Collections.emptyList());
		properties.getMetaRelease().setReleaseTrainProjectName("spring-cloud-stream-starters");
		properties.getPom().setThisTrainBom("spring-cloud-stream-dependencies");
		return properties;
	}

	Set<Project> projects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("foo", "bar"));
		return projects;
	}

	Set<Project> springCloudBuildProjects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("spring-cloud-build", "1.2.3.BUILD-SNAPSHOT"));
		return projects;
	}

	Set<Project> mixedProjects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("foo", "1.0.0.BUILD-SNAPSHOT"));
		projects.add(new Project("fooBar", "1.0.0.RELEASE"));
		projects.add(new Project("spring-boot", "1.0.0"));
		projects.add(new Project("spring-cloud-build", "2.0.0"));
		projects.add(new Project("spring-cloud-release", "Greenwich.RELEASE"));
		projects.add(new Project("spring-cloud-dependencies", "Greenwich.RELEASE"));
		projects.add(new Project("spring-cloud-stream-starters", "Fishtown.RELEASE"));
		return projects;
	}

	Properties validProps() {
		Properties properties = new Properties();
		properties.setProperty("foo.version", "1.0.0");
		return properties;
	}

	Properties missingProps() {
		Properties properties = new Properties();
		properties.setProperty("missing.version", "1.0.0");
		return properties;
	}

}
