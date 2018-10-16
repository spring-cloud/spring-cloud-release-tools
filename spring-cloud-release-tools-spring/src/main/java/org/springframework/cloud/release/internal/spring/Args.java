package org.springframework.cloud.release.internal.spring;

import java.io.File;

import org.apache.commons.validator.Arg;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * @author Marcin Grzejszczak
 */
class Args {
	final Releaser releaser;
	final File project;
	final Projects projects;
	final ProjectVersion originalVersion;
	final ProjectVersion versionFromScRelease;
	final ReleaserProperties properties;
	final boolean interactive;
	final TaskType taskType;

	Args(Releaser releaser, File project, Projects projects, ProjectVersion originalVersion,
			ProjectVersion versionFromScRelease, ReleaserProperties properties,
			boolean interactive, TaskType taskType) {
		this.releaser = releaser;
		this.project = project;
		this.projects = projects;
		this.originalVersion = originalVersion;
		this.versionFromScRelease = versionFromScRelease;
		this.properties = properties;
		this.interactive = interactive;
		this.taskType = taskType;
	}

	// Used by meta-release task
	Args(Releaser releaser, Projects projects,
			ProjectVersion versionFromScRelease,
			ReleaserProperties properties,
			boolean interactive) {
		this.releaser = releaser;
		this.project = null;
		this.projects = projects;
		this.originalVersion = null;
		this.versionFromScRelease = versionFromScRelease;
		this.properties = properties;
		this.interactive = interactive;
		this.taskType = TaskType.POST_RELEASE;
	}

	// Used for tests
	Args(TaskType taskType) {
		this.releaser = null;
		this.project = null;
		this.projects = null;
		this.originalVersion = null;
		this.versionFromScRelease = null;
		this.properties = null;
		this.interactive = false;
		this.taskType = taskType;
	}

	@Override
	public String toString() {
		return "Args{" +
				"releaser=" + this.releaser +
				", project=" + this.project +
				", projects=" + this.projects +
				", originalVersion=" + this.originalVersion +
				", versionFromScRelease=" + this.versionFromScRelease +
				", properties=" + this.properties +
				", interactive=" + this.interactive +
				", taskType=" + this.taskType +
				'}';
	}
}
