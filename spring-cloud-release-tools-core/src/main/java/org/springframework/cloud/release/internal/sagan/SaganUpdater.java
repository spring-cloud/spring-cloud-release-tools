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

package org.springframework.cloud.release.internal.sagan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.util.StringUtils;

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

	public void updateSagan(File projectFile, String branch,
			ProjectVersion originalVersion, ProjectVersion currentVersion) {
		if (!this.releaserProperties.getSagan().isUpdateSagan()) {
			log.info("Will not update sagan, since the switch to do so "
					+ "is off. Set [releaser.sagan.update-sagan] to [true] to change that");
			return;
		}
		ReleaseUpdate update = releaseUpdate(branch, originalVersion, currentVersion);
		updateSaganForNonSnapshot(branch, originalVersion, currentVersion);
		log.info("Updating Sagan releases with \n\n{}", update);
		Project project = this.saganClient.updateRelease(currentVersion.projectName,
				Collections.singletonList(update));
		Optional<ProjectVersion> projectVersion = latestVersion(currentVersion, project);
		log.info("Found the following latest project version [{}]", projectVersion);
		boolean present = projectVersion.isPresent();
		if (present && currentVersionNewerOrEqual(currentVersion, projectVersion)) {
			updateDocumentationIfNecessary(projectFile, project);
		}
		else {
			log.info(present
					? "Latest version [" + projectVersion.get() + "] present and "
							+ "the current version [" + currentVersion
							+ "] is older than that one. " + "Will do nothing."
					: "No latest version found. Will do nothing.");
		}
	}

	private void updateDocumentationIfNecessary(File projectFile, Project project) {
		boolean shouldUpdate = false;
		File docsModule = docsModule(projectFile);
		File indexDoc = new File(docsModule,
				this.releaserProperties.getSagan().getIndexSectionFileName());
		File bootDoc = new File(docsModule,
				this.releaserProperties.getSagan().getBootSectionFileName());
		if (indexDoc.exists()) {
			log.debug("Index adoc file exists");
			String fileText = fileToText(indexDoc);
			if (StringUtils.hasText(fileText) && !fileText.equals(project.rawOverview)) {
				log.info(
						"Index adoc content differs from the previously stored, will update it");
				project.rawOverview = fileText;
				shouldUpdate = true;
			}
		}
		if (bootDoc.exists()) {
			log.debug("Boot adoc file exists");
			String fileText = fileToText(bootDoc);
			if (StringUtils.hasText(fileText)
					&& !fileText.equals(project.rawBootConfig)) {
				log.info(
						"Boot adoc content differs from the previously stored, will update it");
				project.rawBootConfig = fileText;
				shouldUpdate = true;
			}
		}
		if (shouldUpdate) {
			this.saganClient.patchProject(project);
			log.info("Updating Sagan project with adoc data.");
		}
		else {
			log.info("Nothing changed in project's meta-data. Won't change anything.");
		}
	}

	File docsModule(File projectFile) {
		return new File(projectFile,
				this.releaserProperties.getSagan().getDocsAdocsFile());
	}

	private String fileToText(File file) {
		try {
			return new String(Files.readAllBytes(file.toPath()));
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Optional<ProjectVersion> latestVersion(ProjectVersion currentVersion,
			Project project) {
		if (project == null) {
			return Optional.empty();
		}
		return project.projectReleases.stream().filter(release -> release.current)
				.map(release -> new ProjectVersion(currentVersion.projectName,
						release.version))
				.max(Comparator.comparing(o -> o.version));
	}

	private boolean currentVersionNewerOrEqual(ProjectVersion currentVersion,
			Optional<ProjectVersion> projectVersion) {
		return currentVersion.compareTo(projectVersion.get()) >= 0;
	}

	private void updateSaganForNonSnapshot(String branch, ProjectVersion originalVersion,
			ProjectVersion version) {
		if (!version.isSnapshot()) {
			log.info(
					"Version is non snapshot [{}]. Will remove all older versions and mark this as current",
					version);
			Project project = this.saganClient.getProject(version.projectName);
			if (project != null) {
				removeAllSameMinorVersions(version, project);
			}
			String snapshot = toSnapshot(version.version);
			removeVersionFromSagan(version, snapshot);
			if (version.isRelease() || version.isServiceRelease()) {
				String bumpedSnapshot = toSnapshot(version.bumpedVersion());
				ReleaseUpdate snapshotUpdate = releaseUpdate(branch, originalVersion,
						new ProjectVersion(version.projectName, bumpedSnapshot));
				log.info("Updating Sagan with bumped snapshot \n\n[{}]", snapshotUpdate);
				this.saganClient.updateRelease(version.projectName,
						Collections.singletonList(snapshotUpdate));
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
		}
		catch (Exception e) {
			log.error("Failed to remove [" + version.projectName + "/" + snapshot
					+ "] from Sagan", e);
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
		}
		else if (version.matches(".*SR[0-9]+")) {
			return version.substring(0, version.lastIndexOf(".")) + ".BUILD-SNAPSHOT";
		}
		return version;
	}

	private String version(ProjectVersion version) {
		if (version.isSnapshot()) {
			return "SNAPSHOT";
		}
		else if (version.isMilestone() || version.isRc()) {
			return "PRERELEASE";
		}
		else if (version.isRelease() || version.isServiceRelease()) {
			return "GENERAL_AVAILABILITY";
		}
		return "";
	}

	private String referenceUrl(String branch, ProjectVersion version) {
		if (!version.isSnapshot()) {
			// static/sleuth/{version}/
			return "https://cloud.spring.io/spring-cloud-static/" + version.projectName
					+ "/{version}/";
		}
		if (branch.toLowerCase().contains("master")) {
			// sleuth/
			return "https://cloud.spring.io/" + version.projectName + "/"
					+ version.projectName + ".html";
		}
		// sleuth/1.1.x/
		return "https://cloud.spring.io/" + version.projectName + "/" + branch + "/";
	}

}
