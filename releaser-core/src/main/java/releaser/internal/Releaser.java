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

package releaser.internal;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.buildsystem.GradleUpdater;
import releaser.internal.buildsystem.ProjectPomUpdater;
import releaser.internal.docs.DocumentationUpdater;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.postrelease.PostReleaseActions;
import releaser.internal.project.ProcessedProject;
import releaser.internal.project.ProjectCommandExecutor;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.sagan.SaganUpdater;
import releaser.internal.tech.BuildUnstableException;
import releaser.internal.tech.ExecutionResult;
import releaser.internal.template.TemplateGenerator;

import org.springframework.util.Assert;

/**
 * @author Marcin Grzejszczak
 */
public class Releaser {

	private static final Logger log = LoggerFactory.getLogger(Releaser.class);

	private static boolean ASSERT_SNAPSHOTS = true;

	private static boolean SKIP_SNAPSHOT_ASSERTION = false;

	private final ProjectPomUpdater projectPomUpdater;

	private final ProjectCommandExecutor projectCommandExecutor;

	private final ProjectGitHandler projectGitHandler;

	private final ProjectGitHubHandler projectGitHubHandler;

	private final TemplateGenerator templateGenerator;

	private final GradleUpdater gradleUpdater;

	private final SaganUpdater saganUpdater;

	private final DocumentationUpdater documentationUpdater;

	private final PostReleaseActions postReleaseActions;

	private ReleaserProperties releaserProperties;

	public Releaser(ReleaserProperties releaserProperties,
			ProjectPomUpdater projectPomUpdater,
			ProjectCommandExecutor projectCommandExecutor,
			ProjectGitHandler projectGitHandler,
			ProjectGitHubHandler projectGitHubHandler,
			TemplateGenerator templateGenerator, GradleUpdater gradleUpdater,
			SaganUpdater saganUpdater, DocumentationUpdater documentationUpdater,
			PostReleaseActions postReleaseActions) {
		this.releaserProperties = releaserProperties;
		this.projectPomUpdater = projectPomUpdater;
		this.projectCommandExecutor = projectCommandExecutor;
		this.projectGitHandler = projectGitHandler;
		this.projectGitHubHandler = projectGitHubHandler;
		this.templateGenerator = templateGenerator;
		this.gradleUpdater = gradleUpdater;
		this.saganUpdater = saganUpdater;
		this.documentationUpdater = documentationUpdater;
		this.postReleaseActions = postReleaseActions;
	}

	public File clonedProjectFromOrg(String projectName) {
		return this.projectGitHandler.cloneProjectFromOrg(projectName);
	}

	public Projects retrieveVersionsFromBom() {
		return this.projectPomUpdater.retrieveVersionsFromReleaseTrainBom();
	}

	public Projects fixedVersions() {
		return this.projectPomUpdater.fixedVersions();
	}

	public ExecutionResult updateProjectFromBom(File project, Projects versions,
			ProjectVersion versionFromBom) {
		return updateProjectFromBom(project, versions, versionFromBom, ASSERT_SNAPSHOTS);
	}

	private ExecutionResult updateProjectFromBom(File project, Projects versions,
			ProjectVersion versionFromBom, boolean assertSnapshots) {
		log.info("Will update the project with versions [{}]", versions);
		this.projectPomUpdater.updateProjectFromReleaseTrain(project, versions,
				versionFromBom, assertSnapshots);
		this.gradleUpdater.updateProjectFromReleaseTrain(project, versions,
				versionFromBom, assertSnapshots);
		ProjectVersion changedVersion = new ProjectVersion(project);
		log.info("\n\nProject was successfully updated to [{}]", changedVersion.version);
		return ExecutionResult.success();
	}

	public ExecutionResult buildProject(ReleaserProperties properties,
			ProjectVersion originalVersion, ProjectVersion versionFromBom) {
		this.projectCommandExecutor.build(properties, originalVersion, versionFromBom);
		log.info("\nProject was successfully built");
		return ExecutionResult.success();
	}

	public ExecutionResult commitAndPushTags(File project,
			ProjectVersion changedVersion) {
		this.projectGitHandler.commitAndTagIfApplicable(project, changedVersion);
		log.info("\nCommit was made and tag was pushed successfully");
		return ExecutionResult.success();
	}

	public ExecutionResult deploy(ReleaserProperties properties,
			ProjectVersion originalVersion, ProjectVersion versionFromBom) {
		this.projectCommandExecutor.deploy(properties, originalVersion, versionFromBom);
		log.info("\nThe artifact was deployed successfully");
		return ExecutionResult.success();
	}

	public ExecutionResult publishDocs(ReleaserProperties properties,
			ProjectVersion originalVersion, ProjectVersion changedVersion) {
		this.projectCommandExecutor.publishDocs(properties, originalVersion,
				changedVersion);
		log.info("\nThe docs were published successfully");
		return ExecutionResult.success();
	}

	public ExecutionResult rollbackReleaseVersion(File project, Projects projects,
			ProjectVersion scReleaseVersion) {
		if (scReleaseVersion.isSnapshot()) {
			log.info("\nWon't rollback a snapshot version");
			return ExecutionResult.skipped();
		}
		this.projectGitHandler.revertChangesIfApplicable(project, scReleaseVersion);
		ProjectVersion originalVersion = originalVersion(project);
		log.info("Original project version is [{}]", originalVersion);
		if ((scReleaseVersion.isRelease() || scReleaseVersion.isServiceRelease())
				&& originalVersion.isSnapshot()) {
			updateBumpedVersions(project, projects, originalVersion);
		}
		else {
			log.info(
					"\nSuccessfully reverted the commit and came back to snapshot versions");
		}
		return ExecutionResult.success();
	}

	private ExecutionResult updateBumpedVersions(File project, Projects projects,
			ProjectVersion originalVersion) {
		Projects newProjects = Projects.forRollback(releaserProperties, projects);
		ProjectVersion bumpedProject = bumpProject(originalVersion, newProjects);
		log.info("Will bump versions \n{}", newProjects);
		updateProjectFromBom(project, newProjects, originalVersion,
				SKIP_SNAPSHOT_ASSERTION);
		this.projectGitHandler.commitAfterBumpingVersions(project, bumpedProject);
		log.info("\nSuccessfully reverted the commit and bumped snapshot versions");
		return ExecutionResult.success();
	}

	private ProjectVersion bumpProject(ProjectVersion originalVersion,
			Projects newProjects) {
		return newProjects.containsProject(originalVersion.projectName)
				? newProjects.forName(originalVersion.projectName) : new ProjectVersion(
						originalVersion.projectName, originalVersion.bumpedVersion());
	}

	ProjectVersion originalVersion(File project) {
		return new ProjectVersion(project);
	}

	public ExecutionResult pushCurrentBranch(File project) {
		this.projectGitHandler.pushCurrentBranch(project);
		log.info("\nSuccessfully pushed current branch");
		return ExecutionResult.success();
	}

	public ExecutionResult closeMilestone(ProjectVersion releaseVersion) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't close a milestone for a SNAPSHOT version");
			return ExecutionResult.skipped();
		}
		try {
			this.projectGitHubHandler.closeMilestone(releaseVersion);
			log.info("\nSuccessfully closed milestone");
			return ExecutionResult.success();
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(ex);
		}
	}

	public ExecutionResult createEmail(ProjectVersion releaseVersion, Projects projects) {
		Assert.notNull(releaseVersion,
				"You must provide a release version for your project");
		Assert.notNull(releaseVersion.version,
				"You must provide a release version for your project");
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create email template for a SNAPSHOT version");
			return ExecutionResult.skipped();
		}
		try {
			File email = this.templateGenerator.email(projects);
			if (email != null) {
				log.info("\nSuccessfully created email template at location [{}]", email);
				return ExecutionResult.success();
			}
			else {
				return ExecutionResult.unstable(
						new BuildUnstableException("Failed to create an email template"));
			}
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(
					new BuildUnstableException("Failed to create an email template", ex));
		}
	}

	public ExecutionResult createBlog(ProjectVersion releaseVersion, Projects projects) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create blog template for a SNAPSHOT version");
			return ExecutionResult.skipped();
		}
		try {
			File blog = this.templateGenerator.blog(projects);
			if (blog != null) {
				log.info("\nSuccessfully created blog template at location [{}]", blog);
				return ExecutionResult.success();
			}
			else {
				return ExecutionResult.unstable(
						new BuildUnstableException("Failed to create a blog template"));
			}
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(
					new BuildUnstableException("Failed to create a blog template", ex));
		}
	}

	public ExecutionResult updateSpringGuides(ProjectVersion releaseVersion,
			Projects projects, List<ProcessedProject> processedProjects) {
		if (!(releaseVersion.isRelease() || releaseVersion.isServiceRelease())) {
			log.info(
					"\nWon't update Spring Guides for a non Release / Service Release version");
			return ExecutionResult.skipped();
		}
		Exception springGuidesException = createIssueInSpringGuides(releaseVersion,
				projects);
		Exception deployGuidesException = deployGuides(processedProjects);
		if (springGuidesException != null || deployGuidesException != null) {
			return ExecutionResult.unstable(new BuildUnstableException(
					"Failed to update Spring Guides. Spring Guides updated successfully ["
							+ (springGuidesException != null)
							+ "] deployed guides successfully ["
							+ (deployGuidesException != null) + "]"));
		}
		return ExecutionResult.success();
	}

	public ExecutionResult updateStartSpringIo(ProjectVersion releaseVersion,
			Projects projects) {
		if (!(releaseVersion.isRelease() || releaseVersion.isServiceRelease())) {
			log.info(
					"\nWon't update start.spring.io for a non Release / Service Release version");
			return ExecutionResult.skipped();
		}
		Exception exception = createIssueInStartSpringIo(releaseVersion, projects);
		if (exception != null) {
			return ExecutionResult.unstable(
					new BuildUnstableException("Failed to update start.spring.io"));
		}
		return ExecutionResult.success();
	}

	private Exception createIssueInSpringGuides(ProjectVersion releaseVersion,
			Projects projects) {
		try {
			this.projectGitHubHandler.createIssueInSpringGuides(projects, releaseVersion);
			log.info("\nSuccessfully created an issue in Spring Guides");
			return null;
		}
		catch (Exception ex) {
			log.error("Failed to update Spring Guides repo", ex);
			return new BuildUnstableException("Failed to update Spring Guides repo", ex);
		}
	}

	private Exception createIssueInStartSpringIo(ProjectVersion releaseVersion,
			Projects projects) {
		try {
			this.projectGitHubHandler.createIssueInStartSpringIo(projects,
					releaseVersion);
			log.info("\nSuccessfully created an issue in start.spring.io");
			return null;
		}
		catch (Exception ex) {
			log.error("Failed to update start.spring.io repo", ex);
			return new BuildUnstableException("Failed to update start.spring.io repo",
					ex);
		}
	}

	private Exception deployGuides(List<ProcessedProject> processedProjects) {
		try {
			this.postReleaseActions.deployGuides(processedProjects);
			log.info("\nSuccessfully updated the guides repo");
			return null;
		}
		catch (Exception ex) {
			log.error("Failed to deploy new guides", ex);
			return new BuildUnstableException("Failed to deploy new guides", ex);
		}
	}

	public ExecutionResult createTweet(ProjectVersion releaseVersion, Projects projects) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create tweet template for a SNAPSHOT version");
			return ExecutionResult.skipped();
		}
		try {
			File tweet = this.templateGenerator.tweet(projects);
			if (tweet != null) {
				log.info("\nSuccessfully created tweet template at location [{}]", tweet);
				return ExecutionResult.success();
			}
			else {
				return ExecutionResult.unstable(
						new BuildUnstableException("Failed to create a tweet template"));
			}
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(
					new BuildUnstableException("Failed to create a tweet template", ex));
		}
	}

	public ExecutionResult createReleaseNotes(ProjectVersion releaseVersion,
			Projects projects) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create release notes for a SNAPSHOT version");
			return ExecutionResult.skipped();
		}
		try {
			File output = this.templateGenerator.releaseNotes(projects);
			log.info("\nSuccessfully created release notes at location [{}]", output);
			return ExecutionResult.success();
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(
					new BuildUnstableException("Failed to create release notes", ex));
		}
	}

	public ExecutionResult updateSagan(File project, ProjectVersion releaseVersion,
			Projects projects) {
		String currentBranch = this.projectGitHandler.currentBranch(project);
		ProjectVersion originalVersion = new ProjectVersion(project);
		try {
			return this.saganUpdater.updateSagan(project, currentBranch, originalVersion,
					releaseVersion, projects);
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(new BuildUnstableException(ex));
		}
	}

	public ExecutionResult updateDocumentationRepositoryForTrain(
			ReleaserProperties properties, Projects projects,
			ProjectVersion releaseVersion) {
		String releaseBranch = properties.getPom().getBranch();
		File file = this.documentationUpdater.updateDocsRepo(projects, releaseVersion,
				releaseBranch);
		if (file != null) {
			log.info(
					"\nSuccessfully updated documentation repository for train branch [{}]",
					releaseBranch);
			return ExecutionResult.success();
		}
		return ExecutionResult.skipped();
	}

	public ExecutionResult updateDocumentationRepositoryForSingleProject(
			Projects projects, ProjectVersion releaseVersion) {
		if (releaseVersion.projectName
				.equals(projects.releaseTrain(this.releaserProperties).projectName)) {
			log.info("Will not update documentation for project that is a BOM project");
			return ExecutionResult.skipped();
		}
		File file = this.documentationUpdater.updateDocsRepoForSingleProject(projects,
				releaseVersion);
		if (file != null) {
			log.info(
					"\nSuccessfully updated documentation repository for a project with name [{}]",
					releaseVersion.projectName);
			return ExecutionResult.success();
		}
		return ExecutionResult.skipped();
	}

	public ExecutionResult runUpdatedSamples(Projects projects) {
		return this.postReleaseActions.runUpdatedTests(projects);
	}

	public ExecutionResult generateReleaseTrainDocumentation(Projects projects) {
		return this.postReleaseActions.generateReleaseTrainDocumentation(projects);
	}

	public ExecutionResult updateAllSamples(Projects projects) {
		return this.postReleaseActions.updateAllTestSamples(projects);
	}

	public ExecutionResult updateReleaseTrainWiki(Projects projects) {
		File file = this.documentationUpdater.updateReleaseTrainWiki(projects);
		if (file != null) {
			log.info("\nSuccessfully updated project wiki");
			return ExecutionResult.success();
		}
		return ExecutionResult.skipped();
	}

}
