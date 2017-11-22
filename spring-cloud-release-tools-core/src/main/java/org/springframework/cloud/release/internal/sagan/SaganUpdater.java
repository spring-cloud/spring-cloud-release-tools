package org.springframework.cloud.release.internal.sagan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author Marcin Grzejszczak
 */
public class SaganUpdater {

	private static final Logger log = LoggerFactory.getLogger(SaganUpdater.class);

	private final SaganClient saganClient;

	public SaganUpdater(SaganClient saganClient) {
		this.saganClient = saganClient;
	}

	public void updateSagan(String branch, ProjectVersion originalVersion, ProjectVersion version) {
		ReleaseUpdate update = releaseUpdate(branch, originalVersion, version);
		log.info("Updating Sagan with \n\n{}", update);
		this.saganClient.updateRelease(version.projectName, Collections.singletonList(update));
		if (version.isRelease() || version.isServiceRelease()) {
			log.info("Version is GA [{}]. Will remove old snapshot and add a new one", version);
			String snapshot = toSnapshot(version.version);
			String bumpedSnapshot = toSnapshot(version.bumpedVersion());
			log.info("Removing [{}/{}] from Sagan", version.projectName, snapshot);
			this.saganClient.deleteRelease(version.projectName, snapshot);
			ReleaseUpdate snapshotUpdate =
					releaseUpdate(branch, originalVersion, new ProjectVersion(version.projectName, bumpedSnapshot));
			log.info("Updating Sagan with \n\n[{}]", snapshotUpdate);
			this.saganClient.updateRelease(version.projectName, Collections.singletonList(snapshotUpdate));
		}
	}

	private ReleaseUpdate releaseUpdate(String branch, ProjectVersion originalVersion,
			ProjectVersion version) {
		ReleaseUpdate update = new ReleaseUpdate();
		update.groupId = originalVersion.groupId();
		update.artifactId = version.projectName;
		update.version = version.version;
		update.releaseStatus = version(version);
		update.apiDocUrl = referenceUrl(branch, version);
		update.refDocUrl = referenceUrl(branch, version);
		return update;
	}

	private String toSnapshot(String version) {
		if (version.contains("RELEASE")) {
			return version.replace("RELEASE", "BUILD-SNAPSHOT");
		} else if (version.matches(".*SR[0-9]+")) {
			return version.substring(0, version.lastIndexOf(".")) + ".BUILD-SNAPSHOT";
		}
		return version;
	}

	private String version(ProjectVersion version) {
		if (version.isSnapshot()) {
			return "SNAPSHOT";
		} else if (version.isMilestone() || version.isRc()) {
			return "PRERELEASE";
		} else if (version.isRelease() || version.isServiceRelease()) {
			return "GENERAL_AVAILABILITY";
		}
		return "";
	}

	private String referenceUrl(String branch, ProjectVersion version) {
		if (!version.isSnapshot()) {
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
