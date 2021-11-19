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

package releaser.cloud.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.docs.CustomProjectDocumentationUpdater;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudCustomProjectDocumentationUpdater implements CustomProjectDocumentationUpdater {

	private static final Logger log = LoggerFactory.getLogger(SpringCloudCustomProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;

	private final ReleaserProperties releaserProperties;

	SpringCloudCustomProjectDocumentationUpdater(ProjectGitHandler gitHandler, ReleaserProperties releaserProperties) {
		this.gitHandler = gitHandler;
		this.releaserProperties = releaserProperties;
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
	public File updateDocsRepoForReleaseTrain(File clonedDocumentationProject, ProjectVersion currentProject,
			Projects projects, String bomBranch) {
		if (!currentProject.projectName.startsWith("spring-cloud")) {
			log.info("Skipping updating docs for project [{}] that does not start with spring-cloud prefix",
					currentProject.projectName);
			return clonedDocumentationProject;
		}
		log.debug("Cloning the doc project to [{}]", clonedDocumentationProject);
		ProjectVersion releaseTrainProject = new ProjectVersion(
				this.releaserProperties.getMetaRelease().getReleaseTrainProjectName(),
				branchToReleaseVersion(bomBranch));
		File currentReleaseFolder = new File(clonedDocumentationProject, currentFolder(releaseTrainProject));
		// remove the old way
		removeAFolderWithRedirection(currentReleaseFolder);
		File docsRepo = updateTheDocsRepo(releaseTrainProject, clonedDocumentationProject, currentReleaseFolder);
		return pushChanges(docsRepo);
	}

	/**
	 * Updates the documentation repository if current release train version is greater or
	 * equal than the one stored in the repo.
	 * @param currentProject project to update the docs repo for
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used
	 */
	@Override
	public File updateDocsRepoForSingleProject(File clonedDocumentationProject, ProjectVersion currentProject,
			Projects projects) {
		if (!projects.containsProject(currentProject.projectName)) {
			log.warn(
					"Can't update the documentation repo for project [{}] cause it's not present on the projects list {}",
					currentProject.projectName, projects);
			return clonedDocumentationProject;
		}
		if (!currentProject.projectName.startsWith("spring-cloud")) {
			log.info("Skipping updating docs for project [{}] that does not start with spring-cloud prefix",
					currentProject.projectName);
			return clonedDocumentationProject;
		}
		log.info("Updating link to documentation for project [{}]", currentProject.projectName);
		ProjectVersion currentProjectVersion = projects.forName(currentProject.projectName);
		File currentProjectReleaseFolder = new File(clonedDocumentationProject, currentFolder(currentProjectVersion));
		removeAFolderWithRedirection(currentProjectReleaseFolder);
		try {
			updateTheDocsRepo(currentProjectVersion, clonedDocumentationProject, currentProjectReleaseFolder);
			log.info("Processed [{}] for project with name [{}]", currentProjectReleaseFolder,
					currentProjectVersion.projectName);
		}
		catch (Exception ex) {
			log.warn("Exception occurred while trying o update the symlink of a project ["
					+ currentProjectVersion.projectName + "]", ex);
		}
		return pushChanges(clonedDocumentationProject);
	}

	private void removeAFolderWithRedirection(File currentReleaseFolder) {
		if (!isSymbolinkLink(currentReleaseFolder)) {
			FileSystemUtils.deleteRecursively(currentReleaseFolder);
		}
	}

	private boolean isSymbolinkLink(File currentFolder) {
		return Files.isSymbolicLink(currentFolder.toPath());
	}

	private String currentFolder(ProjectVersion currentProjectVersion) {
		String projectName = currentProjectVersion.projectName;
		boolean releaseTrain;
		try {
			releaseTrain = currentProjectVersion.isReleaseTrain();
		}
		catch (IllegalStateException ex) {
			log.warn("Exception occurred while trying to resolve if version [" + currentProjectVersion
					+ "] is a release train", ex);
			releaseTrain = false;
		}
		// release train -> static/current
		// project -> static/spring-cloud-sleuth/current
		return releaseTrain ? "current" : (StringUtils.hasText(projectName) ? projectName : "") + "/current";
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

	private File pushChanges(File docsRepo) {
		this.gitHandler.pushCurrentBranch(docsRepo);
		log.info("Committed and pushed changes to the documentation project");
		return docsRepo;
	}

	private File updateTheDocsRepo(ProjectVersion projectVersion, File documentationProject,
			File currentVersionFolder) {
		try {
			String storedVersion = linkToVersion(currentVersionFolder);
			String currentVersion = projectVersion.version;
			boolean newerVersion = StringUtils.isEmpty(storedVersion) || isMoreMature(storedVersion, currentVersion);
			if (!newerVersion) {
				log.info("Current version [{}] is not newer than the stored one [{}]", currentVersion, storedVersion);
				return documentationProject;
			}
			boolean deleted = Files.deleteIfExists(currentVersionFolder.toPath())
					|| FileSystemUtils.deleteRecursively(currentVersionFolder.toPath());
			if (deleted) {
				log.info("Deleted current version folder link at [{}]", currentVersionFolder);
			}
			boolean creatingParentDirs = currentVersionFolder.getParentFile().mkdirs();
			if (!creatingParentDirs) {
				log.warn("Failed to create parent directory of [{}]", currentVersionFolder);
			}
			File newTarget = new File(projectVersion.version);
			Files.createSymbolicLink(currentVersionFolder.toPath(), newTarget.toPath());
			log.info("Updated the link [{}] to point to [{}]", currentVersionFolder.toPath(),
					Files.readSymbolicLink(currentVersionFolder.toPath()));
			return commitChanges(currentVersion, documentationProject);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private boolean isMoreMature(String storedVersion, String currentVersion) {
		return new ProjectVersion("project", currentVersion).isMoreMature(new ProjectVersion("project", storedVersion));
	}

	private String branchToReleaseVersion(String branch) {
		if (branch.startsWith("v")) {
			return branch.substring(1);
		}
		return branch;
	}

	private File commitChanges(String currentVersion, File documentationProject) throws IOException {
		log.info("Updated the symbolic links");
		this.gitHandler.commit(documentationProject,
				"Updating the link to the current version to [" + currentVersion + "]");
		return documentationProject;
	}

}
