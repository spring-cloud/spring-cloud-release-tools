package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;

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
	public static final String RELEASE_TRAIN_PROJECT_NAME = "spring-cloud-release";

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
		// if meta release, first clone, then continue as usual
		if (options.metaRelease) {
			log.info("Meta Release picked. Will iterate over all projects and perform release of each one");
			Set<String> projects = this.properties.getFixedVersions().keySet();
			projects.forEach(project -> {
				File clonedProjectFromOrg = this.releaser.clonedProjectFromOrg(project);
				log.info("Successfully cloned the project [{}] to [{}]", project, clonedProjectFromOrg);
				processProject(options, clonedProjectFromOrg);
			});
			this.optionsProcessor.postReleaseOptions(options, postReleaseOptionsAgs(options));
		} else {
			log.info("Single project release picked. Will release only the current project");
			String workingDir = this.properties.getWorkingDir();
			File project = new File(workingDir);
			processProject(options, project);
		}
	}

	Args postReleaseOptionsAgs(Options options) {
		Projects projects = projectsToUpdateForFixedVersions();
		ProjectVersion version = projects.forName(RELEASE_TRAIN_PROJECT_NAME);
		return new Args(this.releaser, projects, version,
				this.properties, options.interactive);
	}

	private void processProject(Options options, File project) {
		ProjectVersion originalVersion = new ProjectVersion(project);
		ProjectVersion versionFromScRelease;
		Projects projectsToUpdate;
		if (this.properties.getGit().isFetchVersionsFromGit() && !this.properties.getMetaRelease().isEnabled()) {
			printVersionRetrieval();
			projectsToUpdate = this.releaser.retrieveVersionsFromSCRelease();
			versionFromScRelease = projectsToUpdate.forFile(project);
			assertNoSnapshotsForANonSnapshotProject(projectsToUpdate, versionFromScRelease);
		} else {
			String fixedVersionForProject = this.properties.getFixedVersions().get(originalVersion.projectName);
			versionFromScRelease = new ProjectVersion(originalVersion.projectName, fixedVersionForProject == null ?
			originalVersion.version : fixedVersionForProject);
			projectsToUpdate = this.properties.getFixedVersions().entrySet().stream()
					.map(entry -> new ProjectVersion(entry.getKey(), entry.getValue()))
					.distinct().collect(Collectors.toCollection(Projects::new));
			projectsToUpdate.add(versionFromScRelease);
			printSettingVersionFromFixedVersions(projectsToUpdate);
		}
		final Args defaultArgs = new Args(this.releaser, project, projectsToUpdate,
				originalVersion, versionFromScRelease, this.properties, options.interactive);
		this.optionsProcessor.processOptions(options, defaultArgs);
	}

	private Projects projectsToUpdateForFixedVersions() {
		Projects projectsToUpdate = this.properties.getFixedVersions().entrySet().stream()
				.map(entry -> new ProjectVersion(entry.getKey(), entry.getValue()))
				.distinct().collect(Collectors.toCollection(Projects::new));
		printSettingVersionFromFixedVersions(projectsToUpdate);
		return projectsToUpdate;
	}

	private void printVersionRetrieval() {
		log.info("\n\n\n=== RETRIEVING VERSIONS ===\n\nWill clone Spring Cloud Release"
				+ " to retrieve all versions for the branch [{}]", this.properties.getPom().getBranch());
	}

	private void printSettingVersionFromFixedVersions(Projects projectsToUpdate) {
		log.info("\n\n\n=== RETRIEVED VERSIONS ===\n\nWill use the fixed versions"
				+ " of projects\n\n {}", projectsToUpdate
				.stream().map(p -> p.projectName + " => " + p.version)
				.collect(Collectors.joining("\n")));
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

