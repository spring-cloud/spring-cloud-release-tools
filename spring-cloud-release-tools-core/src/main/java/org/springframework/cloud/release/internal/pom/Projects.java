package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.ReleaserProperties;

/**
 * Abstraction over collection of projects
 *
 * @author Marcin Grzejszczak
 */
public class Projects extends HashSet<ProjectVersion> {

	public Projects(Set<ProjectVersion> versions) {
		addAll(versions);
	}

	@SuppressWarnings("unchecked")
	public Projects(ProjectVersion... versions) {
		addAll(new HashSet<>(Arrays.stream(versions).filter(Objects::nonNull).collect(Collectors.toList())));
	}

	public static Projects forRollback(Projects projects, ProjectVersion originalVersion) {
		Projects newProjects = new Projects();
		newProjects.add(new ProjectVersion(originalVersion.projectName, originalVersion.bumpedVersion()));
		newProjects.addAll(projects.forNameStartingWith(SpringCloudConstants.SPRING_BOOT));
		return newProjects;
	}

	public ProjectVersion releaseTrain(ReleaserProperties properties) {
		return this.forName(properties.getMetaRelease().getReleaseTrainProjectName());
	}

	@Override public boolean add(ProjectVersion projectVersion) {
		if (projectVersion == null) {
			return false;
		}
		return super.add(projectVersion);
	}

	public Projects filter(List<String> projectsToSkip) {
		return this.stream()
				.filter(v -> !projectsToSkip.contains(v.projectName))
				.collect(Collectors.toCollection(Projects::new));
	}

	public Projects postReleaseSnapshotVersion(List<String> projectsToSkip) {
		Projects projects = this.stream()
				.filter(v -> projectsToSkip.contains(v.projectName))
				.collect(Collectors.toCollection(Projects::new));
		Projects bumped = this.stream()
				.map(v -> new ProjectVersion(v.projectName, v.postReleaseSnapshotVersion()))
				.collect(Collectors.toCollection(Projects::new));
		Projects merged = new Projects(projects);
		merged.addAll(bumped);
		return merged;
	}

	public void remove(String projectName) {
		ProjectVersion projectVersion = forName(projectName);
		remove(projectVersion);
	}

	public ProjectVersion forFile(File projectRoot) {
		final ProjectVersion thisProject = new ProjectVersion(projectRoot);
		return this.stream().filter(projectVersion -> projectVersion.projectName.equals(thisProject.projectName))
				.findFirst()
				.orElseThrow(() -> exception(thisProject.projectName));
	}

	private static IllegalStateException exception(String projectName) {
		return new IllegalStateException(
				"Project with name [" + projectName + "] is not present. "
						+ additionalErrorMessage(projectName));
	}

	private static IllegalStateException exceptionStartingWithName(String projectName) {
		return new IllegalStateException(
				"Project starting with name [" + projectName + "] is not present. "
						+ additionalErrorMessage(projectName));
	}

	private static String additionalErrorMessage(String projectName) {
		return "Either put it in the Spring Cloud Release project or set it via the [--releaser.fixed-versions["
				+ projectName + "]=1.0.0.RELEASE] property";
	}

	public ProjectVersion forName(String projectName) {
		return this.stream().filter(projectVersion -> projectVersion.projectName.equals(projectName))
				.findFirst()
				.orElseThrow(() -> exception(projectName));
	}

	public boolean containsProject(String projectName) {
		return this.stream()
				.anyMatch(projectVersion -> projectVersion.projectName.equals(projectName));
	}

	public List<ProjectVersion> forNameStartingWith(String projectName) {
		return this.stream().filter(projectVersion -> projectVersion.projectName.startsWith(projectName))
				.collect(Collectors.toList());
	}

	public boolean containsSnapshots() {
		return this.stream().anyMatch(ProjectVersion::isSnapshot);
	}
}
