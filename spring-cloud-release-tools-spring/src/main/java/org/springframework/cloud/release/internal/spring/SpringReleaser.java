package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.StringUtils;

/**
 * Releaser that gets input from console
 *
 * @author Marcin Grzejszczak
 */
public class SpringReleaser {
	private static final Logger log = LoggerFactory.getLogger(SpringReleaser.class);

	private final Releaser releaser;
	private final ReleaserProperties properties;
	private final OptionsProcessor optionsProcessor;
	private final ReleaserPropertiesUpdater updater;
	private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

	public SpringReleaser(Releaser releaser, ReleaserProperties properties,
			ReleaserPropertiesUpdater updater) {
		this.releaser = releaser;
		this.properties = properties;
		this.updater = updater;
		this.optionsProcessor = new OptionsProcessor(releaser, properties);
	}

	SpringReleaser(Releaser releaser, ReleaserProperties properties,
			OptionsProcessor optionsProcessor, ReleaserPropertiesUpdater updater) {
		this.releaser = releaser;
		this.properties = properties;
		this.optionsProcessor = optionsProcessor;
		this.updater = updater;
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
			this.properties.getGit().setFetchVersionsFromGit(false);
			metaReleaseProjects(options)
					.forEach(project -> processProjectForMetaRelease(options, project));
		} else {
			log.info("Single project release picked. Will release only the current project");
			File projectFolder = projectFolder();
			projectsAndVersion = processProject(options, projectFolder, TaskType.RELEASE);
		}
		this.optionsProcessor.postReleaseOptions(options, postReleaseOptionsAgs(options, projectsAndVersion));
	}

	private void processProjectForMetaRelease(Options options, String project) {
		File clonedProjectFromOrg = this.releaser.clonedProjectFromOrg(project);
		ReleaserProperties copy = clonePropertiesForProject();
		updatePropertiesIfCustomConfigPresent(copy, clonedProjectFromOrg);
		log.info("Successfully cloned the project [{}] to [{}]", project, clonedProjectFromOrg);
		try {
			processProject(options, clonedProjectFromOrg, TaskType.RELEASE);
		} catch (Exception e) {
			log.error("\n\n\nBUILD FAILED!!!\n\nException occurred for project <" +
					project + "> \n\n", e);
			throw e;
		}
	}

	private ReleaserProperties clonePropertiesForProject() {
		ReleaserProperties copy = new ReleaserProperties();
		BeanUtils.copyProperties(this.properties, copy);
		return copy;
	}

	private void updatePropertiesIfCustomConfigPresent(ReleaserProperties copy,
			File clonedProjectFromOrg) {
		File releaserConfig = new File(clonedProjectFromOrg, "config/releaser.yml");
		if (releaserConfig.exists()) {
			try {
				ReleaserProperties releaserProperties = this.objectMapper
						.readValue(releaserConfig, ReleaserProperties.class);
				log.info("config/releaser.yml found. Will update the current properties");
				copy.setMaven(releaserProperties.getMaven());
				copy.setGradle(releaserProperties.getGradle());
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		log.info("Updating working directory to [{}]", clonedProjectFromOrg.getAbsolutePath());
		copy.setWorkingDir(clonedProjectFromOrg.getAbsolutePath());
		this.updater.updateProperties(copy);
	}

	private List<String> metaReleaseProjects(Options options) {
		List<String> projects = new ArrayList<>(this.properties.getFixedVersions().keySet());
		log.info("List of projects that should not be cloned {}", this.properties.getMetaRelease().getProjectsToSkip());
		List<String> filteredProjects = projects.stream()
				.filter(project -> !this.properties.getMetaRelease().getProjectsToSkip().contains(project))
				.collect(Collectors.toList());
		if (StringUtils.hasText(options.startFrom)) {
			int projectIndex = filteredProjects.indexOf(options.startFrom);
			if (projectIndex < 0) throw new IllegalStateException("Project [" + options.startFrom + "] not found");
			if (log.isDebugEnabled()) {
				log.debug("Index of project [{}] is [{}]", options.startFrom, projectIndex);
			}
			filteredProjects = filteredProjects.subList(projectIndex, filteredProjects.size());
			options.startFrom = "";
		} else if (!options.taskNames.isEmpty()) {
			filteredProjects = filteredProjects.stream()
					.filter(project -> options.taskNames.contains(project))
					.collect(Collectors.toList());
			options.taskNames = new ArrayList<>();
		}
		log.info("\n\n\nFor meta-release, will release the projects {}\n\n\n", filteredProjects);
		return filteredProjects;
	}

	private File projectFolder() {
		String workingDir = this.properties.getWorkingDir();
		return new File(workingDir);
	}

	Args postReleaseOptionsAgs(Options options, ProjectsAndVersion projectsAndVersion) {
		Projects projects = projectsAndVersion == null ?
				projectsToUpdateForFixedVersions() : projectsAndVersion.projectVersions;
		ProjectVersion version = projects.containsProject(this.properties.getMetaRelease().getReleaseTrainProjectName()) ?
				projects.forName(this.properties.getMetaRelease().getReleaseTrainProjectName()) : versionFromBranch();
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
		Projects projectsToUpdate = this.releaser.fixedVersions();
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

