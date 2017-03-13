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
	private static final String MSG = "'q' to quit and 's' to skip\n\n";
	private static final String SKIP = "s";
	private static final String QUIT = "q";

	private final ReleaserProperties properties;
	private final ProjectPomUpdater projectPomUpdater;
	private final ProjectBuilder projectBuilder;
	private final ProjectGitUpdater projectGitUpdater;

	public Releaser(ReleaserProperties properties, ProjectPomUpdater projectPomUpdater,
			ProjectBuilder projectBuilder, ProjectGitUpdater projectGitUpdater) {
		this.properties = properties;
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
		this.projectGitUpdater.revertChangesIfApplicable(project, changedVersion);
		this.projectBuilder.bumpVersions(originalVersion.bumpedVersion());
		this.projectGitUpdater.commitAfterBumpingVersions(project, originalVersion);
		log.info("\nSuccessfully reverted the commit and bumped snapshot versions");
	}

	public void pushCurrentBranch(File project) {
		this.projectGitUpdater.pushCurrentBranch(project);
		log.info("\nSuccessfully pushed current branch");
	}

	public void release() {
		String workingDir = this.properties.getWorkingDir();
		File project = new File(workingDir);
		log.info("\n\n\n=== UPDATING POMS ===\n\nWill run the application "
				+ "for root folder [{}]. \n\nPress ENTER to continue {}", workingDir, MSG);
		boolean skipPoms = skipStep();
		ProjectVersion originalVersion = new ProjectVersion(project);
		ProjectVersion changedVersion = new ProjectVersion(project);
		if (!skipPoms) {
			changedVersion = this.updateProjectFromScRelease(project);
		}
		log.info("\n\n\n=== BUILD PROJECT ===\n\nPress ENTER to build the project {}", MSG);
		boolean skipBuild = skipStep();
		if (!skipBuild) {
			this.buildProject();
		}
		log.info("\n\n\n=== COMMITTING AND PUSHING TAGS ===\n\nPress ENTER to commit, tag and push the tag {}", MSG);
		boolean skipCommit = skipStep();
		if (!skipCommit) {
			this.commitAndPushTags(project, changedVersion);
		}
		log.info("\n\n\n=== ARTIFACT DEPLOYMENT ===\n\nPress ENTER to deploy the artifacts {}", MSG);
		boolean skipDeployment = skipStep();
		if (!skipDeployment) {
			this.deploy();
		}
		log.info("\n\n\n=== PUBLISHING DOCS ===\n\nPress ENTER to deploy the artifacts {}", MSG);
		boolean skipDocs = skipStep();
		if (!skipDocs) {
			this.publishDocs(changedVersion);
		}
		if (!changedVersion.isSnapshot()) {
			log.info("\n\n\n=== REVERTING CHANGES & BUMPING VERSION===\n\nPress ENTER to go back to snapshots and bump originalVersion by patch {}", MSG);
			boolean skipRevert = skipStep();
			if (!skipRevert) {
				rollbackReleaseVersion(project, originalVersion, changedVersion);
			}
		}
		log.info("\n\n\n=== PUSHING CHANGES===\n\nPress ENTER to push the commits {}", MSG);
		boolean skipPush = skipStep();
		if (!skipPush) {
			this.pushCurrentBranch(project);
		}
	}

	boolean skipStep() {
		String input = System.console().readLine();
		switch (input.toLowerCase()) {
		case SKIP:
			return true;
		case QUIT:
			System.exit(0);
			return true;
		default:
			return false;
		}
	}
}
