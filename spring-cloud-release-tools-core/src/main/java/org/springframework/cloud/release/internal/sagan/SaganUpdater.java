package org.springframework.cloud.release.internal.sagan;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author Marcin Grzejszczak
 */
public class SaganUpdater {

	private static final Logger log = LoggerFactory.getLogger(SaganUpdater.class);

	private final SaganClient saganClient;
	private final ReleaserProperties releaserProperties;

	public SaganUpdater(SaganClient saganClient, ReleaserProperties releaserProperties) {
		this.saganClient = saganClient;
		this.releaserProperties = releaserProperties;
	}

	public void updateSagan(String branch, ProjectVersion originalVersion, ProjectVersion version) {
		if (!releaserProperties.getSagan().isUpdateSagan()) {
			log.info("Will not update sagan, since the switch to do so "
					+ "is off. Set [releaser.sagan.update-sagan] to [true] to change that");
			return;
		}
		ReleaseUpdate update = releaseUpdate(branch, originalVersion, version);
		updateSaganForNonSnapshot(branch, originalVersion, version);
		log.info("Updating Sagan with \n\n{}", update);
		this.saganClient.updateRelease(version.projectName, Collections.singletonList(update));
	}

	private void updateSaganForNonSnapshot(String branch, ProjectVersion originalVersion,
			ProjectVersion version) {
		if (!version.isSnapshot()) {
			log.info("Version is non snapshot [{}]. Will remove all older versions and mark this as current", version);
			Project project = this.saganClient.getProject(version.projectName);
			if (project != null) {
				removeAllSameMinorVersions(version, project);
			}
			String snapshot = toSnapshot(version.version);
			removeVersionFromSagan(version, snapshot);
			if (version.isRelease() || version.isServiceRelease()) {
				String bumpedSnapshot = toSnapshot(version.bumpedVersion());
				ReleaseUpdate snapshotUpdate =
						releaseUpdate(branch, originalVersion, new ProjectVersion(version.projectName, bumpedSnapshot));
				log.info("Updating Sagan with bumped snapshot \n\n[{}]", snapshotUpdate);
				this.saganClient.updateRelease(version.projectName, Collections.singletonList(snapshotUpdate));
			}
		}
	}

	private void removeAllSameMinorVersions(ProjectVersion version, Project project) {
		project.projectReleases.stream()
				.filter(release -> version.isSameMinor(release.version))
				.collect(Collectors.toList())
				.forEach(release -> removeVersionFromSagan(version, release.version));
	}

	private void removeVersionFromSagan(ProjectVersion version, String snapshot) {
		log.info("Removing [{}/{}] from Sagan", version.projectName, snapshot);
		try {
			this.saganClient.deleteRelease(version.projectName, snapshot);
		} catch (Exception e) {
			log.error("Failed to remove [" + version.projectName + "/" + snapshot + "] from Sagan", e);
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
		update.current = true;
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
