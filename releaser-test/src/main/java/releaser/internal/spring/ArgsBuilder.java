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

package releaser.internal.spring;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.rules.TemporaryFolder;
import releaser.internal.git.GitTestUtils;

public class ArgsBuilder {

	private final File project;

	private final TemporaryFolder tmp;

	List<String> args = new LinkedList<>();

	public ArgsBuilder(File project, TemporaryFolder tmp) throws Exception {
		this.project = project;
		this.tmp = tmp;
		defaults();
	}

	public ArgsBuilder defaults() throws Exception {
		// @formatter:off
		this.args.addAll(Arrays.asList(
				"releaser.git.documentation-url=" + file("/projects/spring-cloud-static-angel/").toURI().toString(),
				"releaser.git.test-samples-project-url=" + this.tmp.newFolder().toString(),
				"releaser.git.release-train-docs-url=" + this.tmp.newFolder().toString(),
				"releaser.maven.build-command=echo build",
				"releaser.maven.deploy-command=echo deploy",
				"releaser.maven.deploy-guides-command=echo guides",
				"releaser.maven.publish-docs-command=echo docs",
				"releaser.maven.generate-release-train-docs-command=echo releaseTrainDocs",
				"releaser.working-dir=" + project.getPath(),
				"releaser.git.spring-project-url=" + tmpFile("spring-cloud").getAbsolutePath() + "/",
				"releaser.git.release-train-wiki-url=" + tmpGitRepo("spring-cloud-wiki").getAbsolutePath() + "/",
				"releaser.git.run-updated-samples=true",
				"releaser.git.update-spring-guides=true",
				"releaser.git.update-spring-project=true",
				"releaser.git.update-start-spring-io=true",
				"releaser.git.update-all-test-samples=true",
				"releaser.git.update-documentation-repo=true",
				"releaser.git.update-github-milestones=true",
				"releaser.git.update-release-train-docs=true",
				"releaser.git.all-test-sample-urls[spring-cloud-consul]=" + this.tmp.newFolder().toString(),
				"releaser.sagan.update-sagan=true",
				"releaser.template.enabled=true",
				"releaser.versions.all-versions-file-url="
						+ ArgsBuilder.class.getResource("/raw/initializr.yml").toURI().toString()
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
		this.args.stream().filter(s -> s.startsWith(string)).findAny()
				.ifPresent(s -> this.args.remove(s));
	}

	public ArgsBuilder projectsToSkip(String... toSkip) throws Exception {
		removeIfPresent("releaser.meta-release.projects-to-skip");
		this.args.add(
				"releaser.meta-release.projects-to-skip=" + String.join(",", toSkip));
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

	public ArgsBuilder cloneDestinationDirectory(File cloneDestinationDirectory)
			throws Exception {
		removeIfPresent("releaser.git.clone-destination-dir");
		this.args.add("releaser.git.clone-destination-dir="
				+ cloneDestinationDirectory.toString());
		return this;
	}

	public ArgsBuilder releaseTrainUrl(String relativePath) throws Exception {
		removeIfPresent("releaser.git.release-train-bom-url");
		this.args.add("releaser.git.release-train-bom-url="
				+ file(relativePath).toURI().toString());
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

	public String[] build() {
		return args.toArray(new String[0]);
	}

	private File file(String relativePath) throws URISyntaxException {
		return new File(ArgsBuilder.class.getResource(relativePath).toURI());
	}

	private File tmpFile(String relativePath) {
		try {
			return new File(this.tmp.newFolder(), relativePath);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private File tmpGitRepo(String relativePath) {
		try {
			File file = new File(this.tmp.newFolder(), relativePath);
			GitTestUtils.initGitProject(file);
			return file;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
