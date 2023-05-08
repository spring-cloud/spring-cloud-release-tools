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

package releaser.cloud.github;

import org.kohsuke.github.GitHub;
import releaser.internal.ReleaserProperties;
import releaser.internal.github.CustomGithubIssues;
import releaser.internal.github.GithubIssueFiler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import org.springframework.util.StringUtils;

class SpringCloudGithubIssues implements CustomGithubIssues {

	private static final String GITHUB_ISSUE_TITLE = "Upgrade to Spring Cloud %s";

	private final GithubIssueFiler githubIssueFiler;

	private final ReleaserProperties properties;

	SpringCloudGithubIssues(ReleaserProperties properties) {
		this.githubIssueFiler = new GithubIssueFiler(properties);
		this.properties = properties;
	}

	SpringCloudGithubIssues(GitHub github, ReleaserProperties properties) {
		this.githubIssueFiler = new GithubIssueFiler(github, properties);
		this.properties = properties;
	}

	@Override
	public void fileIssueInSpringGuides(Projects projects, ProjectVersion version) {
		String user = getGuidesOrg();
		String repo = getGuidesRepo();
		this.githubIssueFiler.fileAGitHubIssue(user, repo, version, issueTitle(), guidesIssueText(projects));
	}

	@Override
	public void fileIssueInStartSpringIo(Projects projects, ProjectVersion version) {
		String user = getStartSpringIoOrg();
		String repo = getStartSpringIoRepo();
		this.githubIssueFiler.fileAGitHubIssue(user, repo, version, issueTitle(), startSpringIoIssueText(projects));
	}

	String getGuidesOrg() {
		return "spring-guides";
	}

	String getGuidesRepo() {
		return "getting-started-guides";
	}

	String getStartSpringIoOrg() {
		return "spring-io";
	}

	String getStartSpringIoRepo() {
		return "start.spring.io";
	}

	private String issueTitle() {
		return String.format(GITHUB_ISSUE_TITLE, StringUtils.capitalize(parsedVersion()));
	}

	private String parsedVersion() {
		String version = this.properties.getPom().getBranch();
		if (version.startsWith("v")) {
			return version.substring(1);
		}
		return version;
	}

	private String startSpringIoIssueText(Projects projects) {
		String springBootVersion = projects.containsProject("spring-boot") ? projects.forName("spring-boot").version
				: "";
		return "Release train [" + this.properties.getMetaRelease().getReleaseTrainProjectName() + "] in version ["
				+ parsedVersion() + "] released with the Spring Boot version [`" + springBootVersion + "`]";
	}

	private String guidesIssueText(Projects projects) {
		StringBuilder builder = new StringBuilder().append("Release train [")
				.append(this.properties.getMetaRelease().getReleaseTrainProjectName()).append("] in version [")
				.append(parsedVersion()).append("] released with the following projects:").append("\n\n");
		projects.forEach(project -> builder.append(project.projectName).append(" : ").append("`")
				.append(project.version).append("`").append("\n"));
		return builder.toString();
	}

}
