package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstraction over collection of projects
 *
 * @author Marcin Grzejszczak
 */
public class Projects extends HashSet<ProjectVersion> {

	public Projects(Set<ProjectVersion> versions) {
		addAll(versions);
	}

	public ProjectVersion forFile(File projectRoot) {
		final ProjectVersion thisProject = new ProjectVersion(projectRoot);
		return this.stream().filter(projectVersion -> projectVersion.projectName.equals(thisProject.projectName))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Project with name [" + thisProject.projectName + "] is not present"));
	}
}
