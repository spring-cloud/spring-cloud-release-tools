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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudCustomProjectDocumentationUpdater
		implements CustomProjectDocumentationUpdater {

	private static final String HTTP_SC_STATIC_URL = "http://cloud.spring.io/spring-cloud-static/";

	private static final String HTTPS_SC_STATIC_URL = "https://cloud.spring.io/spring-cloud-static/";

	private static final Logger log = LoggerFactory
			.getLogger(SpringCloudCustomProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;

	SpringCloudCustomProjectDocumentationUpdater(ProjectGitHandler gitHandler) {
		this.gitHandler = gitHandler;
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
			ProjectVersion currentProject, String bomBranch) {
		log.debug("Cloning the doc project to [{}]", clonedDocumentationProject);
		String pathToIndexHtml = "current/index.html";
		File indexHtml = indexHtml(clonedDocumentationProject, pathToIndexHtml);
		if (!indexHtml.exists()) {
			throw new IllegalStateException(
					"index.html is not present at [" + pathToIndexHtml + "]");
		}
		return updateTheDocsRepo(bomBranch, clonedDocumentationProject, indexHtml);
	}

	File indexHtml(File clonedDocumentationProject, String pathToIndexHtml) {
		return new File(clonedDocumentationProject, pathToIndexHtml);
	}

	private File updateTheDocsRepo(String springCloudReleaseBranch,
			File documentationProject, File indexHtml) {
		try {
			String indexHtmlText = readIndexHtmlContents(indexHtml);
			int httpIndex = indexHtmlText.indexOf(HTTP_SC_STATIC_URL);
			int httpsIndex = indexHtmlText.indexOf(HTTPS_SC_STATIC_URL);
			if (httpIndex == -1 && httpsIndex == -1) {
				throw new IllegalStateException(
						"The URL to the documentation repo not found in the index.html file");
			}
			int beginIndex = beginIndex(httpIndex, httpsIndex);
			String storedReleaseTrainLine = indexHtmlText.substring(beginIndex);
			String storedReleaseTrain = storedReleaseTrainLine.substring(0,
					storedReleaseTrainLine.indexOf("/"));
			String firstLetterOfReleaseTrain = String
					.valueOf(storedReleaseTrain.charAt(0));
			String currentReleaseTrainVersion = branchToReleaseVersion(
					springCloudReleaseBranch);
			String firstLetterOfCurrentReleaseTrain = String
					.valueOf(currentReleaseTrainVersion.charAt(0));
			boolean newerOrEqualReleaseTrain = isNewerOrEqualReleaseTrain(
					storedReleaseTrain, firstLetterOfReleaseTrain,
					currentReleaseTrainVersion, firstLetterOfCurrentReleaseTrain);
			if (!newerOrEqualReleaseTrain) {
				log.info(
						"Current release train [{}] is not newer than the stored one [{}]",
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

	boolean isNewerOrEqualReleaseTrain(String storedReleaseTrain,
			String firstLetterOfReleaseTrain, String currentReleaseTrainVersion,
			String firstLetterOfCurrentReleaseTrain) {
		return (!storedReleaseTrain.equals(currentReleaseTrainVersion))
				&& firstLetterOfCurrentReleaseTrain
						.compareToIgnoreCase(firstLetterOfReleaseTrain) >= 0;
	}

	private int beginIndex(int httpIndex, int httpsIndex) {
		if (httpIndex != -1) {
			return httpIndex + HTTP_SC_STATIC_URL.length();
		}
		return httpsIndex + HTTPS_SC_STATIC_URL.length();
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
		String replacedIndexHtml = indexHtmlText.replace(storedReleaseTrain,
				currentReleaseTrainVersion);
		Files.write(indexHtml.toPath(), replacedIndexHtml.getBytes());
		log.info("Stored the release train [{}] in [{}]", currentReleaseTrainVersion,
				indexHtml.getAbsolutePath());
		this.gitHandler.commit(documentationProject,
				"Updating the link to the current version to ["
						+ currentReleaseTrainVersion + "]");
		this.gitHandler.pushCurrentBranch(documentationProject);
		log.info("Committed and pushed changes to the documentation project");
		return documentationProject;
	}

	String readIndexHtmlContents(File indexHtml) throws IOException {
		return new String(Files.readAllBytes(indexHtml.toPath()));
	}

}
