package org.springframework.cloud.release.internal.git;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.util.Assert;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Milestone;
import com.jcabi.github.RtGithub;
import com.jcabi.http.wire.RetryWire;

/**
 * @author Marcin Grzejszczak
 */
class MilestoneCloser {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Github github;
	private final ReleaserProperties properties;

	MilestoneCloser(ReleaserProperties properties) {
		this.github = new RtGithub(new RtGithub(
				properties.getGit().getOauthToken()).entry().through(RetryWire.class));
		this.properties = properties;
	}

	MilestoneCloser(Github github, ReleaserProperties properties) {
		this.github = github;
		this.properties = properties;
	}

	void closeMilestone(ProjectVersion version) {
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You have to pass Github OAuth token for milestone closing to be operational");
		Iterable<Milestone> milestones = milestones(version);
		log.info("Successfully received list of milestones");
		String tagVersion = version.version;
		log.info("Will try to match against tag version [{}]", tagVersion);
		boolean matchingMilestone = false;
		for (Milestone milestone : milestones) {
			Milestone.Smart smartMilestone = new Milestone.Smart(milestone);
			try {
				if (tagVersion.equals(milestoneTitle(smartMilestone))) {
					log.info("Found a matching milestone - closing it");
					smartMilestone.close();
					matchingMilestone = true;
					log.info("Closed the [{}] milestone", tagVersion);
				}
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		if (!matchingMilestone) {
			log.warn("No matching milestone was found");
		}
	}

	String milestoneTitle(Milestone.Smart milestone) throws IOException {
		return milestone.title();
	}

	private Iterable<Milestone> milestones(ProjectVersion version) {
		return this.github.repos()
				.get(new Coordinates.Simple(org(), version.projectName))
				.milestones().iterate(openMilestones());
	}

	String org() {
		return "spring-cloud";
	}

	private Map<String, String> openMilestones() {
		Map<String, String> params = new HashMap<>();
		params.put("state", "open");
		return params;
	}
}
