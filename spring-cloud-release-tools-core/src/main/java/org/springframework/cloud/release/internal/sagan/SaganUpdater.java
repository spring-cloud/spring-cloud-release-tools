package org.springframework.cloud.release.internal.sagan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
public class SaganUpdater {

	private static final Logger log = LoggerFactory.getLogger(SaganUpdater.class);

	private final ReleaserProperties properties;
	private final SaganClient saganClient;

	public SaganUpdater(ReleaserProperties properties, SaganClient saganClient) {
		this.properties = properties;
		this.saganClient = saganClient;
	}

	public void updateSagan(ProjectVersion version) {
		ReleaseUpdate update = new ReleaseUpdate();
		update.groupId = version.groupId();
		update.artifactId = version.projectName;
		update.version = version.version;
		update.apiDocUrl = "http://github.com/spring-cloud/" + version.projectName;
		update.refDocUrl = referenceUrl(version);
		log.info("Updating Sagan with \n\n{}", update);
		this.saganClient.createOrUpdateRelease(version.projectName, update);
	}

	private String referenceUrl(ProjectVersion version) {
		if (version.isRelease()) {
			return "http://cloud.spring.io/spring-cloud-static/" + version.projectName + "/{version}/";
		}
		// is from master ?
		// if not pick from /1.1.x/ url
		return "http://cloud.spring.io/spring-cloud-sleuth/" + version.projectName + ".html";
	}
}
