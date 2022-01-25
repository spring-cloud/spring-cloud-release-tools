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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Milestone;
import com.jcabi.github.RtGithub;
import com.jcabi.http.wire.RetryWire;
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
	static final Map<ProjectVersion, Milestone.Smart> MILESTONE_CACHE = new ConcurrentHashMap<>();

	private static final Logger log = LoggerFactory.getLogger(GithubMilestones.class);

	private final Github github;

	private final ReleaserProperties properties;

	GithubMilestones(ReleaserProperties properties) {
		this(new RtGithub(new RtGithub(properties.getGit().getOauthToken()).entry().through(RetryWire.class)),
				properties);
	}

	GithubMilestones(Github github, ReleaserProperties properties) {
		this.github = new CachingGithub(github);
		this.properties = properties;
	}

	void closeMilestone(ProjectVersion version) {
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You must set the value of the OAuth token. You can do it "
						+ "either via the command line [--releaser.git.oauth-token=...] "
						+ "or put it as an env variable in [~/.bashrc] or "
						+ "[~/.zshrc] e.g. [export RELEASER_GIT_OAUTH_TOKEN=...]");
		Milestone.Smart foundMilestone = MILESTONE_CACHE.get(version);
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

	Milestone.Smart matchingMilestone(String tagVersion, Iterable<Milestone> milestones) {
		log.debug("Successfully received list of milestones [{}]", milestones);
		log.info("Will try to match against tag version [{}]", tagVersion);
		try {
			int counter = 0;
			for (Milestone milestone : milestones) {
				if (counter++ >= this.properties.getGit().getNumberOfCheckedMilestones()) {
					log.warn(
							"No matching milestones were found within the provided threshold [{}] of checked milestones",
							this.properties.getGit().getNumberOfCheckedMilestones());
					return null;
				}
				Milestone.Smart smartMilestone = new Milestone.Smart(milestone);
				String title = milestoneTitle(smartMilestone);
				if (tagVersion.equals(title) || numericVersion(tagVersion).equals(title)) {
					log.info("Found a matching milestone [{}]", smartMilestone.number());
					return smartMilestone;
				}
			}
		}
		catch (AssertionError | IOException e) {
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
		Milestone.Smart foundMilestone = matchingMilestone(tagVersion, closedMilestones(version));
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

	String milestoneTitle(Milestone.Smart milestone) throws IOException {
		return milestone.title();
	}

	URL foundMilestoneUrl(Milestone.Smart milestone) throws IOException {
		return milestone.url();
	}

	private Iterable<Milestone> getMilestones(ProjectVersion version, Map<String, String> map) {
		try {
			return this.github.repos().get(new Coordinates.Simple(org(), version.projectName)).milestones()
					.iterate(map);
		}
		catch (AssertionError e) {
			log.error("Exception occurred while trying to fetch milestones", e);
			return new ArrayList<>();
		}
	}

	private Iterable<Milestone> openMilestones(ProjectVersion version) {
		return getMilestones(version, openMilestones());
	}

	private Iterable<Milestone> closedMilestones(ProjectVersion version) {
		return getMilestones(version, closedMilestones());
	}

	String org() {
		return this.properties.getGit().getOrgName();
	}

	private Map<String, String> openMilestones() {
		Map<String, String> params = new HashMap<>();
		params.put("state", "open");
		params.put("sort", "due_on");
		params.put("direction", "desc");
		return params;
	}

	private Map<String, String> closedMilestones() {
		Map<String, String> params = new HashMap<>();
		params.put("state", "closed");
		params.put("sort", "due_on");
		params.put("direction", "desc");
		return params;
	}

}
