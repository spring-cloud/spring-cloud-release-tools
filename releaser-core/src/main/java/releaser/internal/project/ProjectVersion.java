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

package releaser.internal.project;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.tech.PomReader;

import org.springframework.util.StringUtils;

enum ReleaseType {

	SNAPSHOT, M, RC, RELEASE, SR

}

/**
 * Object representing a root project's version. Knows how to provide a minor bumped
 * version;
 *
 * @author Marcin Grzejszczak
 */
public class ProjectVersion implements Comparable<ProjectVersion>, Serializable {

	private static final Logger log = LoggerFactory.getLogger(ProjectVersion.class);

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

	private final ReleaseType releaseType;

	public ProjectVersion(String projectName, String version) {
		this.projectName = projectName;
		this.version = version;
		this.groupId = "";
		this.artifactId = "";
		this.releaseType = toReleaseType();
	}

	// for version comparison
	ProjectVersion(String version) {
		this.projectName = "";
		this.version = version;
		this.groupId = "";
		this.artifactId = "";
		this.releaseType = toReleaseType();
	}

	public ProjectVersion(File project) {
		File buildGradle = new File(project, "build.gradle");
		if (buildGradle.exists()) {
			ReleaserProperties properties = new ReleaserProperties();
			properties.setWorkingDir(project.getAbsolutePath());
			log.info("Retrieving fresh Gradle project version information");
			ProjectVersion projectVersion = gradleProject(buildGradle);
			this.projectName = projectVersion.projectName;
			this.version = projectVersion.version;
			this.groupId = new ProjectCommandExecutor().groupId(properties);
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
		this.releaseType = toReleaseType();
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
		ReleaserProperties properties = new ReleaserProperties();
		properties.setWorkingDir(parentFolder.getAbsolutePath());
		String version = new ProjectCommandExecutor().version(properties);
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
		return assertVersion(this.version);
	}

	private SplitVersion assertVersion(String version) {
		if (version == null || StringUtils.isEmpty(version)) {
			throw new IllegalStateException("Version can't be null!");
		}
		else if (version.endsWith(".") || version.endsWith("-")) {
			throw new IllegalStateException("Version can't end with a delimiter!");
		}
		SplitVersion splitByHyphen = tryHyphenSeparatedVersion(version);
		if (splitByHyphen != null) {
			return splitByHyphen;
		}
		return dotSeparatedReleaseTrainsAndVersions(version);
	}

	private SplitVersion tryHyphenSeparatedVersion(String version) {
		// Check for hyphen separated BOMs versioning
		// Dysprosium-BUILD-SNAPSHOT or Dysprosium-RELEASE
		// 1.0.0-BUILD-SNAPSHOT or 1.0.0-RELEASE
		// 1.0.0-SNAPSHOT or 1.0.0-RELEASE
		String[] splitByHyphen = version.split("\\-");
		int splitByHyphens = splitByHyphen.length;
		int numberOfHyphens = splitByHyphens - 1;
		int indexOfFirstHyphen = version.indexOf("-");
		boolean buildSnapshot = version.endsWith("BUILD-SNAPSHOT");
		if (numberOfHyphens == 1 && !buildSnapshot
				|| (numberOfHyphens > 1 && buildSnapshot)) {
			// Dysprosium or 1.0.0
			String versionName = version.substring(0, indexOfFirstHyphen);
			boolean hasDots = versionName.contains(".");
			// BUILD-SNAPSHOT
			String versionType = version.substring(indexOfFirstHyphen + 1);
			// Dysprosium-BUILD-SNAPSHOT
			if (splitByHyphens > 1 && !hasDots && validVersionType(version)) {
				return SplitVersion.hyphen(versionName, versionType);
			}
			// Dysprosium-RELEASE
			else if (splitByHyphens == 1 && !hasDots && validVersionType(version)) {
				return SplitVersion.hyphen(splitByHyphen[0], splitByHyphen[1]);
			}
			// 1.0.0-RELEASE or 1.0.0-BUILD-SNAPSHOT
			else if (splitByHyphens >= 1 && hasDots) {
				String[] newArray = combinedArrays(versionName, versionType);
				return SplitVersion.hyphen(newArray);
			}
			else {
				throw new UnsupportedOperationException(
						"Unknown version [" + version + "]");
			}
		}
		return null;
	}

	private boolean validVersionType(String version) {
		return VALID_PATTERNS.stream().anyMatch(p -> p.matcher(version).matches());
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
		return dotSeparatedReleaseTrainsAndVersions(this.version);
	}

	private SplitVersion dotSeparatedReleaseTrainsAndVersions(String version) {
		// Hoxton.BUILD-SNAPSHOT or 1.0.0.BUILD-SNAPSHOT
		String[] splitVersion = version.split("\\.");
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
			return bumpedVersion(splitVersion).withSnapshot().print();
		}
		return splitVersion.withSnapshot().print();
	}

	public String groupId() {
		return this.groupId;
	}

	public static boolean isValid(String version) {
		try {
			return new ProjectVersion(version).isValid();
		}
		catch (IllegalStateException e) {
			return false;
		}
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

	/**
	 * Compute the previous patch's version, without any prefix or suffix (ie
	 * MAJOR.MINOR.PATCH only). If the patch number is already at 0, returns an empty
	 * {@link Optional} instead.
	 * @param prefix the prefix to prepend to the tag pattern (eg. if tags are in the form
	 * vVERSION)
	 * @param suffix the suffix to append to the tag pattern instead (eg. if tags are in
	 * the form VERSION.RELEASE). this replaces the suffix in the version if any.
	 * @return an {@link Optional} valued with the previous MAJOR.MINOR.PATCH, or empty if
	 * PATCH is already 0
	 * @see #computePreviousMinorTagPattern(String,String)
	 */
	public Optional<String> computePreviousPatchTag(String prefix, String suffix) {
		SplitVersion splitVersion = this.assertVersion();
		if (suffix.isEmpty()) {
			suffix = splitVersion.suffix;
		}
		int patch;
		try {
			patch = Integer.parseInt(splitVersion.patch);
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Version " + this.version
					+ " doesn't contain a numerical PATCH number", nfe);
		}
		if (patch == 0) {
			return Optional.empty();
		}
		return Optional.of(prefix + splitVersion.major + splitVersion.delimiter
				+ splitVersion.minor + splitVersion.delimiter + (patch - 1)
				+ splitVersion.delimiter + suffix);
	}

	/**
	 * Compute a {@link Pattern} that allows to identify all the version tags of the
	 * previous MAJOR.MINOR (MAJOR.MINOR.*), provided MINOR is not 0. If MINOR is 0,
	 * {@link Optional#empty()} is returned.
	 * @param prefix the prefix to prepend to the tag pattern (eg. if tags are in the form
	 * vVERSION)
	 * @param suffix the suffix to append to the tag pattern (eg. if tags are in the form
	 * VERSION.RELEASE)
	 * @return a {@link Optional} of {@link Pattern} to identify all the versions in the
	 * previous MAJOR.MINOR, or empty if current MINOR is 0
	 * @see #computePreviousMajorTagPattern(String, String)
	 */
	public Optional<Pattern> computePreviousMinorTagPattern(String prefix,
			String suffix) {
		SplitVersion splitVersion = this.assertVersion();
		if (suffix.isEmpty()) {
			suffix = splitVersion.suffix;
		}
		String quotedSuffix = Pattern.quote(splitVersion.delimiter + suffix);
		int minor;
		try {
			minor = Integer.parseInt(splitVersion.minor);
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Version " + this.version
					+ " doesn't contain a numerical MINOR number", nfe);
		}
		if (minor == 0) {
			return Optional.empty();
		}
		Pattern p = Pattern
				.compile(Pattern
						.quote(prefix + splitVersion.major + splitVersion.delimiter
								+ (minor - 1) + splitVersion.delimiter)
						+ "\\d+" + quotedSuffix);
		return Optional.of(p);
	}

	/**
	 * Compute a {@link Pattern} that allows to identify all the version tags of the
	 * previous MAJOR. Throws {@link IllegalArgumentException} if MAJOR is 0.
	 * @param prefix the prefix to prepend to the tag pattern (eg. if tags are in the form
	 * vVERSION)
	 * @param suffix the suffix to append to the tag pattern (eg. if tags are in the form
	 * VERSION.RELEASE)
	 * @return a {@link Pattern} to identify all the versions in the previous MAJOR
	 */
	public Pattern computePreviousMajorTagPattern(String prefix, String suffix) {
		SplitVersion splitVersion = this.assertVersion();
		if (suffix.isEmpty()) {
			suffix = splitVersion.suffix;
		}
		String quotedSuffix = Pattern.quote(splitVersion.delimiter + suffix);
		int major;
		try {
			major = Integer.parseInt(splitVersion.major);
		}
		catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Version " + this.version
					+ " doesn't contain a numerical MAJOR number", nfe);
		}
		if (major == 0) {
			throw new IllegalArgumentException(
					"Cannot compute previous MAJOR pattern with MAJOR of 0");
		}
		return Pattern.compile(
				Pattern.quote(prefix + (major - 1) + splitVersion.delimiter) + "\\d+"
						+ Pattern.quote(splitVersion.delimiter) + "\\d+" + quotedSuffix);
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

	public boolean isReleaseTrain() {
		SplitVersion splitVersion = assertVersion();
		return splitVersion.isReleaseTrain();
	}

	public boolean isOldReleaseTrain() {
		SplitVersion splitVersion = assertVersion();
		return splitVersion.isOldReleaseTrain();
	}

	public boolean isReleaseOrServiceRelease() {
		return isRelease() || isServiceRelease();
	}

	public boolean isServiceRelease() {
		return this.version != null && this.version.matches(".*.SR[0-9]+");
	}

	private ReleaseType toReleaseType() {
		if (isMilestone()) {
			return ReleaseType.M;
		}
		else if (isRc()) {
			return ReleaseType.RC;
		}
		else if (isRelease()) {
			return ReleaseType.RELEASE;
		}
		else if (isServiceRelease()) {
			return ReleaseType.SR;
		}
		else if (isSnapshot()) {
			return ReleaseType.SNAPSHOT;
		}
		return releaseTypeForReleaseTrain();
	}

	/*
	 * 2020.0.0-SNAPSHOT 2020.0.0-M1 2020.0.0-M4 2020.0.0-RC1 (those are done already)
	 * 2020.0.0 -> this is RELEASE (no RELEASE suffix) 2020.0.1 -> this is SR1 (no SR1
	 * suffix)
	 */
	private ReleaseType releaseTypeForReleaseTrain() {
		try {
			SplitVersion splitVersion = assertVersion();
			if (StringUtils.isEmpty(splitVersion.suffix)
					&& StringUtils.hasText(splitVersion.patch)) {
				if ("0".equals(splitVersion.patch)) {
					return ReleaseType.RELEASE;
				}
				return ReleaseType.SR;
			}
		}
		catch (IllegalStateException ex) {
			return ReleaseType.SNAPSHOT;
		}
		return ReleaseType.SNAPSHOT;
	}

	/*
	 * E.g. 1.0.1.RELEASE is more mature than 1.0.2.RC2
	 */
	public boolean isMoreMature(ProjectVersion that) {
		return isMoreMatureComparison(that) > 0;
	}

	private int isMoreMatureComparison(ProjectVersion that) {
		SplitVersion thisSplit = assertVersion();
		SplitVersion thatSplit = that.assertVersion();
		int releaseTypeComparison = this.releaseType.compareTo(that.releaseType);
		boolean thisReleaseTypeHigher = releaseTypeComparison > 0;
		boolean bothGa = this.isReleaseOrServiceRelease()
				&& that.isReleaseOrServiceRelease();
		// 1.0.1.M2 vs 1.0.0.RELEASE (x)
		if (thisReleaseTypeHigher && !bothGa) {
			return 1;
		}
		int majorComparison = compare(thisSplit.major, thatSplit.major);
		if (majorComparison != 0) {
			return majorComparison;
		}
		int minorComparison = compare(thisSplit.minor, thatSplit.minor);
		if (minorComparison != 0) {
			return minorComparison;
		}
		int patchComparison = compare(thisSplit.patch, thatSplit.patch);
		if (patchComparison != 0) {
			return patchComparison;
		}
		return releaseTypeComparison;
	}

	private int compare(String thisString, String thatString) {
		try {
			return Integer.valueOf(thisString).compareTo(Integer.valueOf(thatString));
		}
		catch (NumberFormatException ex) {
			return thisString.compareTo(thatString);
		}
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
		SplitVersion thisVersion = assertVersion();
		SplitVersion thatVersion = assertVersion(version);
		if (thisVersion.isOldReleaseTrain() && thatVersion.isOldReleaseTrain()) {
			return thisVersion.major.compareToIgnoreCase(thatVersion.major) == 0;
		}
		else if (thisVersion.isOldReleaseTrain()) {
			return false;
		}
		return thisVersion.major.equals(thatVersion.major)
				&& thisVersion.minor.equals(thatVersion.minor);
	}

	private void assertVersionSet() {
		if (this.version == null) {
			throw new IllegalStateException("Version is not set");
		}
	}

	public int compareToReleaseTrain(String version) {
		return new TrainVersionNumber(this)
				.compareTo(new TrainVersionNumber(new ProjectVersion(version)));
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

	static final class SplitVersion {

		private static final String DOT = ".";

		private static final String HYPHEN = "-";

		private static final String SNAPSHOT_SUFFIX = "SNAPSHOT";

		private static final String LEGACY_SNAPSHOT_SUFFIX = "BUILD-SNAPSHOT";

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
			// Hoxton.SR4
			if (args.length == 2) {
				return new SplitVersion(args[0], "", "", delimiter, args[1]);
			}
			// 1.0.RELEASE
			// but not 1.0.3
			else if (args.length == 3 && notNumeric(args[2])) {
				return new SplitVersion(args[0], args[1], "", delimiter, args[2]);
			}
			return new SplitVersion(args, delimiter);
		}

		private void assertIfValid() {
			if (isInvalid()) {
				throw new IllegalStateException("Version [" + print()
						+ "] is invalid. Should be of format [1.2.3.A] / [1.2.3-A] or [A.B] / [A-B]");
			}
		}

		private boolean isInvalid() {
			return wrongDelimiter() || tooFewElements();
		}

		// Hoxton.BUILD-SNAPSHOT or Hoxton-BUILD-SNAPSHOT or 2020.x.x
		private boolean isReleaseTrain() {
			return isOldReleaseTrain() || calverReleaseTrain();
		}

		private boolean calverReleaseTrain() {
			return Integer.parseInt(this.major) >= 2020;
		}

		private boolean isOldReleaseTrain() {
			return notNumeric(this.major);
		}

		private SplitVersion fullVersionWithIncrementedPatch() {
			int incrementedPatch = Integer.parseInt(patch) + 1;
			return new SplitVersion(major, minor, Integer.toString(incrementedPatch),
					delimiter, suffix);
		}

		String print() {
			// Finchley.SR2
			if (StringUtils.isEmpty(minor)) {
				return StringUtils.isEmpty(suffix) ? major
						: String.format("%s%s%s", major, delimiter, suffix);
			}
			else if (StringUtils.isEmpty(suffix)) {
				return String.format("%s.%s.%s", major, minor, patch);
			}
			return String.format("%s.%s.%s%s%s", major, minor, patch, delimiter, suffix);
		}

		private static boolean notNumeric(String string) {
			return !string.matches("[0-9]+");
		}

		private boolean wrongDelimiter() {
			return !(DOT.equals(this.delimiter) || HYPHEN.equals(this.delimiter));
		}

		private boolean tooFewElements() {
			// 1 is wrong but Finchley-SR3 is ok
			return StringUtils.isEmpty(minor) && StringUtils.isEmpty(patch)
					&& StringUtils.isEmpty(suffix);
		}

		private SplitVersion withSnapshot() {
			return new SplitVersion(major, minor, patch, delimiter, suffix());
		}

		private String suffix() {
			if (suffix.endsWith(LEGACY_SNAPSHOT_SUFFIX)) {
				return LEGACY_SNAPSHOT_SUFFIX;
			}
			return SNAPSHOT_SUFFIX;
		}

	}

	private static class TrainVersionNumber implements Comparable<TrainVersionNumber> {

		private final ProjectVersion version;

		TrainVersionNumber(ProjectVersion version) {
			this.version = version;
		}

		@Override
		public int compareTo(TrainVersionNumber o) {
			ProjectVersion thisVersion = this.version;
			ProjectVersion thatVersion = o.version;
			// 2020.0.0 vs ""
			if (!thatVersion.isValid() && thisVersion.isValid()) {
				return 1;
				// "" vs 2020.0.0
			}
			else if (!thisVersion.isValid() && thatVersion.isValid()) {
				return -1;
			}
			boolean thisOldTrain = isOldReleaseTrain(thisVersion);
			boolean thatOldTrain = isOldReleaseTrain(thatVersion);
			if (thisOldTrain && thatOldTrain) {
				return letterBasedReleaseTrainComparison(o);
			}
			else if (thisOldTrain) {
				return compareWithOldTrain(thisVersion, thatVersion);
			}
			else if (thatOldTrain) {
				return -1 * compareWithOldTrain(thisVersion, thatVersion);
			}
			// new train comparison
			return thisVersion.compareTo(thatVersion);
		}

		private boolean isOldReleaseTrain(ProjectVersion projectVersion) {
			try {
				return projectVersion.isOldReleaseTrain();
			}
			catch (IllegalStateException ex) {
				return false;
			}
		}

		private int compareWithOldTrain(ProjectVersion oldTrain,
				ProjectVersion thatVersion) {
			// Hoxton.SR5 > 2020.0.0-M5
			if (oldTrain.isReleaseOrServiceRelease()
					&& !thatVersion.isReleaseOrServiceRelease()) {
				return 1;
			}
			// Hoxton.SR5 < 2020.0.0-RELEASE
			// Hoxton.M5 < 2020.0.0-M4
			return -1;
		}

		private int letterBasedReleaseTrainComparison(TrainVersionNumber o) {
			String thatName = o.version.major();
			String thisName = this.version.major();
			int nameComparison = thisName.compareTo(thatName);
			if (nameComparison != 0) {
				return nameComparison;
			}
			String thisSuffix = this.version.assertVersion().suffix;
			String thisValue = thisSuffix;
			String thisLower = thisValue.toLowerCase();
			char thisFirst = thisLower.isEmpty() ? ' ' : thisLower.charAt(0);
			String thatSuffix = o.version.assertVersion().suffix;
			String thatLower = thatSuffix.toLowerCase();
			char thatFirst = thatLower.isEmpty() ? ' ' : thatLower.charAt(0);
			// B < M < RC < R < S
			int charComparison = Character.compare(thisFirst, thatFirst);
			if (charComparison != 0) {
				return charComparison;
			}
			String thisVersion = this.version.toString().replaceAll("\\D+", "");
			boolean thisVersionEmpty = StringUtils.isEmpty(thisVersion);
			String thatVersion = o.version.toString().replaceAll("\\D+", "");
			boolean thatVersionEmpty = StringUtils.isEmpty(thatVersion);
			if (thisVersionEmpty || thatVersionEmpty) {
				return thisVersion.compareTo(thatVersion);
			}
			Integer thisNumber = Integer.valueOf(thisVersion);
			Integer thatNumber = Integer.valueOf(thatVersion);
			return thisNumber.compareTo(thatNumber);
		}

	}

}
