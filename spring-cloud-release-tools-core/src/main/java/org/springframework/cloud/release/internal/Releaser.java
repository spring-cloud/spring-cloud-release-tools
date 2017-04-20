package org.springframework.cloud.release.internal;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.cloud.release.internal.git.ProjectGitUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.project.ProjectBuilder;

/**
 * @author Marcin Grzejszczak
 */
public class Releaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ProjectPomUpdater projectPomUpdater;
	private final ProjectBuilder projectBuilder;
	private final ProjectGitUpdater projectGitUpdater;
	private final TemplateGenerator templateGenerator;

	public Releaser(ProjectPomUpdater projectPomUpdater, ProjectBuilder projectBuilder,
			ProjectGitUpdater projectGitUpdater, TemplateGenerator templateGenerator) {
		this.projectPomUpdater = projectPomUpdater;
		this.projectBuilder = projectBuilder;
		this.projectGitUpdater = projectGitUpdater;
		this.templateGenerator = templateGenerator;
	}

	public Projects retrieveVersionsFromSCRelease() {
		return this.projectPomUpdater.retrieveVersionsFromSCRelease();
	}

	public void updateProjectFromScRelease(File project, Projects versions) {
		this.projectPomUpdater.updateProjectFromSCRelease(project, versions);
		ProjectVersion changedVersion = new ProjectVersion(project);
		log.info("\n\nProject was successfully updated to [{}]", changedVersion);
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

	public void rollbackReleaseVersion(File project, ProjectVersion originalVersion, ProjectVersion scReleaseVersion) {
		if (scReleaseVersion.isSnapshot()) {
			log.info("\nWon't rollback a snapshot version");
			return;
		}
		this.projectGitUpdater.revertChangesIfApplicable(project, scReleaseVersion);
		if ((scReleaseVersion.isRelease() || scReleaseVersion.isServiceRelease()) && originalVersion.isSnapshot()) {
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
		if (releaseVersion.isSnapshot()) {
			log.info("\nWon't close a milestone for a SNAPSHOT version");
			return;
		}
		this.projectGitUpdater.closeMilestone(releaseVersion);
		log.info("\nSuccessfully closed milestone");
	}

	public void createEmail(ProjectVersion releaseVersion) {
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
}
