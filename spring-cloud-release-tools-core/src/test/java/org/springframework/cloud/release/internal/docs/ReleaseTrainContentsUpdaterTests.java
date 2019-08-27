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

package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.buildsystem.TestUtils;
import org.springframework.cloud.release.internal.git.GitTestUtils;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.util.FileSystemUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseTrainContentsUpdaterTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	ReleaserProperties properties = new ReleaserProperties();

	ProjectGitHubHandler projectGitHubHandler = new ProjectGitHubHandler(
			this.properties) {
		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "http://www.foo.com/";
		}
	};

	ProjectGitHandler projectGitHandler = new ProjectGitHandler(this.properties);

	TemplateGenerator templateGenerator = new TemplateGenerator(this.properties,
			this.projectGitHubHandler);

	ReleaseTrainContentsUpdater updater = new ReleaseTrainContentsUpdater(this.properties,
			this.projectGitHandler, this.templateGenerator);

	File springCloudRepo;

	File wikiRepo;

	File temporaryFolder;

	@Before
	public void setup() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects"), this.temporaryFolder);
		this.springCloudRepo = new File(this.temporaryFolder, "spring-cloud/");
		this.wikiRepo = new File(this.temporaryFolder, "spring-cloud-wiki/");
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(
				ReleaseTrainContentsUpdaterTests.class.getResource(relativePath).toURI());
	}

	@Test
	public void should_do_nothing_when_switch_is_off() {
		this.properties.getGit().setUpdateSpringProject(false);

		File file = this.updater.updateProjectRepo(oldReleaseTrain());

		BDDAssertions.then(file).isNull();
	}

	@Test
	public void should_not_update_the_contents_of_spring_project_repo_when_release_train_smaller()
			throws GitAPIException {
		this.properties.getGit()
				.setSpringProjectUrl(this.springCloudRepo.getAbsolutePath() + "/");

		File file = this.updater.updateProjectRepo(oldReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions
				.then(GitTestUtils.openGitProject(file).log().call().iterator().next()
						.getShortMessage())
				.doesNotContain("Updating project page to release train");
	}

	@Test
	public void should_update_the_contents_of_spring_project_repo_when_release_train_greater()
			throws GitAPIException {
		this.properties.getGit()
				.setSpringProjectUrl(this.springCloudRepo.getAbsolutePath() + "/");

		File file = this.updater.updateProjectRepo(newReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions
				.then(GitTestUtils.openGitProject(file).log().call().iterator().next()
						.getShortMessage())
				.contains("Updating project page to release train [Edgware.SR7]");
	}

	@Test
	public void should_do_nothing_when_switch_is_off_for_wiki_update() {
		this.properties.getGit().setUpdateReleaseTrainWiki(false);

		File file = this.updater.updateReleaseTrainWiki(oldReleaseTrain());

		BDDAssertions.then(file).isNull();
	}

	@Test
	public void should_do_nothing_when_switch_is_off_for_meta_release() {
		this.properties.getGit().setUpdateReleaseTrainWiki(true);
		this.properties.getMetaRelease().setEnabled(false);

		File file = this.updater.updateReleaseTrainWiki(oldReleaseTrain());

		BDDAssertions.then(file).isNull();
	}

	@Test
	public void should_not_update_the_contents_of_wiki_repo_when_release_train_smaller()
			throws GitAPIException, IOException {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit()
				.setReleaseTrainWikiUrl(this.wikiRepo.getAbsolutePath() + "/");

		File file = this.updater.updateReleaseTrainWiki(oldReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions.then(edgwareWikiEntryContent(file)).doesNotContain("# Edgware.SR7")
				.doesNotContain(
						"Spring Cloud Consul `2.0.1.RELEASE` ([issues](http://www.foo.com/))");
		BDDAssertions
				.then(GitTestUtils.openGitProject(file).log().call().iterator().next()
						.getShortMessage())
				.doesNotContain("Updating project page to release train");
	}

	@Test
	public void should_update_the_contents_of_wiki_when_release_train_greater()
			throws GitAPIException, IOException {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit()
				.setReleaseTrainWikiUrl(this.wikiRepo.getAbsolutePath() + "/");

		File file = this.updater.updateReleaseTrainWiki(newReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions.then(edgwareWikiEntryContent(file)).contains("# Edgware.SR7")
				.contains(
						"Spring Cloud Consul `2.0.1.RELEASE` ([issues](http://www.foo.com/))");
		BDDAssertions
				.then(GitTestUtils.openGitProject(file).log().call().iterator().next()
						.getShortMessage())
				.contains("Updating project page to release train [Edgware.SR7]");
	}

	@Test
	public void should_generate_the_contents_of_wiki_when_release_train_missing()
			throws GitAPIException, IOException {
		this.properties.getMetaRelease().setEnabled(true);
		this.properties.getGit()
				.setReleaseTrainWikiUrl(this.wikiRepo.getAbsolutePath() + "/");

		File file = this.updater.updateReleaseTrainWiki(freshNewReleaseTrain());

		BDDAssertions.then(file).isNotNull();
		BDDAssertions.then(greenwichWikiEntryContent(file))
				.contains("# Greenwich.RELEASE").contains(
						"Spring Cloud Consul `2.0.1.RELEASE` ([issues](http://www.foo.com/))");
		BDDAssertions
				.then(GitTestUtils.openGitProject(file).log().call().iterator().next()
						.getShortMessage())
				.contains("Updating project page to release train [Greenwich.RELEASE]");
	}

	private String edgwareWikiEntryContent(File file) throws IOException {
		return string(file, "Spring-Cloud-Edgware-Release-Notes.md");
	}

	private String greenwichWikiEntryContent(File file) throws IOException {
		return string(file, "Spring-Cloud-Greenwich-Release-Notes.md");
	}

	private String string(File file, String s) throws IOException {
		return new String(Files.readAllBytes(new File(file, s).toPath()));
	}

	Projects oldReleaseTrain() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "2.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "2.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				// old release train
				new ProjectVersion("spring-cloud-release", "Dalston.SR1"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects newReleaseTrain() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "2.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "2.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				// newer release train
				new ProjectVersion("spring-cloud-release", "Edgware.SR7"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects freshNewReleaseTrain() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "2.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "2.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				// newer release train
				new ProjectVersion("spring-cloud-release", "Greenwich.RELEASE"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

}
