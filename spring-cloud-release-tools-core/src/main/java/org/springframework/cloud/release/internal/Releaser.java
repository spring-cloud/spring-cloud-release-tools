package org.springframework.cloud.release.internal;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.builder.ProjectBuilder;
import org.springframework.cloud.release.internal.git.ProjectGitUpdater;
import org.springframework.cloud.release.internal.pom.ProjectUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class Releaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String MSG = "'q' to quit and 's' to skip\n\n";
	private static final String SKIP = "s";
	private static final String QUIT = "q";

	private final ReleaserProperties properties;
	private final ProjectUpdater projectUpdater;
	private final ProjectBuilder projectBuilder;
	private final ProjectGitUpdater projectGitUpdater;

	public Releaser(ReleaserProperties properties, ProjectUpdater projectUpdater,
			ProjectBuilder projectBuilder, ProjectGitUpdater projectGitUpdater) {
		this.properties = properties;
		this.projectUpdater = projectUpdater;
		this.projectBuilder = projectBuilder;
		this.projectGitUpdater = projectGitUpdater;
	}

	public void release() {
		String workingDir = StringUtils.hasText(this.properties.getWorkingDir()) ?
				this.properties.getWorkingDir() : System.getProperty("user.dir");
		File project = new File(workingDir);
		log.info("\n\n\n=== UPDATING POMS ===\n\nWill run the application "
				+ "for root folder [{}]. \n\nPress ENTER to continue {}", workingDir, MSG);
		boolean skipPoms = skipStep();
		ProjectVersion version = new ProjectVersion(project);
		if (!skipPoms) {
			this.projectUpdater.updateProject(project);
			log.info("\n\nProject was successfully updated");
		}
		log.info("\n\n\n=== BUILD PROJECT ===\n\nPress ENTER to build the project {}", MSG);
		boolean skipBuild = skipStep();
		if (!skipBuild) {
			this.projectBuilder.build();
			log.info("\nProject was successfully built");
		}
		log.info("\n\n\n=== COMMITTING AND PUSHING TAGS ===\n\nPress ENTER to commit, tag and push the tag {}", MSG);
		boolean skipCommit = skipStep();
		if (!skipCommit) {
			this.projectGitUpdater.commitAndTagIfApplicable(project, version);
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
