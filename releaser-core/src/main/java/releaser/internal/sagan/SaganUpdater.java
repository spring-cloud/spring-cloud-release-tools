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
		ReleaseInput update = releaseUpdate(currentVersion);
		Exception updateReleaseException = updateSaganForNonSnapshot(currentVersion);
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
				updateReleaseException = new IllegalStateException(ex.toString());
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
		ProjectDetails projectDetails = new ProjectDetails();
		if (indexDoc.exists()) {
			log.debug("Index adoc file exists");
			String fileText = fileToText(indexDoc);
			if (StringUtils.hasText(fileText)) {
				log.info("Index adoc content differs from the previously stored, will update it");
				projectDetails.setBody(fileText);
				shouldUpdate = true;
			}
		}
		if (bootDoc.exists()) {
			log.debug("Boot adoc file exists");
			String fileText = fileToText(bootDoc);
			if (StringUtils.hasText(fileText)) {
				log.info("Boot adoc content differs from the previously stored, will update it");
				projectDetails.setBootConfig(fileText);
				shouldUpdate = true;
			}
		}
		if (shouldUpdate) {
			this.saganClient.patchProjectDetails(project.getSlug(), projectDetails);
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

	private Exception updateSaganForNonSnapshot(ProjectVersion version) {
		Exception updateReleaseException = null;
		if (!version.isSnapshot()) {
			log.info("Version is non snapshot [{}]. Will remove all older versions and mark this as current", version);
			Project project = this.saganClient.getProject(version.projectName);
			if (project != null) {
				removeAllSameMinorVersions(version, project);
			}
			String snapshot = version.toSnapshotVersion();
			removeVersionFromSagan(version, snapshot);
			if (version.isRelease() || version.isServiceRelease()) {
				try {
					String bumpedSnapshot = bumpedSnapshot(version);
					ReleaseInput snapshotUpdate = releaseUpdate(
							new ProjectVersion(version.projectName, bumpedSnapshot));
					log.info("Updating Sagan with bumped snapshot \n\n[{}]", snapshotUpdate);
					this.saganClient.addRelease(version.projectName, snapshotUpdate);
				}
				catch (Exception e) {
					log.warn("Failed to update [" + version.projectName + "/" + snapshot + "] from Sagan", e);
					updateReleaseException = new RuntimeException(e.toString());
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

	private ReleaseInput releaseUpdate(ProjectVersion version) {
		ReleaseInput update = new ReleaseInput();
		update.setVersion(version.version);
		update.setReferenceDocUrl(newReferenceUrl(version));
		update.setApiDocUrl(null);
		return update;
	}

	private String bumpedSnapshot(ProjectVersion projectVersion) {
		return projectVersion.bumpedSnapshotVersion();
	}

	private String newReferenceUrl(ProjectVersion version) {
		String antoraVersions = version.majorAndMinor();
		if (version.isSnapshot()) {
			antoraVersions += "-SNAPSHOT";
		}
		// TODO uncomment the below line and remove the logic above once Contentful
		// "Antora Version" checkbox
		// bug is fixed. Currently the checkbox is not being saved and is always unchecked
		// when using the REST API
		// and this results in the version now being computed propertly by Contentful.
		// Therefore we compute the version
		// in the documentation URL ourselves instead of using the {version} placeholder
		// and letting Contentful compute the
		// version. NOTE: Tests in {@link SaganUpdaterTests} will need to be updated as
		// well once the fix is in place.
		// return "https://docs.spring.io/" + version.projectName +
		// "/reference/{version}/";
		return "https://docs.spring.io/" + version.projectName + "/reference/" + antoraVersions + "/";
	}

}
