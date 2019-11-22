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

package org.springframework.cloud.release.cloud.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudCustomProjectDocumentationUpdater
		implements CustomProjectDocumentationUpdater {

	private static final Logger log = LoggerFactory
			.getLogger(SpringCloudCustomProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;

	private final ReleaserProperties releaserProperties;

	SpringCloudCustomProjectDocumentationUpdater(ProjectGitHandler gitHandler,
			ReleaserProperties releaserProperties) {
		this.gitHandler = gitHandler;
		this.releaserProperties = releaserProperties;
	}

	@Override
	public boolean isApplicable(File clonedDocumentationProject,
			ProjectVersion currentProject, String bomBranch) {
		return clonedDocumentationProject.getName().startsWith("spring-cloud")
				|| currentProject.projectName.startsWith("spring-cloud");
	}

	/**
	 * Updates the documentation repository if current release train version is greater or
	 * equal than the one stored in the repo.
	 * @param currentProject project to update the docs repo for
	 * @param bomBranch the bom project branch
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used
	 */
	@Override
	public File updateDocsRepo(File clonedDocumentationProject,
			ProjectVersion currentProject, Projects projects, String bomBranch) {
		log.debug("Cloning the doc project to [{}]", clonedDocumentationProject);
		ProjectVersion releaseTrainProject = new ProjectVersion(
				this.releaserProperties.getMetaRelease().getReleaseTrainProjectName(),
				branchToReleaseVersion(bomBranch));
		File currentReleaseFolder = new File(clonedDocumentationProject, currentFolder(
				releaseTrainProject.projectName, releaseTrainProject.version));
		// remove the old way
		removeAFolderWithRedirection(currentReleaseFolder);
		File docsRepo = updateTheDocsRepo(releaseTrainProject, clonedDocumentationProject,
				currentReleaseFolder);
		log.info(
				"Updating all current links to documentation for release train projects");
		projects.forEach(projectVersion -> {
			File currentProjectReleaseFolder = new File(clonedDocumentationProject,
					currentFolder(projectVersion.projectName, projectVersion.version));
			removeAFolderWithRedirection(currentProjectReleaseFolder);
			try {
				updateTheDocsRepo(projectVersion, clonedDocumentationProject,
						currentProjectReleaseFolder);
				log.info("Processed [{}] for project with name [{}]",
						currentProjectReleaseFolder, projectVersion.projectName);
			}
			catch (Exception ex) {
				log.warn(
						"Exception occurred while trying o update the index html of a project ["
								+ projectVersion.projectName + "]",
						ex);
			}
		});
		return pushChanges(docsRepo);
	}

	private void removeAFolderWithRedirection(File currentReleaseFolder) {
		if (!isSymbolinkLink(currentReleaseFolder)) {
			FileSystemUtils.deleteRecursively(currentReleaseFolder);
		}
	}

	private boolean isSymbolinkLink(File currentFolder) {
		return Files.isSymbolicLink(currentFolder.toPath());
	}

	private String currentFolder(String projectName, String projectVersion) {
		boolean releaseTrain = new ProjectVersion(projectName, projectVersion)
				.isReleaseTrain();
		// release train -> static/current
		// project -> static/spring-cloud-sleuth/current
		return releaseTrain ? "current"
				: (StringUtils.hasText(projectName) ? projectName : "") + "/current";
	}

	String linkToVersion(File file) {
		if (Files.isSymbolicLink(file.toPath())) {
			try {
				Path path = Files.readSymbolicLink(file.toPath());
				// current -> Hoxton.SR2
				// spring-cloud-sleuth/current -> spring-cloud-sleuth/1.2.3.RELEASE
				return folderName(path.toString());
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return "";
	}

	private String folderName(String path) {
		int last = path.lastIndexOf(File.separator);
		return last > 0 ? path.substring(last + 1) : path;
	}

	private String concreteVersionFolder(ProjectVersion projectVersion) {
		String projectName = projectVersion.projectName;
		boolean releaseTrain = projectVersion.isReleaseTrain();
		// release train -> static/Hoxton.SR2/
		// project -> static/spring-cloud-sleuth/1.2.3.RELEASE/
		String prefix = releaseTrain ? projectVersion.version
				: (projectName + "/" + projectVersion.version);
		return prefix + "/";
	}

	private File pushChanges(File docsRepo) {
		this.gitHandler.pushCurrentBranch(docsRepo);
		log.info("Committed and pushed changes to the documentation project");
		return docsRepo;
	}

	private File updateTheDocsRepo(ProjectVersion projectVersion,
			File documentationProject, File currentVersionFolder) {
		try {
			String storedVersion = linkToVersion(currentVersionFolder);
			String currentVersion = projectVersion.version;
			boolean newerVersion = StringUtils.isEmpty(storedVersion)
					|| isMoreMature(storedVersion, currentVersion);
			if (!newerVersion) {
				log.info("Current version [{}] is not newer than the stored one [{}]",
						currentVersion, storedVersion);
				return documentationProject;
			}
			boolean deleted = Files.deleteIfExists(currentVersionFolder.toPath())
					|| FileSystemUtils.deleteRecursively(currentVersionFolder.toPath());
			if (deleted) {
				log.info("Deleted current version folder link at [{}]",
						currentVersionFolder);
			}
			currentVersionFolder.getParentFile().mkdirs();
			File newTarget = new File(documentationProject,
					concreteVersionFolder(projectVersion));
			Files.createSymbolicLink(currentVersionFolder.toPath(), newTarget.toPath());
			log.info("Updated the link [{}] to point to [{}]",
					currentVersionFolder.toPath(),
					Files.readSymbolicLink(currentVersionFolder.toPath()));
			return commitChanges(currentVersion, documentationProject);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private boolean isMoreMature(String storedVersion, String currentVersion) {
		return new ProjectVersion("project", currentVersion)
				.isMoreMature(new ProjectVersion("project", storedVersion));
	}

	private String branchToReleaseVersion(String branch) {
		if (branch.startsWith("v")) {
			return branch.substring(1);
		}
		return branch;
	}

	private File commitChanges(String currentVersion, File documentationProject)
			throws IOException {
		log.info("Updated the symbolic links");
		this.gitHandler.commit(documentationProject,
				"Updating the link to the current version to [" + currentVersion + "]");
		return documentationProject;
	}

}
