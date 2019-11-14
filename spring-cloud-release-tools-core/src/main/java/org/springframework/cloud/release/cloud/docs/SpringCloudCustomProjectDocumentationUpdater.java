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
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class SpringCloudCustomProjectDocumentationUpdater
		implements CustomProjectDocumentationUpdater {

	private static final String HTTP_SC_STATIC_URL = "http://cloud.spring.io/spring-cloud-static/";

	private static final String HTTPS_SC_STATIC_URL = "https://cloud.spring.io/spring-cloud-static/";

	public static final String NO_PROJECT_NAME = "";

	private final File indexHtmlTemplate;

	private static final Logger log = LoggerFactory
			.getLogger(SpringCloudCustomProjectDocumentationUpdater.class);

	private final ProjectGitHandler gitHandler;

	SpringCloudCustomProjectDocumentationUpdater(ProjectGitHandler gitHandler) {
		this.gitHandler = gitHandler;
		try {
			this.indexHtmlTemplate = new File(CustomProjectDocumentationUpdater.class
					.getResource("/cloud/index.html").toURI());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
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
		String pathToIndexHtml = "current/index.html";
		File indexHtml = indexHtmlWithRedirection(clonedDocumentationProject, "",
				bomBranch, pathToIndexHtml);
		File docsRepo = updateTheDocsRepo(NO_PROJECT_NAME, bomBranch,
				clonedDocumentationProject, indexHtml);
		log.info(
				"Updating all current links to documentation for release train projects");
		projects.forEach(projectVersion -> {
			File index = indexHtmlWithRedirection(clonedDocumentationProject,
					projectVersion.projectName, projectVersion.version, pathToIndexHtml);
			try {
				updateTheDocsRepo(projectVersion.projectName, projectVersion.version,
						clonedDocumentationProject, index);
				log.info("Processed [{}] for project with name [{}]", index,
						projectVersion.projectName);
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

	private File indexHtmlWithRedirection(File clonedDocumentationProject,
			String projectName, String version, String pathToIndexHtml) {
		File indexHtml = indexHtml(clonedDocumentationProject,
				combinedPathToIndexHtml(projectName, version, pathToIndexHtml));
		if (!indexHtml.exists()) {
			return generateIndexHtml(projectName, version, pathToIndexHtml, indexHtml);
		}
		return indexHtml;
	}

	private File generateIndexHtml(String projectName, String projectVersion,
			String pathToIndexHtml, File indexHtml) {
		try {
			log.info("No file [{}] found, will create one", indexHtml);
			indexHtml.getParentFile().mkdirs();
			indexHtml.createNewFile();
			String newIndex = new String(Files.readAllBytes(indexHtmlTemplate.toPath()))
					.replaceAll("\\{\\{URL}}",
							"https://cloud.spring.io/spring-cloud-static/"
									+ concreteVersionIndexHtml(projectName,
											branchToReleaseVersion(projectVersion),
											"index.html"));
			Files.write(indexHtml.toPath(), newIndex.getBytes());
			return indexHtml;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String combinedPathToIndexHtml(String projectName, String projectVersion,
			String pathToIndexHtml) {
		boolean releaseTrain = new ProjectVersion(projectName, projectVersion)
				.isReleaseTrain();
		// release train -> static/current/index.html
		// project -> static/spring-cloud-sleuth/current/index.html
		String prefix = releaseTrain ? ""
				: (StringUtils.hasText(projectName) ? projectName : "") + "/";
		return prefix + pathToIndexHtml;
	}

	private String concreteVersionIndexHtml(String projectName, String projectVersion,
			String pathToIndexHtml) {
		boolean releaseTrain = new ProjectVersion(projectName, projectVersion)
				.isReleaseTrain();
		// release train -> static/current/index.html
		// project -> static/spring-cloud-sleuth/current/index.html
		String prefix = releaseTrain ? projectVersion
				: (projectName + "/" + projectVersion);
		return prefix + "/" + pathToIndexHtml;
	}

	private File pushChanges(File docsRepo) {
		this.gitHandler.pushCurrentBranch(docsRepo);
		log.info("Committed and pushed changes to the documentation project");
		return docsRepo;
	}

	File indexHtml(File clonedDocumentationProject, String pathToIndexHtml) {
		return new File(clonedDocumentationProject, pathToIndexHtml);
	}

	private File updateTheDocsRepo(String projectName, String version,
			File documentationProject, File indexHtml) {
		try {
			String indexHtmlText = readIndexHtmlContents(indexHtml);
			String httpSubstring = HTTP_SC_STATIC_URL
					+ (StringUtils.hasText(projectName) ? (projectName + "/") : "");
			String httpsSubstring = HTTPS_SC_STATIC_URL
					+ (StringUtils.hasText(projectName) ? (projectName + "/") : "");
			int httpIndex = indexHtmlText.indexOf(httpSubstring);
			int httpsIndex = indexHtmlText.indexOf(httpsSubstring);
			if (httpIndex == -1 && httpsIndex == -1) {
				throw new IllegalStateException(
						"The URL to the documentation repo not found in the index.html file");
			}
			int beginIndex = beginIndex(httpIndex, httpSubstring, httpsIndex,
					httpsSubstring);
			String storedVersionLine = indexHtmlText.substring(beginIndex);
			String storedVersion = storedVersionLine.substring(0,
					storedVersionLine.indexOf("/"));
			String currentVersion = branchToReleaseVersion(version);
			boolean newerVersion = isMoreMature(storedVersion, currentVersion);
			if (!newerVersion) {
				log.info("Current version [{}] is not newer than the stored one [{}]",
						currentVersion, storedVersion);
				return documentationProject;
			}
			return commitChanges(currentVersion, documentationProject, indexHtml,
					indexHtmlText, storedVersion);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	boolean isMoreMature(String storedVersion, String currentVersion) {
		return new ProjectVersion("project", currentVersion)
				.isMoreMature(new ProjectVersion("project", storedVersion));
	}

	private int beginIndex(int httpIndex, String httpSubstring, int httpsIndex,
			String httpsSubstring) {
		if (httpIndex != -1) {
			return httpIndex + httpSubstring.length();
		}
		return httpsIndex + httpsSubstring.length();
	}

	private String branchToReleaseVersion(String springCloudReleaseBranch) {
		if (springCloudReleaseBranch.startsWith("v")) {
			return springCloudReleaseBranch.substring(1);
		}
		return springCloudReleaseBranch;
	}

	private File commitChanges(String currentVersion, File documentationProject,
			File indexHtml, String indexHtmlText, String storedReleaseTrain)
			throws IOException {
		String replacedIndexHtml = indexHtmlText.replace(storedReleaseTrain,
				currentVersion);
		Files.write(indexHtml.toPath(), replacedIndexHtml.getBytes());
		log.info("Stored the version URL [{}] in [{}]", currentVersion,
				indexHtml.getAbsolutePath());
		this.gitHandler.commit(documentationProject,
				"Updating the link to the current version to [" + currentVersion + "]");
		return documentationProject;
	}

	String readIndexHtmlContents(File indexHtml) throws IOException {
		return new String(Files.readAllBytes(indexHtml.toPath()));
	}

}
