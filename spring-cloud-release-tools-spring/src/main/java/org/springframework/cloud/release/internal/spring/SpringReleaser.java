package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.StringUtils;

/**
 * Releaser that gets input from console
 *
 * @author Marcin Grzejszczak
 */
public class SpringReleaser {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String RELEASE_TRAIN_PROJECT_NAME = "spring-cloud-release";

	private final Releaser releaser;
	private final ReleaserProperties properties;
	private final OptionsProcessor optionsProcessor;
	private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

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
		ProjectsAndVersion projectsAndVersion = null;
		// if meta release, first clone, then continue as usual
		if (options.metaRelease) {
			log.info("Meta Release picked. Will iterate over all projects and perform release of each one");
			ReleaserProperties original = new ReleaserProperties();
			BeanUtils.copyProperties(this.properties, original);
			metaReleaseProjects(options).forEach(project -> {
				File clonedProjectFromOrg = this.releaser.clonedProjectFromOrg(project);
				updatePropertiesIfPresent(original, clonedProjectFromOrg);
				log.info("Successfully cloned the project [{}] to [{}]", project, clonedProjectFromOrg);
				try {
					processProject(options, clonedProjectFromOrg, TaskType.RELEASE);
				} catch (Exception e) {
					log.error("\n\n\nBUILD FAILED!!!\n\nException occurred for project <" +
							project + "> \n\n");
					throw e;
				}
			});
		} else {
			log.info("Single project release picked. Will release only the current project");
			File projectFolder = projectFolder();
			projectsAndVersion = processProject(options, projectFolder, TaskType.RELEASE);
		}
		this.optionsProcessor.postReleaseOptions(options, postReleaseOptionsAgs(options, projectsAndVersion));
	}

	private void updatePropertiesIfPresent(ReleaserProperties original,
			File clonedProjectFromOrg) {
		File releaserConfig = new File(clonedProjectFromOrg, "config/releaser.yml");
		if (releaserConfig.exists()) {
			try {
				ReleaserProperties releaserProperties = this.objectMapper
						.readValue(releaserConfig, ReleaserProperties.class);
				BeanUtils.copyProperties(releaserProperties, this.properties);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			BeanUtils.copyProperties(original, this.properties);
		}
	}

	private List<String> metaReleaseProjects(Options options) {
		List<String> projects = new ArrayList<>(this.properties.getFixedVersions().keySet());
		if (StringUtils.hasText(options.startFrom)) {
			int projectIndex = projects.indexOf(options.startFrom);
			if (projectIndex < 0) throw new IllegalStateException("Project [" + options.startFrom + "] not found");
			projects = projects.subList(projectIndex, projects.size());
			options.startFrom = "";
		} else if (!options.taskNames.isEmpty()) {
			projects = projects.stream()
					.filter(project -> options.taskNames.contains(project))
					.collect(Collectors.toList());
			options.taskNames = new ArrayList<>();
		}
		log.info("\n\n\nFor meta-release, will release the projects {}\n\n\n", projects);
		return projects;
	}

	private File projectFolder() {
		String workingDir = this.properties.getWorkingDir();
		return new File(workingDir);
	}

	Args postReleaseOptionsAgs(Options options, ProjectsAndVersion projectsAndVersion) {
		Projects projects = projectsAndVersion == null ?
				projectsToUpdateForFixedVersions() : projectsAndVersion.projectVersions;
		ProjectVersion version = projects.containsProject(RELEASE_TRAIN_PROJECT_NAME) ?
				projects.forName(RELEASE_TRAIN_PROJECT_NAME) : versionFromBranch();
		return new Args(this.releaser, projects, version,
				this.properties, options.interactive);
	}

	private ProjectVersion versionFromBranch() {
		String branch = this.properties.getPom().getBranch();
		return new ProjectVersion(projectFolder().getName(), branch.startsWith("v") ? branch.substring(1) : branch);
	}

	private ProjectsAndVersion projects(File project) {
		ProjectVersion versionFromScRelease;
		Projects projectsToUpdate;
		if (this.properties.getGit().isFetchVersionsFromGit() && !this.properties.getMetaRelease().isEnabled()) {
			printVersionRetrieval();
			projectsToUpdate = this.releaser.retrieveVersionsFromSCRelease();
			versionFromScRelease = projectsToUpdate.forFile(project);
			assertNoSnapshotsForANonSnapshotProject(projectsToUpdate, versionFromScRelease);
		} else {
			ProjectVersion originalVersion = new ProjectVersion(project);
			String fixedVersionForProject = this.properties.getFixedVersions().get(originalVersion.projectName);
			versionFromScRelease = new ProjectVersion(originalVersion.projectName, fixedVersionForProject == null ?
			originalVersion.version : fixedVersionForProject);
			projectsToUpdate = this.properties.getFixedVersions().entrySet().stream()
					.map(entry -> new ProjectVersion(entry.getKey(), entry.getValue()))
					.distinct().collect(Collectors.toCollection(Projects::new));
			projectsToUpdate.add(versionFromScRelease);
			printSettingVersionFromFixedVersions(projectsToUpdate);
		}
		return new ProjectsAndVersion(projectsToUpdate, versionFromScRelease);
	}

	class ProjectsAndVersion {
		final Projects projectVersions;
		final ProjectVersion versionFromScRelease;

		ProjectsAndVersion(Projects projectVersions, ProjectVersion versionFromScRelease) {
			this.projectVersions = projectVersions;
			this.versionFromScRelease = versionFromScRelease;
		}
	}

	private ProjectsAndVersion processProject(Options options, File project, TaskType taskType) {
		ProjectsAndVersion projectsAndVersion = projects(project);
		ProjectVersion originalVersion = new ProjectVersion(project);
		final Args defaultArgs = new Args(this.releaser, project, projectsAndVersion.projectVersions,
				originalVersion, projectsAndVersion.versionFromScRelease, this.properties,
				options.interactive, taskType);
		this.optionsProcessor.processOptions(options, defaultArgs);
		return projectsAndVersion;
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

