package org.springframework.cloud.release.internal.sagan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
public class SaganUpdater {

	private static final Logger log = LoggerFactory.getLogger(SaganUpdater.class);

	private final SaganClient saganClient;

	public SaganUpdater(SaganClient saganClient) {
		this.saganClient = saganClient;
	}

	public void updateSagan(String branch, ProjectVersion version) {
		if (version.isMilestone() || version.isRc()) {
			log.info("Won't update Sagan about milestones / rc");
			return;
		}
		ReleaseUpdate update = new ReleaseUpdate();
		update.groupId = version.groupId();
		update.artifactId = version.projectName;
		update.version = version.version;
		update.apiDocUrl = "http://github.com/spring-cloud/" + version.projectName;
		update.refDocUrl = referenceUrl(branch, version);
		log.info("Updating Sagan with \n\n{}", update);
		this.saganClient.createOrUpdateRelease(version.projectName, update);
	}

	private String referenceUrl(String branch, ProjectVersion version) {
		if (version.isRelease()) {
			// static/sleuth/{version}/
			return "http://cloud.spring.io/spring-cloud-static/" + version.projectName + "/{version}/";
		}
		if (branch.toLowerCase().contains("master")) {
			// sleuth/
			return "http://cloud.spring.io/" + version.projectName + "/" + version.projectName + ".html";
		}
		// sleuth/1.1.x/
		return "http://cloud.spring.io/" + version.projectName + "/" + branch + "/";
	}
}
