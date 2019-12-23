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

package releaser.internal.github;

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
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

import org.springframework.util.Assert;

/**
 * Infrastructure component for filing Github issues.
 *
 * @author Marcin Grzejszczak
 */
public class GithubIssueFiler {

	private static final Logger log = LoggerFactory.getLogger(GithubIssues.class);

	private final Github github;

	private final ReleaserProperties properties;

	public GithubIssueFiler(ReleaserProperties properties) {
		this(new RtGithub(new RtGithub(properties.getGit().getOauthToken()).entry()
				.through(RetryWire.class)), properties);
	}

	public GithubIssueFiler(Github github, ReleaserProperties properties) {
		this.github = new CachingGithub(github);
		this.properties = properties;
	}

	public void fileAGitHubIssue(String user, String repo, ProjectVersion version,
			String issueTitle, String issueText) {
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You have to pass Github OAuth token for milestone closing to be operational");
		// do this only for RELEASE & SR
		if (version.isSnapshot()) {
			log.info(
					"Github issue creation will occur only for non snapshot versions. Your version is [{}]",
					parsedVersion());
			return;
		}
		fileAGithubIssue(user, repo, issueTitle, issueText);
	}

	private void fileAGithubIssue(String user, String repo, String issueTitle,
			String issueText) {
		Repo ghRepo = this.github.repos().get(new Coordinates.Simple(user, repo));
		// check if the issue is not already there
		boolean issueAlreadyFiled = issueAlreadyFiled(ghRepo, issueTitle);
		if (issueAlreadyFiled) {
			log.info("Issue already filed, will not do that again");
			return;
		}
		try {
			int number = ghRepo.issues().create(issueTitle, issueText).number();
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
