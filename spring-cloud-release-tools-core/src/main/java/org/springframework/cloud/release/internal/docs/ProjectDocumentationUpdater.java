package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
class ProjectDocumentationUpdater implements ReleaserPropertiesAware {

	private static final String SC_STATIC_URL = "http://cloud.spring.io/spring-cloud-static/";
	private static final Logger log = LoggerFactory.getLogger(ProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;
	private ReleaserProperties properties;

	ProjectDocumentationUpdater(ReleaserProperties properties,
			ProjectGitHandler gitHandler) {
		this.gitHandler = gitHandler;
		this.properties = properties;
	}

	/**
	 * Updates the documentation repository if current release train version is greater or equal
	 * than the one stored in the repo.
	 *
	 * @param currentProject
	 * @param springCloudReleaseBranch
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is used
	 */
	File updateDocsRepo(ProjectVersion currentProject, String springCloudReleaseBranch) {
		if (!this.properties.getGit().isUpdateDocumentationRepo()) {
			log.info("Will not update documentation repository, since the switch to do so "
					+ "is off. Set [releaser.git.update-documentation-repo] to [true] to change that");
			return null;
		}
		if (!currentProject.isReleaseOrServiceRelease()) {
			log.info("Will not update documentation repository for non release or service release [{}]", currentProject.version);
			return null;
		}
		File documentationProject = this.gitHandler.cloneDocumentationProject();
		log.debug("Cloning the doc project to [{}]", documentationProject);
		String pathToIndexHtml = "current/index.html";
		File indexHtml = new File(documentationProject, pathToIndexHtml);
		if (!indexHtml.exists()) {
			throw new IllegalStateException("index.html is not present at [" + pathToIndexHtml + "]");
		}
		return updateTheDocsRepo(springCloudReleaseBranch, documentationProject, indexHtml);
	}

	private File updateTheDocsRepo(String springCloudReleaseBranch, File documentationProject, File indexHtml) {
		try {
			String indexHtmlText = readIndexHtmlContents(indexHtml);
			int index = indexHtmlText.indexOf(SC_STATIC_URL);
			if (index == -1) {
				throw new IllegalStateException("The URL to the documentation repo not found in the index.html file");
			}
			int beginIndex = index + SC_STATIC_URL.length();
			String storedReleaseTrainLine = indexHtmlText.substring(beginIndex);
			String storedReleaseTrain = storedReleaseTrainLine.substring(0, storedReleaseTrainLine.indexOf("/"));
			String firstLetterOfReleaseTrain = String.valueOf(storedReleaseTrain.charAt(0));
			String currentReleaseTrainVersion = branchToReleaseVersion(springCloudReleaseBranch);
			String firstLetterOfCurrentReleaseTrain = String.valueOf(currentReleaseTrainVersion.charAt(0));
			boolean newerOrEqualReleaseTrain = (!storedReleaseTrain.equals(currentReleaseTrainVersion)) && firstLetterOfCurrentReleaseTrain
					.compareToIgnoreCase(firstLetterOfReleaseTrain) >= 0;
			if (!newerOrEqualReleaseTrain) {
				log.info("Current release train [{}] is not newer than the stored one [{}]",
						currentReleaseTrainVersion, storedReleaseTrain);
				return documentationProject;
			}
			return pushCommitedChanges(currentReleaseTrainVersion, documentationProject,
					indexHtml, indexHtmlText, storedReleaseTrain);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String branchToReleaseVersion(String springCloudReleaseBranch) {
		if (springCloudReleaseBranch.startsWith("v")) {
			return springCloudReleaseBranch.substring(1);
		}
		return springCloudReleaseBranch;
	}

	private File pushCommitedChanges(String currentReleaseTrainVersion,
			File documentationProject, File indexHtml, String indexHtmlText,
			String storedReleaseTrain) throws IOException {
		String replacedIndexHtml = indexHtmlText.replace(storedReleaseTrain, currentReleaseTrainVersion);
		Files.write(indexHtml.toPath(), replacedIndexHtml.getBytes());
		log.info("Stored the release train [{}] in [{}]",
				currentReleaseTrainVersion, indexHtml.getAbsolutePath());
		this.gitHandler.commit(documentationProject, "Updating the link to the current version to [" + currentReleaseTrainVersion + "]");
		this.gitHandler.pushCurrentBranch(documentationProject);
		log.info("Committed and pushed changes to the documentation project");
		return documentationProject;
	}

	String readIndexHtmlContents(File indexHtml) throws IOException {
		return new String(Files.readAllBytes(indexHtml.toPath()));
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}
}
