package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		addAll(new HashSet<>(Arrays.asList(versions)));
	}

	public static Projects forRollback(Projects projects, ProjectVersion originalVersion) {
		Projects newProjects = new Projects();
		newProjects.add(new ProjectVersion(originalVersion.projectName, originalVersion.bumpedVersion()));
		newProjects.addAll(projects.forNameStartingWith(SpringCloudConstants.SPRING_BOOT));
		newProjects.add(projects.forName(SpringCloudConstants.BUILD_ARTIFACT_ID));
		return newProjects;
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
