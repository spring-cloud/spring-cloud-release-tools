/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.project;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;

import org.springframework.cloud.release.internal.tech.PomReader;
import org.springframework.util.StringUtils;

/**
 * Object representing a root project's version. Knows how to provide a minor bumped
 * version;
 *
 * @author Marcin Grzejszczak
 */
public class ProjectVersion implements Comparable<ProjectVersion> {

	private static final Pattern SNAPSHOT_PATTERN = Pattern
			.compile("^.*[\\.|\\-](BUILD-)?SNAPSHOT.*$");

	private static final String MILESTONE_REGEX = ".*[\\.|\\-]M[0-9]+";

	private static final String RC_REGEX = "^.*[\\.|\\-]RC.*$";

	private static final String RELEASE_REGEX = "^.*[\\.|\\-]RELEASE.*$";

	private static final String SR_REGEX = "^.*[\\.|\\-]SR[0-9]+.*$";

	private static final List<Pattern> VALID_PATTERNS = Arrays.asList(SNAPSHOT_PATTERN,
			Pattern.compile(MILESTONE_REGEX), Pattern.compile(RC_REGEX),
			Pattern.compile(RELEASE_REGEX), Pattern.compile(SR_REGEX));

	/**
	 * Name of the project.
	 */
	public final String projectName;

	/**
	 * Version of the project.
	 */
	public final String version;

	private final String groupId;

	private final String artifactId;

	public ProjectVersion(String projectName, String version) {
		this.projectName = projectName;
		this.version = version;
		this.groupId = "";
		this.artifactId = "";
	}

	public ProjectVersion(File project) {
		if (new File(project, "build.gradle").exists()) {
			ProjectVersion projectVersion = gradleProject(project);
			this.projectName = projectVersion.projectName;
			this.version = projectVersion.version;
			this.groupId = new ProjectCommandExecutor().groupId();
			this.artifactId = projectName;
		}
		else {
			Model model = PomReader.readPom(project);
			if (model != null) {
				this.projectName = nameWithoutParent(model.getArtifactId());
				this.version = model.getVersion();
				this.groupId = groupId(model);
				this.artifactId = model.getArtifactId();
			}
			else {
				ProjectVersion projectVersion = notMavenProject(project);
				this.projectName = projectVersion.projectName;
				this.version = projectVersion.version;
				this.groupId = projectVersion.groupId;
				this.artifactId = projectVersion.artifactId;
			}
		}
	}

	public static ProjectVersion notMavenProject(File file) {
		File parentFolder = file.getParentFile() != null ? file.getParentFile() : file;
		String name = parentFolder.getName();
		String version = "1.0.0.BUILD-SNAPSHOT";
		return new ProjectVersion(nameWithoutParent(name), version);
	}

	public static ProjectVersion gradleProject(File file) {
		File parentFolder = file.getParentFile() != null ? file.getParentFile() : file;
		String name = parentFolder.getName();
		String version = new ProjectCommandExecutor().version();
		return new ProjectVersion(nameWithoutParent(name), version);
	}

	private static String nameWithoutParent(String projectName) {
		boolean containsParent = projectName.endsWith("-parent");
		if (!containsParent) {
			return projectName;
		}
		return projectName.substring(0, projectName.indexOf("-parent"));
	}

	private String groupId(Model model) {
		if (model == null) {
			return "";
		}
		if (StringUtils.hasText(model.getGroupId())) {
			return model.getGroupId();
		}
		if (model.getParent() != null
				&& StringUtils.hasText(model.getParent().getGroupId())) {
			return model.getParent().getGroupId();
		}
		return "";
	}

	public String bumpedVersion() {
		return bumpedVersion(assertVersion()).print();
	}

	private SplitVersion bumpedVersion(SplitVersion splitVersion) {
		if (splitVersion.isReleaseTrain()) {
			return splitVersion;
		}
		return splitVersion.fullVersionWithIncrementedPatch();
	}

	private SplitVersion assertVersion() {
		if (this.version == null) {
			throw new IllegalStateException("Version can't be null!");
		}
		SplitVersion splitByHyphen = tryHyphenSeparatedVersion();
		if (splitByHyphen != null) {
			return splitByHyphen;
		}
		return dotSeparatedReleaseTrainsAndVersions();
	}

	private SplitVersion tryHyphenSeparatedVersion() {
		// Check for hyphen separated BOMs versioning
		// Dysprosium-BUILD-SNAPSHOT or Dysprosium-RELEASE
		// 1.0.0-BUILD-SNAPSHOT or 1.0.0-RELEASE
		String[] splitByHyphen = this.version.split("\\-");
		int splitByHyphens = splitByHyphen.length;
		int numberOfHyphens = splitByHyphens - 1;
		int indexOfFirstHyphen = this.version.indexOf("-");
		boolean buildSnapshot = this.version.endsWith("BUILD-SNAPSHOT");
		if (numberOfHyphens == 1 && !buildSnapshot
				|| (numberOfHyphens > 1 && buildSnapshot)) {
			// Dysprosium or 1.0.0
			String versionName = this.version.substring(0, indexOfFirstHyphen);
			boolean hasDots = versionName.contains(".");
			// BUILD-SNAPSHOT
			String versionType = this.version.substring(indexOfFirstHyphen + 1);
			// Dysprosium-BUILD-SNAPSHOT
			if (splitByHyphens > 1 && !hasDots && validVersionType()) {
				return SplitVersion.hyphen(versionName, versionType);
			}
			// Dysprosium-RELEASE
			else if (splitByHyphens == 1 && !hasDots && validVersionType()) {
				return SplitVersion.hyphen(splitByHyphen[0], splitByHyphen[1]);
			}
			// 1.0.0-RELEASE or 1.0.0-BUILD-SNAPSHOT
			else if (splitByHyphens >= 1 && hasDots) {
				String[] newArray = combinedArrays(versionName, versionType);
				return SplitVersion.hyphen(newArray);
			}
			else {
				throw new UnsupportedOperationException(
						"Unknown version [" + this.version + "]");
			}
		}
		return null;
	}

	private boolean validVersionType() {
		return VALID_PATTERNS.stream().anyMatch(p -> p.matcher(this.version).matches());
	}

	private String[] combinedArrays(String versionName, String versionType) {
		String[] split = versionName.split("\\.");
		String[] newArray = new String[split.length + 1];
		for (int i = 0; i < split.length; i++) {
			newArray[i] = split[i];
		}
		newArray[split.length] = versionType;
		return newArray;
	}

	private SplitVersion dotSeparatedReleaseTrainsAndVersions() {
		// Hoxton.BUILD-SNAPSHOT or 1.0.0.BUILD-SNAPSHOT
		String[] splitVersion = this.version.split("\\.");
		return SplitVersion.dot(splitVersion);
	}

	/**
	 * For GA and SR will bump the snapshots in the rest of cases will return snapshot of
	 * the current version.
	 * @return the post release snapshot version
	 */
	public String postReleaseSnapshotVersion() {
		SplitVersion splitVersion = assertVersion();
		if (isReleaseOrServiceRelease()) {
			return bumpedVersion(splitVersion).withBuildSnapshot().print();
		}
		return splitVersion.withBuildSnapshot().print();
	}

	public String groupId() {
		return this.groupId;
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
		return this.assertVersion().major;
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
		return new TrainVersionNumber(thisValue)
				.compareTo(new TrainVersionNumber(thatValue));
	}

	/**
	 * Returns the release tag name.
	 * @return tag name or empty if non ga or sr.
	 */
	public String releaseTagName() {
		if (isReleaseOrServiceRelease()) {
			return "v" + this.version;
		}
		return "";
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

	@Override
	public int compareTo(ProjectVersion o) {
		// very simple comparison
		return this.version.compareTo(o.version);
	}

	private static final class SplitVersion {

		private static final String DOT = ".";

		private static final String HYPHEN = "-";

		private static final String BUILD_SNAPSHOT_SUFFIX = "BUILD-SNAPSHOT";

		final String major;

		final String minor;

		final String patch;

		final String delimiter;

		final String suffix;

		// 1.0.0.RELEASE
		// 1.0.0-RELEASE
		private SplitVersion(String major, String minor, String patch, String delimiter,
				String suffix) {
			this.major = major;
			this.minor = minor;
			this.patch = patch;
			this.delimiter = delimiter;
			this.suffix = suffix;
			assertIfValid();
		}

		// Hoxton.RELEASE
		// Hoxton-RELEASE
		private SplitVersion(String major, String delimiter, String suffix) {
			this(major, "", "", delimiter, suffix);
		}

		private SplitVersion(String[] args, String delimiter) {
			this.major = orDefault(args, 0);
			this.minor = orDefault(args, 1);
			this.patch = orDefault(args, 2);
			this.delimiter = delimiter;
			this.suffix = orDefault(args, 3);
			assertIfValid();
		}

		private static String orDefault(String[] args, int argIndex) {
			return args.length > argIndex ? args[argIndex] : "";
		}

		static SplitVersion hyphen(String major, String suffix) {
			return new SplitVersion(major, HYPHEN, suffix);
		}

		static SplitVersion hyphen(String[] args) {
			return version(args, HYPHEN);
		}

		static SplitVersion dot(String[] args) {
			return version(args, DOT);
		}

		private static SplitVersion version(String[] args, String delimiter) {
			if (args.length == 2) {
				return new SplitVersion(args[0], "", "", delimiter, args[1]);
			}
			else if (args.length == 3) {
				return new SplitVersion(args[0], args[1], "", delimiter, args[2]);
			}
			return new SplitVersion(args, delimiter);
		}

		private void assertIfValid() {
			if (isInvalid()) {
				throw new IllegalStateException(
						"Version is invalid. Should be of format [1.2.3.A] / [1.2.3-A] or [A.B] / [A-B]");
			}
		}

		private boolean isInvalid() {
			return wrongReleaseTrainVersion() || wrongLibraryVersion() || wrongDelimiter()
					|| noSuffix();
		}

		private boolean noSuffix() {
			return StringUtils.isEmpty(suffix);
		}

		// Hoxton.BUILD-SNAPSHOT or Hoxton-BUILD-SNAPSHOT
		private boolean isReleaseTrain() {
			return !isNumeric(this.major);
		}

		private SplitVersion fullVersionWithIncrementedPatch() {
			int incrementedPatch = Integer.parseInt(patch) + 1;
			return new SplitVersion(major, minor, Integer.toString(incrementedPatch),
					delimiter, suffix);
		}

		private String print() {
			// Finchley.SR2
			if (StringUtils.isEmpty(minor)) {
				return String.format("%s%s%s", major, delimiter, suffix);
			}
			return String.format("%s.%s.%s%s%s", major, minor, patch, delimiter, suffix);
		}

		private boolean isNumeric(String string) {
			return string.matches("[0-9]+");
		}

		private boolean wrongDelimiter() {
			return !(DOT.equals(this.delimiter) || HYPHEN.equals(this.delimiter));
		}

		private boolean wrongLibraryVersion() {
			// GOOD:
			// 1.2.3.RELEASE, 1.2.3-RELEASE, Hoxton.BUILD-SNAPSHOT, Hoxton-RELEASE
			// must have
			// either major and suffix (release train)
			// major, minor, patch and suffix
			return isNumeric(major) && (StringUtils.isEmpty(minor)
					|| StringUtils.isEmpty(patch) || StringUtils.isEmpty(suffix)
					|| StringUtils.isEmpty(delimiter));
		}

		private boolean wrongReleaseTrainVersion() {
			// BAD: 1.EXAMPLE, GOOD: Hoxton.RELEASE
			return isNumeric(major) && StringUtils.isEmpty(suffix);
		}

		private SplitVersion withBuildSnapshot() {
			return new SplitVersion(major, minor, patch, delimiter,
					BUILD_SNAPSHOT_SUFFIX);
		}

	}

}

class TrainVersionNumber implements Comparable<TrainVersionNumber> {

	private final String version;

	TrainVersionNumber(String version) {
		this.version = version;
	}

	@Override
	public int compareTo(TrainVersionNumber o) {
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
