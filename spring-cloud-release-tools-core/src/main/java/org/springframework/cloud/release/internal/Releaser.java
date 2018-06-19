package org.springframework.cloud.release.internal;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.gradle.GradleUpdater;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.util.Assert;

/**
 * @author Marcin Grzejszczak
 */
public class Releaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static boolean ASSERT_SNAPSHOTS = true;
	private static boolean SKIP_SNAPSHOT_ASSERTION = false;

	private final ProjectPomUpdater projectPomUpdater;
	private final ProjectBuilder projectBuilder;
	private final ProjectGitHandler projectGitHandler;
	private final TemplateGenerator templateGenerator;
	private final GradleUpdater gradleUpdater;
	private final SaganUpdater saganUpdater;
	private final DocumentationUpdater documentationUpdater;

	public Releaser(ProjectPomUpdater projectPomUpdater, ProjectBuilder projectBuilder,
			ProjectGitHandler projectGitHandler, TemplateGenerator templateGenerator,
			GradleUpdater gradleUpdater, SaganUpdater saganUpdater,
			DocumentationUpdater documentationUpdater) {
		this.projectPomUpdater = projectPomUpdater;
		this.projectBuilder = projectBuilder;
		this.projectGitHandler = projectGitHandler;
		this.templateGenerator = templateGenerator;
		this.gradleUpdater = gradleUpdater;
		this.saganUpdater = saganUpdater;
		this.documentationUpdater = documentationUpdater;
	}

	public File clonedProjectFromOrg(String projectName) {
		return this.projectGitHandler.cloneProjectFromOrg(projectName);
	}

	public Projects retrieveVersionsFromSCRelease() {
		return this.projectPomUpdater.retrieveVersionsFromSCRelease();
	}

	public void updateProjectFromScRelease(File project, Projects versions,
			ProjectVersion versionFromScRelease) {
		updateProjectFromScRelease(project, versions, versionFromScRelease, ASSERT_SNAPSHOTS);
	}

	private void updateProjectFromScRelease(File project, Projects versions,
			ProjectVersion versionFromScRelease, boolean assertSnapshots) {
		this.projectPomUpdater.updateProjectFromSCRelease(project, versions,
				versionFromScRelease, assertSnapshots);
		this.gradleUpdater.updateProjectFromSCRelease(project, versions,
				versionFromScRelease, assertSnapshots);
		ProjectVersion changedVersion = new ProjectVersion(project);
		log.info("\n\nProject was successfully updated to [{}]", changedVersion);
	}

	public void buildProject(ProjectVersion versionFromScRelease) {
		this.projectBuilder.build(versionFromScRelease);
		log.info("\nProject was successfully built");
	}

	public void commitAndPushTags(File project, ProjectVersion changedVersion) {
		this.projectGitHandler.commitAndTagIfApplicable(project, changedVersion);
		log.info("\nCommit was made and tag was pushed successfully");
	}

	public void deploy(ProjectVersion versionFromScRelease) {
		this.projectBuilder.deploy(versionFromScRelease);
		log.info("\nThe artifact was deployed successfully");
	}

	public void publishDocs(ProjectVersion changedVersion) {
		this.projectBuilder.publishDocs(changedVersion.version);
		log.info("\nThe docs were published successfully");
	}

	public void rollbackReleaseVersion(File project, Projects projects, ProjectVersion scReleaseVersion) {
		if (scReleaseVersion.isSnapshot()) {
			log.info("\nWon't rollback a snapshot version");
			return;
		}
		this.projectGitHandler.revertChangesIfApplicable(project, scReleaseVersion);
		ProjectVersion originalVersion = originalVersion(project);
		log.info("Original project version is [{}]", originalVersion);
		if ((scReleaseVersion.isRelease() || scReleaseVersion.isServiceRelease()) && originalVersion.isSnapshot()) {
			Projects newProjects = Projects.forRollback(projects, originalVersion);
			updateProjectFromScRelease(project, newProjects, originalVersion, SKIP_SNAPSHOT_ASSERTION);
			this.projectGitHandler.commitAfterBumpingVersions(project, originalVersion);
			log.info("\nSuccessfully reverted the commit and bumped snapshot versions");
		} else {
			log.info("\nSuccessfully reverted the commit and came back to snapshot versions");
		}
	}

	ProjectVersion originalVersion(File project) {
		return new ProjectVersion(project);
	}

	public void pushCurrentBranch(File project) {
		this.projectGitHandler.pushCurrentBranch(project);
		log.info("\nSuccessfully pushed current branch");
	}

	public void closeMilestone(ProjectVersion releaseVersion) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't close a milestone for a SNAPSHOT version");
			return;
		}
		this.projectGitHandler.closeMilestone(releaseVersion);
		log.info("\nSuccessfully closed milestone");
	}

	public void createEmail(ProjectVersion releaseVersion) {
		Assert.notNull(releaseVersion, "You must provide a release version for your project");
		Assert.notNull(releaseVersion.version, "You must provide a release version for your project");
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create email template for a SNAPSHOT version");
			return;
		}
		File email = this.templateGenerator.email();
		log.info("\nSuccessfully created email template at location [{}]", email);
	}

	public void createBlog(ProjectVersion releaseVersion, Projects projects) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create blog template for a SNAPSHOT version");
			return;
		}
		File blog = this.templateGenerator.blog(projects);
		log.info("\nSuccessfully created blog template at location [{}]", blog);

	}

	public void updateSpringGuides(ProjectVersion releaseVersion, Projects projects) {
		if (!(releaseVersion.isRelease() || releaseVersion.isServiceRelease())) {
			log.info("\nWon't update Spring Guides for a non Release / Service Release version");
			return;
		}
		this.projectGitHandler.createIssueInSpringGuides(projects, releaseVersion);
		log.info("\nSuccessfully updated Spring Guides issues");
	}

	public void createTweet(ProjectVersion releaseVersion) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create tweet template for a SNAPSHOT version");
			return;
		}
		File blog = this.templateGenerator.tweet();
		log.info("\nSuccessfully created tweet template at location [{}]", blog);
	}

	public void createReleaseNotes(ProjectVersion releaseVersion, Projects projects) {
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't create release notes for a SNAPSHOT version");
			return;
		}
		File output = this.templateGenerator.releaseNotes(projects);
		log.info("\nSuccessfully created release notes at location [{}]", output);
	}

	public void updateSagan(File project, ProjectVersion releaseVersion) {
		String currentBranch = this.projectGitHandler.currentBranch(project);
		ProjectVersion originalVersion = new ProjectVersion(project);
		try {
			this.saganUpdater.updateSagan(currentBranch, originalVersion, releaseVersion);
			log.info("\nSuccessfully updated Sagan for branch [{}]", currentBranch);
		} catch (Exception e) {
			log.warn("\nUnable to update Sagan for branch ["+ currentBranch + "]", e);
		}
	}

	public void updateDocumentationRepository(ReleaserProperties properties, ProjectVersion releaseVersion) {
		String releaseBranch = properties.getPom().getBranch();
		try {
			this.documentationUpdater.updateDocsRepo(releaseVersion, releaseBranch);
			log.info("\nSuccessfully updated documentation repository for branch [{}]", releaseBranch);
		} catch (Exception e) {
			log.warn("\nUnable to update documentation repository for branch ["+ releaseBranch + "]", e);
		}
	}
}
