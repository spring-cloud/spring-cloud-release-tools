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

package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.git.GitRepoTests;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class PomUpdaterTests {

	@Rule
	public OutputCapture capture = new OutputCapture();

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	Versions versions = new Versions("0.0.1", "0.0.2", projects());

	PomUpdater pomUpdater = new PomUpdater();

	PomReader pomReader = new PomReader();

	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_not_update_pom_when_project_is_not_on_the_versions_list()
			throws Exception {
		File springCloudReleasePom = file("/projects/spring-cloud-release");

		BDDAssertions.then(this.pomUpdater.shouldProjectBeUpdated(springCloudReleasePom,
				this.versions)).isFalse();
	}

	@Test
	public void should_not_update_pom_when_project_with_parent_suffix_is_not_on_the_versions_list()
			throws Exception {
		File springCloud = pom("/projects/project", "pom_with_parent_suffix.xml");

		BDDAssertions
				.then(this.pomUpdater.shouldProjectBeUpdated(springCloud, this.versions))
				.isFalse();
	}

	@Test
	public void should_update_pom_for_project_with_suffix_when_project_is_on_the_versions_list()
			throws Exception {
		File springCloud = pom("/projects/project",
				"pom_matching_with_parent_suffix.xml");

		BDDAssertions
				.then(this.pomUpdater.shouldProjectBeUpdated(springCloud, this.versions))
				.isTrue();
	}

	@Test
	public void should_update_pom_when_project_is_not_on_the_versions_list()
			throws Exception {
		File springCloudSleuthPom = file("/projects/spring-cloud-sleuth");

		BDDAssertions.then(this.pomUpdater.shouldProjectBeUpdated(springCloudSleuthPom,
				this.versions)).isTrue();
	}

	@Test
	public void should_not_update_pom_when_project_is_on_the_versions_list_but_there_is_no_pom()
			throws Exception {
		File springCloudSleuthPom = file("/projects/spring-cloud-sleuth/empty-folder");

		BDDAssertions.then(this.pomUpdater.shouldProjectBeUpdated(springCloudSleuthPom,
				this.versions)).isFalse();
	}

	@Test
	public void should_not_update_the_pom_if_no_changes_were_made() throws Exception {
		File originalPom = pom("/projects/project");
		File pomInTemp = tmpFile("/project/pom.xml");
		ModelWrapper rootPom = model("foo");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isEqualTo(asString(originalPom));
	}

	@Test
	public void should_update_the_pom_if_only_artifact_id_is_matched_in_the_root_pom()
			throws Exception {
		File originalPom = pom("/projects/project", "pom_matching_artifact.xml");
		File pomInTemp = tmpFile("/project/pom_matching_artifact.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_pom_if_parent_is_matched_via_sc_build()
			throws Exception {
		File originalPom = pom("/projects/project", "pom_matching_parent_v2.xml");
		File pomInTemp = tmpFile("/project/pom_matching_parent_v2.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(originalPom)).isNotEqualTo(asString(storedPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.2");
	}

	@Test
	public void should_update_the_pom_if_parent_is_matched_via_sc_dependencies_parent()
			throws Exception {
		File originalPom = pom("/projects/project", "pom_matching_parent.xml");
		File pomInTemp = tmpFile("/project/pom_matching_parent.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.2");
	}

	@Test
	public void should_not_update_child_pom_when_project_is_not_on_the_versions_list()
			throws Exception {
		File springCloudReleasePom = file("/projects/spring-cloud-release");

		BDDAssertions.then(this.pomUpdater.shouldProjectBeUpdated(springCloudReleasePom,
				this.versions)).isFalse();
	}

	@Test
	public void should_update_child_pom_when_project_is_not_on_the_versions_list()
			throws Exception {
		File springCloudSleuthPom = file("/projects/spring-cloud-sleuth");

		BDDAssertions.then(this.pomUpdater.shouldProjectBeUpdated(springCloudSleuthPom,
				this.versions)).isTrue();
	}

	@Test
	public void should_update_the_child_pom_if_parent_is_matched_via_sc_build()
			throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_matching_parent_v2.xml");
		File pomInTemp = tmpFile("/project/children/pom_matching_parent_v2.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		// the rest is the same
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
				.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_parent_from_properties_even_if_group_ids_dont_match()
			throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_different_group_boot_parent.xml");
		File pomInTemp = tmpFile("/project/children/pom_different_group_boot_parent.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth", "org.springframework.cloud");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.1");
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT");
	}

	@Test
	public void should_only_update_the_properties_section_when_group_ids_dont_match_and_there_is_no_skip_deployment()
			throws Exception {
		File originalPom = pom("/projects/project/children", "pom_different_group.xml");
		File pomInTemp = tmpFile("/project/children/pom_different_group.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth", "org.springframework.cloud");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("1.2.2.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("1.5.8.RELEASE");
		// the rest is the same
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_everything_when_group_ids_dont_match_and_there_is_skip_deployment_property()
			throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_different_group_skip_deployment_prop.xml");
		File pomInTemp = tmpFile(
				"/project/children/pom_different_group_skip_deployment_prop.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth", "org.springframework.cloud");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("1.2.2.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.1");
		// the rest is the same
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_everything_when_group_ids_dont_match_and_there_is_skip_in_deployment_plugin()
			throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_different_group_skip_deployment_plugin.xml");
		File pomInTemp = tmpFile(
				"/project/children/pom_different_group_skip_deployment_plugin.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth", "org.springframework.cloud");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("1.2.2.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.1");
		// the rest is the same
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_everything_when_group_ids_dont_match_and_there_is_skip_in_deployment_plugin_management()
			throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_different_group_skip_deployment_plugin_mngmnt.xml");
		File pomInTemp = tmpFile(
				"/project/children/pom_different_group_skip_deployment_plugin_mngmnt.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth", "org.springframework.cloud");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("1.2.2.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.1");
		// the rest is the same
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_boot_parent_even_if_the_project_group_doesnt_match()
			throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_case_from_contract.xml");
		File pomInTemp = tmpFile("/project/children/pom_case_from_contract.xml");
		ModelWrapper rootPom = model("spring-cloud-contract",
				"org.springframework.cloud");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.1");

	}

	@Test
	public void should_update_the_child_pom_if_parent_is_matched_via_sc_dependencies_parent()
			throws Exception {
		File originalPom = pom("/projects/project/children", "pom_matching_parent.xml");
		File pomInTemp = tmpFile("/project/children/pom_matching_parent.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		// the rest is the same
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-foo.version", "1.3.1.BUILD-SNAPSHOT")
				.containsEntry("foo.version", "1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_update_the_child_pom_if_properties_are_matched() throws Exception {
		File originalPom = pom("/projects/project/children",
				"pom_matching_properties.xml");
		File pomInTemp = tmpFile("/project/children/pom_matching_properties.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.3.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getProperties())
				.containsEntry("spring-cloud-sleuth.version", "0.0.3.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-vault.version", "0.0.4.BUILD-SNAPSHOT");
	}

	@Test
	public void should_override_a_pom_when_there_was_a_change_in_the_model()
			throws Exception {
		File beforeProcessing = pom("/projects/project/children",
				"pom_matching_properties.xml");
		File afterProcessing = tmpFile("/project/children/pom_matching_properties.xml");
		ModelWrapper model = this.pomUpdater.updateModel(model("spring-cloud-sleuth"),
				afterProcessing, this.versions);

		File processedPom = this.pomUpdater.overwritePomIfDirty(model,
				Versions.EMPTY_VERSION, afterProcessing);

		String processedPomText = asString(processedPom);
		String beforeProcessingText = asString(beforeProcessing);
		BDDAssertions.then(processedPomText).isNotEqualTo(beforeProcessingText);
	}

	@Test
	public void should_not_override_a_pom_when_there_was_no_change_in_the_model()
			throws Exception {
		File beforeProcessing = pom("/projects/project/");
		File afterProcessing = tmpFile("/project/pom.xml");
		ModelWrapper model = this.pomUpdater.updateModel(model("foo"), afterProcessing,
				this.versions);

		File processedPom = this.pomUpdater.overwritePomIfDirty(model,
				Versions.EMPTY_VERSION, afterProcessing);

		BDDAssertions.then(asString(processedPom)).isEqualTo(asString(beforeProcessing));
	}

	@Test
	public void should_update_the_model_when_root_project_has_parent_suffix()
			throws Exception {
		File originalPom = pom("/projects/spring-cloud-contract");
		File pomInTemp = tmpFile("/spring-cloud-contract/pom.xml");
		ModelWrapper rootPom = model("spring-cloud-contract-parent");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isNotEqualTo(asString(originalPom));
		Model overriddenPomModel = this.pomReader.readPom(storedPom);
		BDDAssertions.then(overriddenPomModel.getVersion())
				.isEqualTo("0.0.2.BUILD-SNAPSHOT");
		BDDAssertions.then(overriddenPomModel.getParent().getVersion())
				.isEqualTo("0.0.2");
	}

	@Test
	public void should_not_update_the_model_when_project_uses_same_version_for_artifact()
			throws Exception {
		File originalPom = pom("/projects/project/",
				"pom_matching_artifact_same_version.xml");
		File pomInTemp = tmpFile("/project/pom_matching_artifact_same_version.xml");
		ModelWrapper rootPom = model("spring-cloud-sleuth");
		ModelWrapper model = this.pomUpdater.updateModel(rootPom, pomInTemp,
				this.versions);

		File storedPom = this.pomUpdater.overwritePomIfDirty(model, this.versions,
				pomInTemp);

		BDDAssertions.then(asString(storedPom)).isEqualTo(asString(originalPom));
	}

	Set<Project> projects() {
		Set<Project> projects = new HashSet<>();
		projects.add(new Project("spring-cloud-contract", "0.0.2.BUILD-SNAPSHOT"));
		projects.add(new Project("spring-cloud-sleuth", "0.0.3.BUILD-SNAPSHOT"));
		projects.add(new Project("spring-cloud-vault", "0.0.4.BUILD-SNAPSHOT"));
		return projects;
	}

	private ModelWrapper model(String projectName) {
		Model parent = new Model();
		parent.setArtifactId(projectName);
		return new ModelWrapper(parent);
	}

	private ModelWrapper model(String projectName, String groupId) {
		Model parent = new Model();
		parent.setArtifactId(projectName);
		parent.setGroupId(groupId);
		return new ModelWrapper(parent);
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(GitRepoTests.class.getResource(relativePath).toURI());
	}

	private File pom(String relativePath) throws URISyntaxException {
		return pom(relativePath, "pom.xml");
	}

	private File pom(String relativePath, String pomName) throws URISyntaxException {
		return new File(new File(GitRepoTests.class.getResource(relativePath).toURI()),
				pomName);
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

}
