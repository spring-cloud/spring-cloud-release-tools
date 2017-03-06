/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.release.internal;

import static org.assertj.core.api.BDDAssertions.then;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class PomUpdaterTests {

	Versions versions = new Versions("0.0.1", "0.0.2", projects());
	PomUpdater pomUpdater = new PomUpdater();
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	File temporaryFolder;

	@Before
	public void setup() throws IOException {
		this.temporaryFolder = this.tmp.newFolder();
	}

	@Test
	public void should_not_update_pom_when_project_is_not_on_the_versions_list() throws Exception {
		File springCloudReleasePom = file("/projects/spring-cloud-release");

		then(this.pomUpdater.shouldProjectBeUpdated(springCloudReleasePom, this.versions)).isFalse();
	}

	@Test
	public void should_update_pom_when_project_is_not_on_the_versions_list() throws Exception {
		File springCloudSleuthPom = file("/projects/spring-cloud-sleuth");

		then(this.pomUpdater.shouldProjectBeUpdated(springCloudSleuthPom, this.versions)).isTrue();
	}

	@Test
	public void should_not_update_the_model_if_no_changes_were_made() throws Exception {
		File nonMatchingPom = pom("/projects/project");

		ModelWrapper model = this.pomUpdater.updateModel("foo", nonMatchingPom, this.versions);

		then(model.dirty).isFalse();
	}

	@Test
	public void should_update_the_model_if_only_artifact_id_is_matched_in_the_root_pom() throws Exception {
		File matchingArtifactId = pom("/projects/project", "pom_matching_artifact.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		// the rest is the same
		then(model.model.getParent().getVersion()).isEqualTo("1.3.1.BUILD-SNAPSHOT");
		then(model.model.getProperties())
			.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
			.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_model_if_parent_is_matched_via_sc_build() throws Exception {
		File matchingArtifactId = pom("/projects/project", "pom_matching_parent_v2.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		then(model.model.getParent().getVersion()).isEqualTo("0.0.2");
		// the rest is the same
		then(model.model.getProperties())
				.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
				.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_model_if_parent_is_matched_via_sc_dependencies_parent() throws Exception {
		File matchingArtifactId = pom("/projects/project", "pom_matching_parent.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		then(model.model.getParent().getVersion()).isEqualTo("0.0.2");
		// the rest is the same
		then(model.model.getProperties())
			.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
			.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_model_if_properties_are_matched() throws Exception {
		File matchingArtifactId = pom("/projects/project", "pom_matching_properties.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		then(model.model.getParent().getVersion()).isEqualTo("0.0.2");
		then(model.model.getProperties())
			.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT")
			.containsEntry("spring-cloud-vault.version", "0.0.4.BUILD-SNAPSHOT");
	}

	@Test
	public void should_not_update_child_pom_when_project_is_not_on_the_versions_list() throws Exception {
		File springCloudReleasePom = file("/projects/spring-cloud-release");

		then(this.pomUpdater.shouldProjectBeUpdated(springCloudReleasePom, this.versions)).isFalse();
	}

	@Test
	public void should_update_child_pom_when_project_is_not_on_the_versions_list() throws Exception {
		File springCloudSleuthPom = file("/projects/spring-cloud-sleuth");

		then(this.pomUpdater.shouldProjectBeUpdated(springCloudSleuthPom, this.versions)).isTrue();
	}

	@Test
	public void should_update_the_child_model_if_parent_is_matched_via_sc_build() throws Exception {
		File matchingArtifactId = pom("/projects/project/children", "pom_matching_parent_v2.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getParent().getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		// the rest is the same
		then(model.model.getProperties())
				.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
				.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_child_model_if_parent_is_matched_via_sc_dependencies_parent() throws Exception {
		File matchingArtifactId = pom("/projects/project/children", "pom_matching_parent.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getParent().getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		// the rest is the same
		then(model.model.getProperties())
			.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
			.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_child_model_if_properties_are_matched() throws Exception {
		File matchingArtifactId = pom("/projects/project/children", "pom_matching_properties.xml");

		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", matchingArtifactId, this.versions);

		then(model.dirty).isTrue();
		then(model.model.getParent().getVersion()).isEqualTo("0.0.3.BUILD-SNAPSHOT");
		then(model.model.getProperties())
			.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT")
			.containsEntry("spring-cloud-vault.version", "0.0.4.BUILD-SNAPSHOT");
	}

	@Test
	public void should_override_a_pom_when_there_was_a_change_in_the_model() throws Exception {
		FileSystemUtils.copyRecursively(file("/projects/project"), this.temporaryFolder);
		File beforeProcessing = pom("/projects/project/children", "pom_matching_properties.xml");
		File afterProcessing = new File(this.temporaryFolder, "/children/pom_matching_properties.xml");
		ModelWrapper model = this.pomUpdater.updateModel("spring-cloud-sleuth", afterProcessing, this.versions);

		File processedPom = this.pomUpdater.overwritePomIfDirty(model, afterProcessing);

		then(processedPom).isSameAs(afterProcessing);
		String processedPomText = asString(processedPom);
		String beforeProcessingText = asString(beforeProcessing);
		then(processedPomText).isNotEqualTo(beforeProcessingText);
	}

	@Test
	public void should_not_override_a_pom_when_there_was_no_change_in_the_model() throws Exception {
		FileSystemUtils.copyRecursively(file("/projects/project"), this.temporaryFolder);
		File beforeProcessing = pom("/projects/project/");
		File afterProcessing = new File(this.temporaryFolder, "/pom.xml");
		ModelWrapper model = this.pomUpdater.updateModel("foo", afterProcessing, this.versions);

		File processedPom = this.pomUpdater.overwritePomIfDirty(model, afterProcessing);

		then(processedPom).isSameAs(afterProcessing);
		then(asString(processedPom)).isEqualTo(asString(beforeProcessing));
	}

	Set<Project> projects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("spring-cloud-sleuth", "0.0.3.BUILD-SNAPSHOT"));
		projects.add(new Project("spring-cloud-vault", "0.0.4.BUILD-SNAPSHOT"));
		return projects;
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(GitProjectRepoTests.class.getResource(relativePath).toURI());
	}

	private File pom(String relativePath) throws URISyntaxException {
		return pom(relativePath, "pom.xml");
	}

	private File pom(String relativePath, String pomName) throws URISyntaxException {
		return new File(new File(GitProjectRepoTests.class.getResource(relativePath).toURI()), pomName);
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

}