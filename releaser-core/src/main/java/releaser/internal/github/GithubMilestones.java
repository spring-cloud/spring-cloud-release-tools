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
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class GithubMilestones {

	static final Map<ProjectVersion, String> MILESTONE_URL_CACHE = new ConcurrentHashMap<>();
	static final Map<ProjectVersion, GHMilestone> MILESTONE_CACHE = new ConcurrentHashMap<>();

	private static final Logger log = LoggerFactory.getLogger(GithubMilestones.class);

	private final GitHub github;

	private final ReleaserProperties properties;

	GithubMilestones(ReleaserProperties properties) {
		this(CachingGithub.getInstance(properties.getGit().getOauthToken(), properties.getGit().getCacheDirectory()),
				properties);
	}

	GithubMilestones(GitHub github, ReleaserProperties properties) {
		this.github = github;
		this.properties = properties;
	}

	void closeMilestone(ProjectVersion version) {
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You must set the value of the OAuth token. You can do it "
						+ "either via the command line [--releaser.git.oauth-token=...] "
						+ "or put it as an env variable in [~/.bashrc] or "
						+ "[~/.zshrc] e.g. [export RELEASER_GIT_OAUTH_TOKEN=...]");
		GHMilestone foundMilestone = MILESTONE_CACHE.get(version);
		String tagVersion = version.version;
		if (foundMilestone == null) {
			foundMilestone = matchingMilestone(tagVersion, openMilestones(version));
			if (foundMilestone != null) {
				MILESTONE_CACHE.put(version, foundMilestone);
			}
		}
		if (foundMilestone != null) {
			try {
				log.info("Found a matching milestone - closing it");
				foundMilestone.close();
				log.info("Closed the [{}] milestone", tagVersion);
			}
			catch (IOException e) {
				log.error("Exception occurred while trying to retrieve the milestone", e);
			}
		}
		else {
			log.warn("No matching milestone was found");
		}
	}

	GHMilestone matchingMilestone(String tagVersion, Iterable<GHMilestone> milestones) {
		log.debug("Successfully received list of milestones [{}]", milestones);
		log.info("Will try to match against tag version [{}]", tagVersion);
		try {
			int counter = 0;
			for (GHMilestone milestone : milestones) {
				if (counter++ >= this.properties.getGit().getNumberOfCheckedMilestones()) {
					log.warn(
							"No matching milestones were found within the provided threshold [{}] of checked milestones",
							this.properties.getGit().getNumberOfCheckedMilestones());
					return null;
				}
				String title = milestone.getTitle();
				if (tagVersion.equals(title) || numericVersion(tagVersion).equals(title)) {
					log.info("Found a matching milestone [{}]", milestone.getNumber());
					return milestone;
				}
			}
		}
		catch (Exception e) {
			log.error("Exception occurred while trying to retrieve the milestone", e);
			return null;
		}
		log.warn("No matching milestones were found");
		return null;
	}

	String milestoneUrl(ProjectVersion version) {
		String cachedUrl = MILESTONE_URL_CACHE.get(version);
		if (StringUtils.hasText(cachedUrl)) {
			return cachedUrl;
		}
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You have to pass Github OAuth token for milestone closing to be operational");
		String tagVersion = version.version;
		GHMilestone foundMilestone = matchingMilestone(tagVersion, closedMilestones(version));
		String foundUrl = "";
		if (foundMilestone != null) {
			try {
				URL url = foundMilestoneUrl(foundMilestone);
				log.info("Found a matching milestone with issues URL [{}]", url);
				foundUrl = url.toString().replace("https://api.github.com/repos", "https://github.com")
						.replace("milestones", "milestone") + "?closed=1";
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			catch (Exception e) {
				log.error("Exception occurred while trying to find milestone", e);
			}
		}
		MILESTONE_URL_CACHE.put(version, foundUrl);
		return foundUrl;
	}

	private String numericVersion(String version) {
		return version.contains("RELEASE") ? version.substring(0, version.lastIndexOf(".")) : "";
	}

	String milestoneTitle(GHMilestone milestone) throws IOException {
		return milestone.getTitle();
	}

	URL foundMilestoneUrl(GHMilestone milestone) throws IOException {
		return milestone.getUrl();
	}

	private Iterable<GHMilestone> openMilestones(ProjectVersion version) {
		try {
			return this.github.getRepository(org() + "/" + version.projectName).listMilestones(GHIssueState.OPEN)
					.toList();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Iterable<GHMilestone> closedMilestones(ProjectVersion version) {
		try {
			return this.github.getRepository(org() + "/" + version.projectName).listMilestones(GHIssueState.CLOSED)
					.toList();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	String org() {
		return this.properties.getGit().getOrgName();
	}

}
