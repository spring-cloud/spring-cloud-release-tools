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

package releaser.internal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.maven.model.Model;
import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import releaser.SpringCloudReleaserProperties;
import releaser.internal.buildsystem.MavenBomParserAccessor;
import releaser.internal.buildsystem.ProjectPomUpdater;
import releaser.internal.buildsystem.TestUtils;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.tech.PomReader;

import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class PomUpdateAcceptanceTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	@Test
	public void should_update_all_versions_for_a_release_train() throws Exception {
		ReleaserProperties releaserProperties = releaserProperties();
		releaserProperties.getFixedVersions().put("checkstyle", "100.0.0.RELEASE");
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties,
				Collections
						.singletonList(MavenBomParserAccessor.maven(releaserProperties)));
		Projects projects = projectPomUpdater.retrieveVersionsFromReleaseTrainBom();
		File project = new File(this.temporaryFolder, "/spring-cloud-sleuth");

		projectPomUpdater.updateProjectFromReleaseTrain(project, projects,
				projects.forFile(project), true);

		then(this.temporaryFolder).exists();
		Model rootPom = PomReader.readPom(tmpFile("/spring-cloud-sleuth/pom.xml"));
		Model depsPom = PomReader.readPom(
				tmpFile("/spring-cloud-sleuth/spring-cloud-sleuth-dependencies/pom.xml"));
		Model corePom = PomReader.readPom(
				tmpFile("/spring-cloud-sleuth/spring-cloud-sleuth-core/pom.xml"));
		Model zipkinStreamPom = PomReader.readPom(tmpFile(
				"/spring-cloud-sleuth/spring-cloud-sleuth-samples/spring-cloud-sleuth-sample-zipkin-stream/pom.xml"));
		then(rootPom.getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		then(rootPom.getProperties())
				.containsEntry("spring-cloud-commons.version", "1.2.0.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-stream.version", "Chelsea.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-netflix.version", "1.3.0.BUILD-SNAPSHOT")
				.containsEntry("checkstyle.version", "100.0.0.RELEASE");
		then(depsPom.getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		then(depsPom.getParent().getVersion()).isEqualTo("0.3.1.BUILD-SNAPSHOT");
		then(corePom.getParent().getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
		then(zipkinStreamPom.getParent().getVersion()).isEqualTo("1.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_not_fail_when_after_updating_a_release_version_there_still_is_a_snapshot_version()
			throws Exception {
		ReleaserProperties releaserProperties = branchReleaserProperties();
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties,
				Collections
						.singletonList(MavenBomParserAccessor.maven(releaserProperties)));
		Projects projects = projectPomUpdater.retrieveVersionsFromReleaseTrainBom();
		projects.add(new ProjectVersion("spring-cloud-sleuth-samples", "0.0.5.RELEASE"));
		File project = new File(this.temporaryFolder,
				"/spring-cloud-sleuth-with-unmatched-property/spring-cloud-sleuth-samples");
		addBuildSnapshotToChildPom(project);

		projectPomUpdater.updateProjectFromReleaseTrain(project, projects,
				projects.forFile(project), true);
	}

	private void addBuildSnapshotToChildPom(File project) throws IOException {
		File childPom = new File(project, "pom.xml");
		String text = new String(Files.readAllBytes(childPom.toPath()));
		Files.write(childPom.toPath(),
				text.replaceAll("1.19.2", "1.19.2.BUILD-SNAPSHOT").getBytes());
	}

	@Test
	public void should_not_fail_update_when_after_updating_a_release_version_there_still_is_a_snapshot_version_in_a_non_deployable_module()
			throws Exception {
		ReleaserProperties releaserProperties = branchReleaserProperties();
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties,
				Collections
						.singletonList(MavenBomParserAccessor.maven(releaserProperties)));
		Projects projects = projectPomUpdater.retrieveVersionsFromReleaseTrainBom();
		File project = new File(this.temporaryFolder,
				"/spring-cloud-sleuth-with-unmatched-property");

		BDDAssertions
				.thenThrownBy(() -> projectPomUpdater.updateProjectFromReleaseTrain(
						project, projects, projects.forFile(project), true))
				.hasMessageContaining("<version>0.3.1.BUILD-SNAPSHOT</version>");
	}

	@Test
	public void should_update_fail_when_after_updating_a_release_version_there_still_is_a_snapshot_version_for_boot_snapshot_version()
			throws Exception {
		ReleaserProperties releaserProperties = branchReleaserProperties();
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties,
				Collections
						.singletonList(MavenBomParserAccessor.maven(releaserProperties)));
		Projects projects = projectPomUpdater.retrieveVersionsFromReleaseTrainBom();
		projects.removeIf(projectVersion -> projectVersion.projectName
				.contains("spring-cloud-build"));
		projects.add(new ProjectVersion("spring-cloud-build", "1.4.2.BUILD-SNAPSHOT"));
		File project = new File(this.temporaryFolder, "/spring-cloud-sleuth");

		BDDAssertions
				.thenThrownBy(() -> projectPomUpdater.updateProjectFromReleaseTrain(
						project, projects, projects.forFile(project), true))
				.hasMessageContaining("<version>1.4.2.BUILD-SNAPSHOT</version>");
	}

	@Test
	public void should_not_update_a_project_that_is_not_on_the_list() throws Exception {
		ReleaserProperties releaserProperties = releaserProperties();
		ProjectPomUpdater projectPomUpdater = new ProjectPomUpdater(releaserProperties,
				Collections
						.singletonList(MavenBomParserAccessor.maven(releaserProperties)));
		File beforeProcessing = pom("/projects/project/");
		Projects projects = projectPomUpdater.retrieveVersionsFromReleaseTrainBom();
		File project = tmpFile("/project/");

		projectPomUpdater.updateProjectFromReleaseTrain(project, projects,
				new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"), true);

		then(this.temporaryFolder).exists();
		File afterProcessing = tmpFile("/project/pom.xml");
		then(asString(beforeProcessing)).isEqualTo(asString(afterProcessing));
	}

	private ReleaserProperties releaserProperties() throws URISyntaxException {
		ReleaserProperties releaserProperties = SpringCloudReleaserProperties.get();
		releaserProperties.getGit().setReleaseTrainBomUrl(
				file("/projects/spring-cloud-release/").toURI().toString());
		return releaserProperties;
	}

	private ReleaserProperties branchReleaserProperties() throws URISyntaxException {
		ReleaserProperties releaserProperties = releaserProperties();
		releaserProperties.getPom().setBranch("vCamden.SR4");
		return releaserProperties;
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(PomUpdateAcceptanceTests.class.getResource(relativePath).toURI());
	}

	private File pom(String relativePath) throws URISyntaxException {
		return new File(
				new File(
						PomUpdateAcceptanceTests.class.getResource(relativePath).toURI()),
				"pom.xml");
	}

	private String asString(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

}
