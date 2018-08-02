package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.util.Objects;

import org.apache.maven.model.Model;
import org.springframework.util.StringUtils;

/**
 * Object representing a root project's version.
 * Knows how to provide a minor bumped version;
 *
 * @author Marcin Grzejszczak
 */
public class ProjectVersion {

	public final String projectName;
	public final String version;
	private final Model model;

	public ProjectVersion(String projectName, String version) {
		this.projectName = nameWithoutParent(projectName);
		this.version = version;
		this.model = null;
	}

	public ProjectVersion(File project) {
		PomReader pomReader = new PomReader();
		Model model = pomReader.readPom(project);
		this.projectName = nameWithoutParent(model.getArtifactId());
		this.version = model.getVersion();
		this.model = model;
	}

	private String nameWithoutParent(String projectName) {
		boolean containsParent = projectName.endsWith("-parent");
		if (!containsParent) {
			return projectName;
		}
		return projectName.substring(0, projectName.indexOf("-parent"));
	}

	public String bumpedVersion() {
		if (this.version == null) {
			throw new IllegalStateException("Version can't be null!");
		}
		// 1.0.0.BUILD-SNAPSHOT
		String[] splitVersion = this.version.split("\\.");
		if (splitVersion.length < 4 && isNumeric(splitVersion[0])) {
			throw new IllegalStateException("Version is invalid. Should be of format [1.2.3.A]");
		}
		if (splitVersion.length == 2 && !isNumeric(splitVersion[0])) {
			return this.version;
		}
		Integer incrementedPatch = Integer.valueOf(splitVersion[2]) + 1;
		return String.format("%s.%s.%s.%s", splitVersion[0], splitVersion[1], incrementedPatch, splitVersion[3]);
	}

	private boolean isNumeric(String string) {
		return string.matches("[0-9]+");
	}

	public String groupId() {
		if (this.model == null) {
			return "";
		}
		if (StringUtils.hasText(this.model.getGroupId())) {
			return this.model.getGroupId();
		}
		if (this.model.getParent() != null && StringUtils.hasText(this.model.getParent().getGroupId())) {
			return this.model.getParent().getGroupId();
		}
		return "";
	}

	public boolean isSnapshot() {
		return this.version != null && this.version.contains("SNAPSHOT");
	}

	public boolean isRc() {
		return this.version != null && this.version.contains("RC");
	}

	public boolean isMilestone() {
		return this.version != null && this.version.matches(".*M[0-9]+");
	}

	public boolean isRelease() {
		return this.version != null && this.version.contains("RELEASE");
	}

	public boolean isReleaseOrServiceRelease() {
		return isRelease() || isServiceRelease();
	}

	public boolean isServiceRelease() {
		return this.version != null && this.version.matches(".*.SR[0-9]+");
	}

	public boolean isSameMinor(String version) {
		if (this.version == null) {
			return false;
		}
		String[] splitThis = this.version.split("\\.");
		String[] splitThat = version.split("\\.");
		return splitThis.length == splitThat.length &&
				splitThis[0].equals(splitThat[0]) && splitThis[1].equals(splitThat[1]);
	}

	@Override public String toString() {
		return this.version;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProjectVersion that = (ProjectVersion) o;
		return Objects.equals(this.projectName, that.projectName);
	}

	@Override public int hashCode() {
		return Objects.hash(this.projectName);
	}
}
