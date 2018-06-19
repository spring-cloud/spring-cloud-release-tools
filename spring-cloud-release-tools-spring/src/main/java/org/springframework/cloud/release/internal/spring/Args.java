package org.springframework.cloud.release.internal.spring;

import java.io.File;

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
	final boolean assertMetaRelease;

	Args(Releaser releaser, File project, Projects projects, ProjectVersion originalVersion,
			ProjectVersion versionFromScRelease, ReleaserProperties properties,
			boolean interactive) {
		this(releaser, project, projects, originalVersion, versionFromScRelease,
				properties, interactive, true);
	}

	Args(Releaser releaser, File project, Projects projects, ProjectVersion originalVersion,
			ProjectVersion versionFromScRelease, ReleaserProperties properties,
			boolean interactive, boolean assertMetaRelease) {
		this.releaser = releaser;
		this.project = project;
		this.projects = projects;
		this.originalVersion = originalVersion;
		this.versionFromScRelease = versionFromScRelease;
		this.properties = properties;
		this.interactive = interactive;
		this.assertMetaRelease = assertMetaRelease;
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
		this.assertMetaRelease = false;
	}
}
