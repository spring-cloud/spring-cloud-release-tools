package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * Releaser that gets input from console
 *
 * @author Marcin Grzejszczak
 */
public class SpringReleaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Releaser releaser;
	private final ReleaserProperties properties;
	private final OptionsProcessor optionsProcessor;

	@Autowired
	public SpringReleaser(Releaser releaser, ReleaserProperties properties) {
		this.releaser = releaser;
		this.properties = properties;
		this.optionsProcessor = new OptionsProcessor(releaser, properties);
	}

	SpringReleaser(Releaser releaser, ReleaserProperties properties,
			OptionsProcessor optionsProcessor) {
		this.releaser = releaser;
		this.properties = properties;
		this.optionsProcessor = optionsProcessor;
	}

	/**
	 * Default behaviour - interactive mode
	 */
	public void release() {
		release(new OptionsBuilder().options());
	}

	public void release(Options options) {
		printVersionRetrieval();
		String workingDir = this.properties.getWorkingDir();
		File project = new File(workingDir);
		ProjectVersion originalVersion = new ProjectVersion(project);
		Projects projectsFromScRelease = this.releaser.retrieveVersionsFromSCRelease();
		ProjectVersion versionFromScRelease = projectsFromScRelease.forFile(project);
		assertNoSnapshotsForANonSnapshotProject(projectsFromScRelease, versionFromScRelease);
		final Args defaultArgs = new Args(this.releaser, project, projectsFromScRelease,
				originalVersion, versionFromScRelease, this.properties, options.interactive);
		this.optionsProcessor.processOptions(options, defaultArgs);
	}

	private void printVersionRetrieval() {
		log.info("\n\n\n=== RETRIEVING VERSIONS ===\n\nWill clone Spring Cloud Release"
				+ " to retrieve all versions for the branch [{}]", this.properties.getPom().getBranch());
	}

	private void assertNoSnapshotsForANonSnapshotProject(Projects projects,
			ProjectVersion versionFromScRelease) {
		if (!versionFromScRelease.isSnapshot() && projects.containsSnapshots()) {
			throw new IllegalStateException("You are trying to release a non snapshot "
					+ "version [" + versionFromScRelease + "] of the project [" + versionFromScRelease.projectName + "] but "
					+ "there is at least one SNAPSHOT library version in the Spring Cloud Release project");
		}
	}


}

