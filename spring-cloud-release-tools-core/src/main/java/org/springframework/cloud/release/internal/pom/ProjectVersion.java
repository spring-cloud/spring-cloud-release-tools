package org.springframework.cloud.release.internal.pom;

import java.io.File;

/**
 * Object representing a root project's version.
 * Knows how to provide a minor bumped version;
 *
 * @author Marcin Grzejszczak
 */
public class ProjectVersion {

	public static ProjectVersion NO_VERSION = new ProjectVersion("");

	public final String version;
	private final PomReader pomReader = new PomReader();

	public ProjectVersion(String version) {
		this.version = version;
	}

	public ProjectVersion(File project) {
		this.version = this.pomReader.readPom(project).getVersion();
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

	@Override public String toString() {
		return this.version;
	}
}
