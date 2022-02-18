/*
 * Copyright 2013-2020 the original author or authors.
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

package releaser.internal.sagan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.tech.ExecutionResult;

import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 * @author Oleg Zhurakousky
 */
public class SaganUpdater {

	private static final Logger log = LoggerFactory.getLogger(SaganUpdater.class);

	private final SaganClient saganClient;

	private final ReleaserProperties releaserProperties;

	public SaganUpdater(SaganClient saganClient, ReleaserProperties releaserProperties) {
		this.saganClient = saganClient;
		this.releaserProperties = releaserProperties;
	}

	public ExecutionResult updateSagan(File projectFile, String branch, ProjectVersion originalVersion,
			ProjectVersion currentVersion, Projects projects) {
		if (!this.releaserProperties.getSagan().isUpdateSagan()) {
			log.info("Will not update sagan, since the switch to do so "
					+ "is off. Set [releaser.sagan.update-sagan] to [true] to change that");
			return ExecutionResult.skipped();
		}
		ReleaseInput update = releaseUpdate(branch, originalVersion, currentVersion, projects);
		Exception updateReleaseException = updateSaganForNonSnapshot(branch, originalVersion, currentVersion, projects);
		if (updateReleaseException == null) {
			log.info("Updating Sagan releases with \n\n{}", update);
			try {
				boolean added = this.saganClient.addRelease(currentVersion.projectName, update);
				if (!added) {
					return ExecutionResult.unstable(
							new Exception("Unable to add release for project " + currentVersion.toPrettyString()));
				}
				Project project = saganClient.getProject(currentVersion.projectName);
				Optional<ProjectVersion> projectVersion = latestVersion(currentVersion, project);
				log.info("Found the following latest project version [{}]", projectVersion);
				boolean present = projectVersion.isPresent();
				if (present && currentVersionNewerOrEqual(currentVersion, projectVersion)) {
					updateDocumentationIfNecessary(projectFile, project);
				}
				else {
					log.info(present
							? "Latest version [" + projectVersion.get() + "] present and " + "the current version ["
									+ currentVersion + "] is older than that one. " + "Will do nothing."
							: "No latest version found. Will do nothing.");
					return ExecutionResult.skipped();
				}
			}
			catch (Exception ex) {
				log.warn("Exception occurred while trying to update sagan release", ex);
				updateReleaseException = ex;
			}
		}
		return updateReleaseException == null ? ExecutionResult.success()
				: ExecutionResult.unstable(new IllegalStateException(updateReleaseException.getMessage()));
	}

	private void updateDocumentationIfNecessary(File projectFile, Project project) {
		boolean shouldUpdate = false;
		File docsModule = docsModule(projectFile);
		File indexDoc = new File(docsModule, this.releaserProperties.getSagan().getIndexSectionFileName());
		File bootDoc = new File(docsModule, this.releaserProperties.getSagan().getBootSectionFileName());
		if (indexDoc.exists()) {
			log.debug("Index adoc file exists");
			String fileText = fileToText(indexDoc);
			// if (StringUtils.hasText(fileText) && !fileText.equals(project.rawOverview))
			// {
			// log.info("Index adoc content differs from the previously stored, will
			// update it");
			// project.rawOverview = fileText;
			// shouldUpdate = true;
			// }
		}
		if (bootDoc.exists()) {
			log.debug("Boot adoc file exists");
			String fileText = fileToText(bootDoc);
			// if (StringUtils.hasText(fileText) &&
			// !fileText.equals(project.rawBootConfig)) {
			// log.info("Boot adoc content differs from the previously stored, will update
			// it");
			// project.rawBootConfig = fileText;
			// shouldUpdate = true;
			// }
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
		return new File(projectFile, this.releaserProperties.getSagan().getDocsAdocsFile());
	}

	private String fileToText(File file) {
		try {
			return new String(Files.readAllBytes(file.toPath()));
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Optional<ProjectVersion> latestVersion(ProjectVersion currentVersion, Project project) {
		if (project == null) {
			return Optional.empty();
		}
		return project.getReleases().stream().filter(Release::isCurrent)
				.map(release -> new ProjectVersion(currentVersion.projectName, release.getVersion()))
				.max(Comparator.comparing(o -> o.version));
	}

	private boolean currentVersionNewerOrEqual(ProjectVersion currentVersion, Optional<ProjectVersion> projectVersion) {
		return currentVersion.compareTo(projectVersion.get()) >= 0;
	}

	private Exception updateSaganForNonSnapshot(String branch, ProjectVersion originalVersion, ProjectVersion version,
			Projects projects) {
		Exception updateReleaseException = null;
		if (!version.isSnapshot()) {
			log.info("Version is non snapshot [{}]. Will remove all older versions and mark this as current", version);
			Project project = this.saganClient.getProject(version.projectName);
			if (project != null) {
				removeAllSameMinorVersions(version, project);
			}
			String snapshot = toSnapshot(version);
			removeVersionFromSagan(version, snapshot);
			if (version.isRelease() || version.isServiceRelease()) {
				try {
					String bumpedSnapshot = bumpedSnapshot(version);
					ReleaseInput snapshotUpdate = releaseUpdate(branch, originalVersion,
							new ProjectVersion(version.projectName, bumpedSnapshot), projects);
					log.info("Updating Sagan with bumped snapshot \n\n[{}]", snapshotUpdate);
					this.saganClient.addRelease(version.projectName, snapshotUpdate);
				}
				catch (Exception e) {
					log.warn("Failed to update [" + version.projectName + "/" + snapshot + "] from Sagan", e);
					updateReleaseException = e;
				}
			}
		}
		return updateReleaseException;
	}

	private void removeAllSameMinorVersions(ProjectVersion version, Project project) {
		project.getReleases().stream().filter(release -> version.isSameMinor(release.getVersion()))
				.collect(Collectors.toList()).forEach(release -> removeVersionFromSagan(version, release.getVersion()));
	}

	private void removeVersionFromSagan(ProjectVersion version, String snapshot) {
		log.info("Removing [{}/{}] from Sagan", version.projectName, snapshot);
		try {
			boolean deleted = this.saganClient.deleteRelease(version.projectName, snapshot);
			if (!deleted) {
				log.warn("Failed to remove [" + version.projectName + "/" + snapshot + "] from Sagan");
			}
		}
		catch (Exception e) {
			log.warn("Failed to remove [" + version.projectName + "/" + snapshot + "] from Sagan", e);
		}
	}

	private ReleaseInput releaseUpdate(String branch, ProjectVersion originalVersion, ProjectVersion version,
			Projects projects) {
		ReleaseInput update = new ReleaseInput();
		update.setVersion(version.version);
		update.setReferenceDocUrl(referenceUrl(branch, version, projects));
		update.setApiDocUrl(null);
		return update;
	}

	private String bumpedSnapshot(ProjectVersion projectVersion) {
		String bumpedVersion = projectVersion.bumpedVersion();
		return toSnapshot(new ProjectVersion("", bumpedVersion));
	}

	private String toSnapshot(ProjectVersion projectVersion) {
		String version = projectVersion.version;
		if (version.contains("RELEASE")) {
			return version.replace("RELEASE", "BUILD-SNAPSHOT");
		}
		else if (version.matches(".*SR[0-9]+")) {
			return version.substring(0, version.lastIndexOf(".")) + ".BUILD-SNAPSHOT";
		}
		return projectVersion.toSnapshotVersion();
	}

	private String releaseTrainVersion(Projects projects) {
		String releaseTrainProjectName = this.releaserProperties.getMetaRelease().getReleaseTrainProjectName();
		return projects.containsProject(releaseTrainProjectName) ? projects.forName(releaseTrainProjectName).version
				: "";
	}

	private String referenceUrl(String branch, ProjectVersion version, Projects projects) {
		String releaseTrainVersion = releaseTrainVersion(projects);
		// up till Greenwich we have a different URL for docs
		// if there's no release train, will assume that 2.2.x is the version that has the
		// new docs
		boolean hasReleaseTrainVersion = StringUtils.hasText(releaseTrainVersion);
		boolean newDocs = hasReleaseTrainVersion ? releaseTrainVersion.toLowerCase().charAt(0) > 'g'
				: version.version.compareTo("2.2") > 0;
		if (newDocs) {
			return newReferenceUrl(branch, version);
		}
		return oldReferenceUrl(branch, version);
	}

	private String newReferenceUrl(String branch, ProjectVersion version) {
		return "https://docs.spring.io/" + version.projectName + "/docs/{version}/reference/html/";
	}

	private String oldReferenceUrl(String branch, ProjectVersion version) {
		if (!version.isSnapshot()) {
			// static/sleuth/{version}/
			return "https://cloud.spring.io/spring-cloud-static/" + version.projectName + "/{version}/";
		}
		if (branch.toLowerCase().contains("main")) {
			// sleuth/
			return "https://cloud.spring.io/" + version.projectName + "/" + version.projectName + ".html";
		}
		// sleuth/1.1.x/
		return "https://cloud.spring.io/" + version.projectName + "/" + branch + "/";
	}

}
