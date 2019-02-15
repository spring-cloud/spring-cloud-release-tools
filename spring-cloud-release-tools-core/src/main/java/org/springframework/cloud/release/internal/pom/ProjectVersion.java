/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;

import org.springframework.util.StringUtils;

/**
 * Object representing a root project's version. Knows how to provide a minor bumped
 * version;
 *
 * @author Marcin Grzejszczak
 */
public class ProjectVersion {

	private static final Pattern SNAPSHOT_PATTERN = Pattern
			.compile("^.*\\.(BUILD-)?SNAPSHOT.*$");

	private static final String MILESTONE_REGEX = ".*\\.M[0-9]+";

	private static final String RC_REGEX = "^.*\\.RC.*$";

	/**
	 * Name of the project.
	 */
	public final String projectName;

	/**
	 * Version of the project.
	 */
	public final String version;

	private final Model model;

	public ProjectVersion(String projectName, String version) {
		this.projectName = nameWithoutParent(projectName);
		this.version = version;
		this.model = null;
	}

	public ProjectVersion(File project) {
		if (new File(project, "build.gradle").exists()) {
			ProjectVersion projectVersion = notMavenProject(project);
			this.projectName = projectVersion.projectName;
			this.version = projectVersion.version;
			this.model = null;
		}
		else {
			PomReader pomReader = new PomReader();
			Model model = pomReader.readPom(project);
			this.projectName = nameWithoutParent(model.getArtifactId());
			this.version = model.getVersion();
			this.model = model;
		}
	}

	public static ProjectVersion notMavenProject(File file) {
		File parentFolder = file.getParentFile() != null ? file.getParentFile() : file;
		String name = parentFolder.getName();
		String version = "1.0.0.BUILD-SNAPSHOT";
		return new ProjectVersion(nameWithoutParent(name), version);
	}

	private static String nameWithoutParent(String projectName) {
		boolean containsParent = projectName.endsWith("-parent");
		if (!containsParent) {
			return projectName;
		}
		return projectName.substring(0, projectName.indexOf("-parent"));
	}

	public String bumpedVersion() {
		return bumpedVersion(assertVersion());
	}

	private String bumpedVersion(String[] splitVersion) {
		if (splitVersion.length == 2 && !isNumeric(splitVersion[0])) {
			return this.version;
		}
		Integer incrementedPatch = Integer.valueOf(splitVersion[2]) + 1;
		return String.format("%s.%s.%s.%s", splitVersion[0], splitVersion[1],
				incrementedPatch, splitVersion[3]);
	}

	private String[] assertVersion() {
		if (this.version == null) {
			throw new IllegalStateException("Version can't be null!");
		}
		// 1.0.0.BUILD-SNAPSHOT
		String[] splitVersion = this.version.split("\\.");
		if (splitVersion.length < 4 && isNumeric(splitVersion[0])
				|| splitVersion.length == 1 && !isNumeric(splitVersion[0])) {
			throw new IllegalStateException(
					"Version is invalid. Should be of format [1.2.3.A]");
		}
		return splitVersion;
	}

	/**
	 * For GA and SR will bump the snapshots in the rest of cases will return snapshot of
	 * the current version.
	 * @return the post release snapshot version
	 */
	public String postReleaseSnapshotVersion() {
		String[] strings = assertVersion();
		if (isReleaseOrServiceRelease()) {
			String bumpedVersion = bumpedVersion(strings);
			return appendBuildSnapshot(bumpedVersion);
		}
		return appendBuildSnapshot(this.version);
	}

	private String appendBuildSnapshot(String bumpedVersion) {
		int lastIndexOfDot = bumpedVersion.lastIndexOf(".");
		return bumpedVersion.substring(0, lastIndexOfDot) + ".BUILD-SNAPSHOT";
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
		if (this.model.getParent() != null
				&& StringUtils.hasText(this.model.getParent().getGroupId())) {
			return this.model.getParent().getGroupId();
		}
		return "";
	}

	public boolean isValid() {
		try {
			assertVersion();
			return true;
		}
		catch (IllegalStateException e) {
			return false;
		}
	}

	public String major() {
		return this.assertVersion()[0];
	}

	public boolean isSnapshot() {
		return this.version != null && this.version.contains("SNAPSHOT");
	}

	public boolean isRc() {
		return this.version != null && this.version.matches(RC_REGEX);
	}

	public boolean isMilestone() {
		return this.version != null && this.version.matches(MILESTONE_REGEX);
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
		return splitThis.length == splitThat.length && splitThis[0].equals(splitThat[0])
				&& splitThis[1].equals(splitThat[1]);
	}

	public boolean isSameReleaseTrainName(String version) {
		assertVersionSet();
		String[] splitThis = this.version.split("\\.");
		String[] splitThat = version.split("\\.");
		return splitThis[0].compareToIgnoreCase(splitThat[0]) == 0;
	}

	private void assertVersionSet() {
		if (this.version == null) {
			throw new IllegalStateException("Version is not set");
		}
	}

	public int compareToReleaseTrainName(String version) {
		assertVersionSet();
		String[] split = version.split("\\.");
		String thatName = split[0];
		String thatValue = split.length > 1 ? split[1] : "";
		String[] thisSplit = this.version.split("\\.");
		String thisName = thisSplit[0];
		String thisValue = thisSplit.length > 1 ? thisSplit[1] : "";
		int nameComparison = thisName.compareTo(thatName);
		if (nameComparison != 0) {
			return nameComparison;
		}
		return new VersionNumber(thisValue).compareTo(new VersionNumber(thatValue));
	}

	@Override
	public String toString() {
		return this.version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProjectVersion that = (ProjectVersion) o;
		return Objects.equals(this.projectName, that.projectName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.projectName);
	}

	public List<Pattern> unacceptableVersionPatterns() {
		if (isSnapshot()) {
			return Collections.emptyList();
		}
		else if (isMilestone() || isRc()) {
			return Collections.singletonList(SNAPSHOT_PATTERN);
		}
		// treat like GA
		return Arrays.asList(SNAPSHOT_PATTERN, Pattern.compile(MILESTONE_REGEX),
				Pattern.compile(RC_REGEX));
	}

}

class VersionNumber implements Comparable<VersionNumber> {

	private final String version;

	VersionNumber(String version) {
		this.version = version;
	}

	@Override
	public int compareTo(VersionNumber o) {
		String thisLower = this.version.toLowerCase();
		char thisFirst = thisLower.isEmpty() ? ' ' : thisLower.charAt(0);
		String thatLower = o.version.toLowerCase();
		char thatFirst = thatLower.isEmpty() ? ' ' : thatLower.charAt(0);
		// B < M < RC < R < S
		int charComparison = Character.compare(thisFirst, thatFirst);
		if (charComparison != 0) {
			return charComparison;
		}
		String thisVersion = this.version.replaceAll("\\D+", "");
		boolean thisVersionEmpty = StringUtils.isEmpty(this.version);
		String thatVersion = o.version.replaceAll("\\D+", "");
		boolean thatVersionEmpty = StringUtils.isEmpty(o.version);
		if (thisVersionEmpty || thatVersionEmpty) {
			return thisVersion.compareTo(thatVersion);
		}
		Integer thisNumber = Integer.valueOf(thisVersion);
		Integer thatNumber = Integer.valueOf(thatVersion);
		return thisNumber.compareTo(thatNumber);
	}

}
