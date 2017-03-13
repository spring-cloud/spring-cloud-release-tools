package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * Releaser that gets input from console
 *
 * @author Marcin Grzejszczak
 */
public class SpringReleaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String MSG = "'q' to quit and 's' to skip\n\n";
	private static final String SKIP = "s";
	private static final String QUIT = "q";

	private final Releaser releaser;
	private final ReleaserProperties properties;

	public SpringReleaser(Releaser releaser, ReleaserProperties properties) {
		this.releaser = releaser;
		this.properties = properties;
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
			changedVersion = this.releaser.updateProjectFromScRelease(project);
		}
		log.info("\n\n\n=== BUILD PROJECT ===\n\nPress ENTER to build the project {}", MSG);
		boolean skipBuild = skipStep();
		if (!skipBuild) {
			this.releaser.buildProject();
		}
		log.info("\n\n\n=== COMMITTING AND PUSHING TAGS ===\n\nPress ENTER to commit, tag and push the tag {}", MSG);
		boolean skipCommit = skipStep();
		if (!skipCommit) {
			this.releaser.commitAndPushTags(project, changedVersion);
		}
		log.info("\n\n\n=== ARTIFACT DEPLOYMENT ===\n\nPress ENTER to deploy the artifacts {}", MSG);
		boolean skipDeployment = skipStep();
		if (!skipDeployment) {
			this.releaser.deploy();
		}
		log.info("\n\n\n=== PUBLISHING DOCS ===\n\nPress ENTER to deploy the artifacts {}", MSG);
		boolean skipDocs = skipStep();
		if (!skipDocs) {
			this.releaser.publishDocs(changedVersion);
		}
		if (!changedVersion.isSnapshot()) {
			log.info("\n\n\n=== REVERTING CHANGES & BUMPING VERSION===\n\nPress ENTER to go "
					+ "back to snapshots and bump originalVersion by patch {}", MSG);
			boolean skipRevert = skipStep();
			if (!skipRevert) {
				this.releaser.rollbackReleaseVersion(project, originalVersion, changedVersion);
			}
		}
		log.info("\n\n\n=== PUSHING CHANGES===\n\nPress ENTER to push the commits {}", MSG);
		boolean skipPush = skipStep();
		if (!skipPush) {
			this.releaser.pushCurrentBranch(project);
		}
		if (!changedVersion.isSnapshot()) {
			log.info("\n\n\n=== CLOSING MILESTONE===\n\nPress ENTER to close the milestone at Github {}", MSG);
			boolean skipMilestone = skipStep();
			if (!skipMilestone) {
				this.releaser.closeMilestone(changedVersion);
			}
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
