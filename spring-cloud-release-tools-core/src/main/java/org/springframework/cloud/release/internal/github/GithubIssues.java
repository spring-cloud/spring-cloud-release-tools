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

package org.springframework.cloud.release.internal.github;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;
import com.jcabi.http.wire.RetryWire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class GithubIssues {

	private static final Logger log = LoggerFactory.getLogger(GithubIssues.class);

	private static final String GITHUB_ISSUE_TITLE = "release of the [%s] release train took place";

	private final Github github;

	private final ReleaserProperties properties;

	GithubIssues(ReleaserProperties properties) {
		this(new RtGithub(new RtGithub(properties.getGit().getOauthToken()).entry()
				.through(RetryWire.class)), properties);
	}

	GithubIssues(Github github, ReleaserProperties properties) {
		this.github = new CachingGithub(github);
		this.properties = properties;
	}

	void fileIssueInSpringGuides(Projects projects, ProjectVersion version) {
		if (!this.properties.getGit().isUpdateSpringGuides()) {
			log.info("Will not file an issue to Spring Guides, since the switch to do so "
					+ "is off. Set [releaser.git.update-spring-guides] to [true] to change that");
			return;
		}
		fileAGitHubIssue("spring-guides", "getting-started-guides", projects, version);
		// iterate over projects, checkout the tag, build the guides project
		// only with -Pintegration,guides profile
	}

	void fileIssueInStartSpringIo(Projects projects, ProjectVersion version) {
		if (!this.properties.getGit().isUpdateStartSpringIo()) {
			log.info(
					"Will not file an issue to Start Spring Io, since the switch to do so "
							+ "is off. Set [releaser.git.update-start-spring-io] to [true] to change that");
			return;
		}
		fileAGitHubIssue("spring-io", "start.spring.io", projects, version);
		// iterate over projects, checkout the tag, build the guides project
		// only with -Pintegration,guides profile
	}

	private void fileAGitHubIssue(String user, String repo, Projects projects,
			ProjectVersion version) {
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You have to pass Github OAuth token for milestone closing to be operational");
		// do this only for RELEASE & SR
		String releaseVersion = parsedVersion();
		if (version.isSnapshot()) {
			log.info(
					"Github issue creation will occur only for non snapshot versions. Your version is [{}]",
					releaseVersion);
			return;
		}
		fileAGithubIssue(user, repo, projects, releaseVersion);
	}

	private void fileAGithubIssue(String user, String repo, Projects projects,
			String releaseVersion) {
		Repo ghRepo = this.github.repos().get(new Coordinates.Simple(user, repo));
		String issueTitle = "[" + StringUtils.capitalize(releaseVersion) + "] "
				+ String.format(GITHUB_ISSUE_TITLE,
						this.properties.getMetaRelease().getReleaseTrainProjectName());
		// check if the issue is not already there
		boolean issueAlreadyFiled = issueAlreadyFiled(ghRepo, issueTitle);
		if (issueAlreadyFiled) {
			log.info("Issue already filed, will not do that again");
			return;
		}
		try {
			int number = ghRepo.issues().create(issueTitle, issueText(projects)).number();
			log.info(
					"Successfully created an issue with "
							+ "title [{}] for the [{}/{}] GitHub repository" + number,
					issueTitle, user, repo);
		}
		catch (IOException e) {
			log.error("Exception occurred while trying to create the issue in guides", e);
		}
	}

	private String parsedVersion() {
		String version = this.properties.getPom().getBranch();
		if (version.startsWith("v")) {
			return version.substring(1);
		}
		return version;
	}

	private String issueText(Projects projects) {
		StringBuilder builder = new StringBuilder().append("Release train [")
				.append(this.properties.getMetaRelease().getReleaseTrainProjectName())
				.append("] in version [").append(parsedVersion())
				.append("] released with the following projects:").append("\n\n");
		projects.forEach(project -> builder.append(project.projectName).append(" : ")
				.append("`").append(project.version).append("`").append("\n"));
		return builder.toString();
	}

	private boolean issueAlreadyFiled(Repo springGuides, String issueTitle) {
		Map<String, String> map = new HashMap<>();
		map.put("state", "open");
		int counter = 0;
		int maxIssues = 10;
		for (Issue issue : springGuides.issues().iterate(map)) {
			if (counter >= maxIssues) {
				return false;
			}
			Issue.Smart smartIssue = new Issue.Smart(issue);
			try {
				if (issueTitle.equals(smartIssue.title())) {
					return true;
				}
			}
			catch (IOException e) {
				return false;
			}
			counter = counter + 1;
		}
		return false;
	}

}
