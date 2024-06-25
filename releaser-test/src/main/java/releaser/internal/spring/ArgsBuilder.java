/*
 * Copyright 2013-2022 the original author or authors.
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

package releaser.internal.spring;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import releaser.internal.git.GitTestUtils;

public class ArgsBuilder {

	private final File project;

	private final File tempDirTestSamplesProject;

	private final File tempDirReleaseTrainDocs;

	private final File tempDirSpringCloud;

	private final File tempDirReleaseTrainWiki;

	private final File tempDirAllTestSample;

	private File tempDirSpringDocsActions;

	List<String> args = new LinkedList<>();

	public ArgsBuilder(File project, File tempDirTestSamplesProject, File tempDirReleaseTrainDocs,
			File tempDirSpringCloud, File tempDirReleaseTrainWiki, File tempDirAllTestSample) throws Exception {
		this.project = project;
		this.tempDirTestSamplesProject = tempDirTestSamplesProject;
		this.tempDirReleaseTrainDocs = tempDirReleaseTrainDocs;
		this.tempDirSpringCloud = tempDirSpringCloud;
		this.tempDirReleaseTrainWiki = tempDirReleaseTrainWiki;
		this.tempDirAllTestSample = tempDirAllTestSample;
		defaults();
	}

	public ArgsBuilder defaults() throws Exception {
		// @formatter:off
		this.args.addAll(Arrays.asList(
				"releaser.git.documentation-url=" + file("/projects/spring-cloud-static-angel/").toURI(),
				"releaser.git.test-samples-project-url=" + tempDirTestSamplesProject.getAbsolutePath(),
				"releaser.git.release-train-docs-url=" + tempDirReleaseTrainDocs.getAbsolutePath(),
				"releaser.antora.spring-docs-actions-url=" + file("/projects/spring-docs-actions").toURI(),
				"releaser.antora.spring-docs-actions-tag=v0.0.16",
				"releaser.maven.build-command=echo build",
				"releaser.maven.deploy-command=echo deploy",
				"releaser.maven.deploy-guides-command=echo guides",
				"releaser.maven.publish-docs-command=echo docs",
				"releaser.maven.generate-release-train-docs-command=echo releaseTrainDocs",
				"releaser.maven.run-antora-command=echo antora",
				"releaser.working-dir=" + project.getPath(),
				"releaser.git.spring-project-url=" + tempDirSpringCloud.getAbsolutePath(),
				"releaser.git.release-train-wiki-url=" + tmpGitRepo(tempDirReleaseTrainWiki).getAbsolutePath() + "/",
				"releaser.git.run-updated-samples=true",
				"releaser.git.update-spring-guides=true",
				"releaser.git.update-spring-project=true",
				"releaser.git.update-start-spring-io=true",
				"releaser.git.update-all-test-samples=true",
				"releaser.git.update-documentation-repo=true",
				"releaser.git.update-github-milestones=true",
				"releaser.git.update-release-train-docs=true",
				"releaser.git.all-test-sample-urls[spring-cloud-consul]=" + tempDirAllTestSample.getAbsolutePath(),
				"releaser.sagan.update-sagan=true",
				"releaser.template.enabled=true",
				"releaser.versions.all-versions-file-url="
						+ ArgsBuilder.class.getResource("/raw/initializr.yml").toURI()
		));
		// @formatter:on
		fetchVersionsFromGit(true);
		updateReleaseTrainWiki(true);
		projectName("spring-cloud-consul");
		return this;
	}

	public ArgsBuilder updateReleaseTrainWiki(boolean enabled) throws Exception {
		removeIfPresent("releaser.git.update-release-train-wiki");
		this.args.add("releaser.git.update-release-train-wiki=" + enabled);
		return this;
	}

	private void removeIfPresent(String string) {
		this.args.stream().filter(s -> s.startsWith(string)).findAny().ifPresent(s -> this.args.remove(s));
	}

	public ArgsBuilder projectsToSkip(String... toSkip) throws Exception {
		removeIfPresent("releaser.meta-release.projects-to-skip");
		this.args.add("releaser.meta-release.projects-to-skip=" + String.join(",", toSkip));
		return this;
	}

	public ArgsBuilder gitOrgUrl(String url) throws Exception {
		removeIfPresent("releaser.meta-release.git-org-url");
		this.args.add("releaser.meta-release.git-org-url=" + url);
		return this;
	}

	public ArgsBuilder mavenBuildCommand(String command) throws Exception {
		removeIfPresent("releaser.maven.build-command");
		this.args.add("releaser.maven.build-command=" + command);
		return this;
	}

	public ArgsBuilder mavenDeployCommand(String command) throws Exception {
		removeIfPresent("releaser.maven.deploy-command");
		this.args.add("releaser.maven.deploy-command=" + command);
		return this;
	}

	public ArgsBuilder mavenPublishCommand(String command) throws Exception {
		removeIfPresent("releaser.maven.publish-docs-command");
		this.args.add("releaser.maven.publish-docs-command=" + command);
		return this;
	}

	public ArgsBuilder metaReleaseGroups(String... groups) throws Exception {
		for (int i = 0; i < groups.length; i++) {
			removeIfPresent("releaser.meta-release.release-groups[" + i + "]");
			this.args.add("releaser.meta-release.release-groups[" + i + "]=" + groups[i]);
		}
		return this;
	}

	public ArgsBuilder addFixedVersion(String projectName, String projectVersion) {
		removeIfPresent("releaser.fixed-versions[" + projectName + "]");
		this.args.add("releaser.fixed-versions[" + projectName + "]=" + projectVersion);
		return this;
	}

	public ArgsBuilder addFixedVersions(Map<String, String> versions) throws Exception {
		versions.forEach(this::addFixedVersion);
		return this;
	}

	public ArgsBuilder releaseTrainBomUrl(String url) throws Exception {
		removeIfPresent("releaser.git.release-train-bom-url");
		this.args.add("releaser.git.release-train-bom-url=" + url);
		return this;
	}

	public ArgsBuilder fetchVersionsFromGit(boolean fetch) throws Exception {
		removeIfPresent("releaser.git.fetch-versions-from-git");
		this.args.add("releaser.git.fetch-versions-from-git=" + fetch);
		return this;
	}

	public ArgsBuilder cloneDestinationDirectory(File cloneDestinationDirectory) throws Exception {
		removeIfPresent("releaser.git.clone-destination-dir");
		this.args.add("releaser.git.clone-destination-dir=" + cloneDestinationDirectory.toString());
		return this;
	}

	public ArgsBuilder releaseTrainUrl(String relativePath) throws Exception {
		removeIfPresent("releaser.git.release-train-bom-url");
		this.args.add("releaser.git.release-train-bom-url=" + file(relativePath).toURI());
		return this;
	}

	public ArgsBuilder projectName(String projectName) {
		removeIfPresent("test.projectName");
		this.args.add("test.projectName=" + projectName);
		return this;
	}

	public ArgsBuilder expectedVersion(String expectedVersion) {
		removeIfPresent("test.expectedVersion");
		this.args.add("test.expectedVersion=" + expectedVersion);
		return this;
	}

	public ArgsBuilder chosenOption(String chosenOption) {
		removeIfPresent("test.chosenOption");
		this.args.add("test.chosenOption=" + chosenOption);
		return this;
	}

	public ArgsBuilder bomBranch(String bomBranch) {
		removeIfPresent("releaser.pom.branch");
		this.args.add("releaser.pom.branch=" + bomBranch);
		return this;
	}

	public ArgsBuilder commercial(boolean commercial) {
		removeIfPresent("releaser.commercial");
		this.args.add("releaser.commercial=" + commercial);
		return this;
	}

	public ArgsBuilder projectReleaseBundle(boolean enable) {
		removeIfPresent("releaser.bundles.create-project-release-bundle");
		this.args.add("releaser.bundles.create-project-release-bundle=" + enable);
		return this;
	}

	public ArgsBuilder distributeProjectReleaseBundle(boolean enable) {
		removeIfPresent("releaser.bundles.distribute-project-release-bundle");
		this.args.add("releaser.bundles.distribute-project-release-bundle=" + enable);
		return this;
	}

	public ArgsBuilder releaseTrainSourceReleaseBundle(boolean enable) {
		removeIfPresent("releaser.bundles.create-release-train-release-bundle");
		this.args.add("releaser.bundles.create-release-train-release-bundle=" + enable);
		return this;
	}

	public ArgsBuilder distributeReleaseTrainSourceReleaseBundle(boolean enable) {
		removeIfPresent("releaser.bundles.distribute-release-train-source-bundle");
		this.args.add("releaser.bundles.distribute-release-train-source-bundle=" + enable);
		return this;
	}

	public String[] build() {
		return args.toArray(new String[0]);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(ArgsBuilder.class.getResource(relativePath).toURI());
	}

	private File tmpGitRepo(File tempDir) {
		GitTestUtils.initGitProject(tempDir);
		return tempDir;
	}

}
