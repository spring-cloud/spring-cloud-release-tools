package org.springframework.cloud.release.internal;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.cloud.release.internal.git.ProjectGitUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
public class Releaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ProjectPomUpdater projectPomUpdater;
	private final ProjectBuilder projectBuilder;
	private final ProjectGitUpdater projectGitUpdater;

	public Releaser(ProjectPomUpdater projectPomUpdater,
			ProjectBuilder projectBuilder, ProjectGitUpdater projectGitUpdater) {
		this.projectPomUpdater = projectPomUpdater;
		this.projectBuilder = projectBuilder;
		this.projectGitUpdater = projectGitUpdater;
	}

	public ProjectVersion updateProjectFromScRelease(File project) {
		this.projectPomUpdater.updateProjectFromSCRelease(project);
		ProjectVersion changedVersion = new ProjectVersion(project);
		log.info("\n\nProject was successfully updated to [{}]", changedVersion);
		return changedVersion;
	}

	public void buildProject() {
		this.projectBuilder.build();
		log.info("\nProject was successfully built");
	}

	public void commitAndPushTags(File project, ProjectVersion changedVersion) {
		this.projectGitUpdater.commitAndTagIfApplicable(project, changedVersion);
		log.info("\nCommit was made and tag was pushed successfully");
	}

	public void deploy() {
		this.projectBuilder.deploy();
		log.info("\nThe artifact was deployed successfully");
	}

	public void publishDocs(ProjectVersion changedVersion) {
		this.projectBuilder.publishDocs(changedVersion.version);
		log.info("\nThe docs were published successfully");
	}

	public void rollbackReleaseVersion(File project, ProjectVersion originalVersion, ProjectVersion changedVersion) {
		ProjectVersion version = new ProjectVersion(project);
		if (version.isSnapshot()) {
			log.info("\nCurrent pom contains snapshot version [{}]. Will not proceed with rollback", version.toString());
			return;
		}
		this.projectGitUpdater.revertChangesIfApplicable(project, changedVersion);
		if (changedVersion.isRelease()) {
			this.projectBuilder.bumpVersions(originalVersion.bumpedVersion());
			this.projectGitUpdater.commitAfterBumpingVersions(project, originalVersion);
			log.info("\nSuccessfully reverted the commit and bumped snapshot versions");
		} else {
			log.info("\nSuccessfully reverted the commit and came back to snapshot versions");
		}
	}

	public void pushCurrentBranch(File project) {
		this.projectGitUpdater.pushCurrentBranch(project);
		log.info("\nSuccessfully pushed current branch");
	}

	public void closeMilestone(ProjectVersion releaseVersion) {
		this.projectGitUpdater.closeMilestone(releaseVersion);
		log.info("\nSuccessfully closed milestone");
	}
}
