package org.springframework.cloud.release.internal;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.project.Project;
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
	private final Project project;
	private final ProjectGitUpdater projectGitUpdater;

	public Releaser(ReleaserProperties properties, ProjectPomUpdater projectPomUpdater,
			Project project, ProjectGitUpdater projectGitUpdater) {
		this.properties = properties;
		this.projectPomUpdater = projectPomUpdater;
		this.project = project;
		this.projectGitUpdater = projectGitUpdater;
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
			this.projectPomUpdater.updateProject(project);
			changedVersion = new ProjectVersion(project);
			log.info("\n\nProject was successfully updated to [{}]", originalVersion);
		}
		log.info("\n\n\n=== BUILD PROJECT ===\n\nPress ENTER to build the project {}", MSG);
		boolean skipBuild = skipStep();
		if (!skipBuild) {
			this.project.build();
			log.info("\nProject was successfully built");
		}
		log.info("\n\n\n=== COMMITTING AND PUSHING TAGS ===\n\nPress ENTER to commit, tag and push the tag {}", MSG);
		boolean skipCommit = skipStep();
		if (!skipCommit) {
			this.projectGitUpdater.commitAndTagIfApplicable(project, changedVersion);
		}
		log.info("\n\n\n=== ARTIFACT DEPLOYMENT ===\n\nPress ENTER to deploy the artifacts {}", MSG);
		boolean skipDeployment = skipStep();
		if (!skipDeployment) {
			this.project.deploy();
		}
		log.info("\n\n\n=== PUBLISHING DOCS ===\n\nPress ENTER to deploy the artifacts {}", MSG);
		boolean skipDocs = skipStep();
		if (!skipDocs) {
			this.project.publishDocs(changedVersion.version);
		}
		if (!changedVersion.isSnapshot()) {
			log.info("\n\n\n=== REVERTING CHANGES & BUMPING VERSION===\n\nPress ENTER to go back to snapshots and bump originalVersion by patch {}", MSG);
			boolean skipRevert = skipStep();
			if (!skipRevert) {
				this.projectGitUpdater.revertChangesIfApplicable(project, changedVersion);
				this.project.bumpVersions(originalVersion.bumpedVersion());
				this.projectGitUpdater.commitAfterBumpingVersions(project, originalVersion);
			}
		}
		log.info("\n\n\n=== PUSHING CHANGES===\n\nPress ENTER to push the commits {}", MSG);
		boolean skipPush = skipStep();
		if (!skipPush) {
			this.projectGitUpdater.pushCurrentBranch(project);
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
