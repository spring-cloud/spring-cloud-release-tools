package org.springframework.cloud.release.internal.pom;

import java.io.File;

import org.apache.maven.model.Model;

/**
 * Object representing a root project's version.
 * Knows how to provide a minor bumped version;
 *
 * @author Marcin Grzejszczak
 */
public class ProjectVersion {

	public final String projectName;
	public final String version;
	private final PomReader pomReader = new PomReader();

	public ProjectVersion(String projectName, String version) {
		this.projectName = nameWithoutParent(projectName);
		this.version = version;
	}

	public ProjectVersion(File project) {
		Model model = this.pomReader.readPom(project);
		this.projectName = nameWithoutParent(model.getArtifactId());
		this.version = model.getVersion();
	}

	private String nameWithoutParent(String projectName) {
		boolean containsParent = projectName.endsWith("-parent");
		if (!containsParent) {
			return projectName;
		}
		return projectName.substring(0, projectName.indexOf("-parent"));
	}

	public String bumpedVersion() {
		// 1.0.0.BUILD-SNAPSHOT
		String[] splitVersion = this.version.split("\\.");
		if (splitVersion.length < 4) {
			throw new IllegalStateException("Version is invalid. Should be of format [1.2.3.A]");
		}
		Integer incrementedPatch = Integer.valueOf(splitVersion[2]) + 1;
		return String.format("%s.%s.%s.%s", splitVersion[0], splitVersion[1], incrementedPatch, splitVersion[3]);
	}

	public boolean isSnapshot() {
		return this.version.contains("SNAPSHOT");
	}

	public boolean isRc() {
		return this.version.contains("RC");
	}

	public boolean isMilestone() {
		return this.version.matches(".*M[0-9]+");
	}

	public boolean isRelease() {
		return this.version.contains("RELEASE");
	}

	public boolean isServiceRelease() {
		return this.version.matches(".*.SR[0-9]+");
	}

	@Override public String toString() {
		return this.version;
	}
}
