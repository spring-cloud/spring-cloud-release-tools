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

package org.springframework.cloud.release.internal.buildsystem;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.release.internal.git.GitRepoTests;
import org.springframework.cloud.release.internal.project.ProjectVersion;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectVersionTests {

	File springCloudReleaseProject;

	File springCloudContract;

	@Before
	public void setup() throws URISyntaxException {
		URI scRelease = GitRepoTests.class.getResource("/projects/spring-cloud-release")
				.toURI();
		URI scContract = GitRepoTests.class.getResource("/projects/spring-cloud-contract")
				.toURI();
		this.springCloudReleaseProject = new File(scRelease.getPath(), "pom.xml");
		this.springCloudContract = new File(scContract.getPath(), "pom.xml");
	}

	@Test
	public void should_build_version_from_text() {
		ProjectVersion projectVersion = new ProjectVersion("foo", "1.0.0");

		then(projectVersion.version).isEqualTo("1.0.0");
		then(projectVersion.projectName).isEqualTo("foo");
	}

	@Test
	public void should_build_version_from_file() {
		ProjectVersion projectVersion = new ProjectVersion(
				this.springCloudReleaseProject);

		then(projectVersion.version).isEqualTo("Dalston.BUILD-SNAPSHOT");
		then(projectVersion.projectName).isEqualTo("spring-cloud-starter-build");
	}

	@Test
	public void should_build_version_from_file_when_parent_suffix_is_present() {
		ProjectVersion projectVersion = new ProjectVersion(this.springCloudContract);

		then(projectVersion.version).isEqualTo("1.1.0.BUILD-SNAPSHOT");
		then(projectVersion.projectName).isEqualTo("spring-cloud-contract");
	}

	@Test
	public void should_return_group_id_when_it_is_present() {
		ProjectVersion projectVersion = new ProjectVersion(this.springCloudContract);

		then(projectVersion.groupId()).isEqualTo("org.springframework.cloud");
	}

	@Test
	public void should_throw_exception_if_version_is_not_long_enough() {
		String version = "1.0";

		thenThrownBy(() -> projectVersion(version).bumpedVersion())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Version is invalid");
	}

	@Test
	public void should_bump_version_by_patch_version() {
		String version = "1.0.1.BUILD-SNAPSHOT";

		then(projectVersion(version).bumpedVersion()).isEqualTo("1.0.2.BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_the_previous_version_for_release_train_version() {
		String version = "Edgware.BUILD-SNAPSHOT";

		then(projectVersion(version).bumpedVersion()).isEqualTo("Edgware.BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_true_for_a_valid_version() {
		then(projectVersion("1.0.1.BUILD-SNAPSHOT").isValid()).isTrue();
		then(projectVersion("1.0.3.RC1").isValid()).isTrue();
		then(projectVersion("1.0.4.M1").isValid()).isTrue();
		then(projectVersion("Finchley.BUILD-SNAPSHOT").isValid()).isTrue();
		then(projectVersion("Finchley.SR1").isValid()).isTrue();
	}

	@Test
	public void should_return_false_for_an_invalid_version() {
		then(projectVersion("1").isValid()).isFalse();
		then(projectVersion("1.").isValid()).isFalse();
		then(projectVersion("1.0.4.").isValid()).isFalse();
		then(projectVersion("Some random text").isValid()).isFalse();
	}

	@Test
	public void should_throw_exception_if_version_is_not_long_enough_when_bumping_snapshots() {
		String version = "1.0";

		thenThrownBy(() -> projectVersion(version).postReleaseSnapshotVersion())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Version is invalid");
	}

	@Test
	public void should_throw_exception_when_trying_to_get_major_from_invalid_version() {
		String version = "1.0";

		thenThrownBy(() -> projectVersion(version).major())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Version is invalid");
	}

	@Test
	public void should_get_major_from_version() {
		then(projectVersion("2.0.1.BUILD-SNAPSHOT").major()).isEqualTo("2");
		then(projectVersion("2.0.1.M1").major()).isEqualTo("2");
		then(projectVersion("2.0.1.RC1").major()).isEqualTo("2");
		then(projectVersion("Finchley.SR1").major()).isEqualTo("Finchley");
	}

	@Test
	public void should_not_bump_version_by_patch_version_when_non_ga_or_sr() {
		then(projectVersion("1.0.1.BUILD-SNAPSHOT").postReleaseSnapshotVersion())
				.isEqualTo("1.0.1.BUILD-SNAPSHOT");
		then(projectVersion("1.0.1.M1").postReleaseSnapshotVersion())
				.isEqualTo("1.0.1.BUILD-SNAPSHOT");
		then(projectVersion("1.0.1.RC1").postReleaseSnapshotVersion())
				.isEqualTo("1.0.1.BUILD-SNAPSHOT");
		then(projectVersion("Finchley.SR1").postReleaseSnapshotVersion())
				.isEqualTo("Finchley.BUILD-SNAPSHOT");
	}

	@Test
	public void should_not_bump_version_by_patch_version_when_non_ga_or_sr_with_hyphen() {
		then(projectVersion("1.0.1-BUILD-SNAPSHOT").postReleaseSnapshotVersion())
				.isEqualTo("1.0.1-BUILD-SNAPSHOT");
		then(projectVersion("1.0.1-M1").postReleaseSnapshotVersion())
				.isEqualTo("1.0.1-BUILD-SNAPSHOT");
		then(projectVersion("1.0.1-RC1").postReleaseSnapshotVersion())
				.isEqualTo("1.0.1-BUILD-SNAPSHOT");
		then(projectVersion("Finchley-SR1").postReleaseSnapshotVersion())
				.isEqualTo("Finchley-BUILD-SNAPSHOT");
	}

	@Test
	public void should_bump_version_by_patch_version_when_bumping_snapshots_for_ga() {
		then(projectVersion("1.0.1.RELEASE").postReleaseSnapshotVersion())
				.isEqualTo("1.0.2.BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_the_previous_version_for_release_train_version_when_bumping_snapshots() {
		String version = "Edgware.BUILD-SNAPSHOT";

		then(projectVersion(version).postReleaseSnapshotVersion())
				.isEqualTo("Edgware.BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_the_previous_version_for_hyphen_release_train_version_when_bumping_snapshots() {
		String version = "Edgware-BUILD-SNAPSHOT";

		then(projectVersion(version).postReleaseSnapshotVersion())
				.isEqualTo("Edgware-BUILD-SNAPSHOT");
	}

	@Test
	public void should_bump_version_by_patch_version_when_bumping_releases() {
		String version = "1.0.1.RELEASE";

		then(projectVersion(version).postReleaseSnapshotVersion())
				.isEqualTo("1.0.2.BUILD-SNAPSHOT");
	}

	@Test
	public void should_bump_version_by_patch_version_when_bumping_releases_with_hyphen() {
		String version = "1.0.1-RELEASE";

		then(projectVersion(version).postReleaseSnapshotVersion())
				.isEqualTo("1.0.2-BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_the_previous_version_for_release_train_version_when_bumping_releases() {
		String version = "Edgware.RELEASE";

		then(projectVersion(version).postReleaseSnapshotVersion())
				.isEqualTo("Edgware.BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_true_for_snapshot_version() {
		String version = "1.0.1.BUILD-SNAPSHOT";

		then(projectVersion(version).isSnapshot()).isTrue();
	}

	@Test
	public void should_return_false_for_snapshot_version() {
		String version = "1.0.1.RELEASE";

		then(projectVersion(version).isSnapshot()).isFalse();
	}

	@Test
	public void should_return_false_for_milestone_version() {
		String version = "1.0.1.M1";

		then(projectVersion(version).isRelease()).isFalse();
	}

	@Test
	public void should_return_false_for_rc_version() {
		String version = "1.0.1.RC1";

		then(projectVersion(version).isRelease()).isFalse();
	}

	@Test
	public void should_return_true_for_release_versions() {
		String version = "1.0.1.RELEASE";

		then(projectVersion(version).isRelease()).isTrue();
	}

	@Test
	public void should_return_true_for_service_release_versions() {
		String version = "1.0.1.SR1";

		then(projectVersion(version).isServiceRelease()).isTrue();
	}

	@Test
	public void should_return_true_when_checking_milestone_version_against_milestone() {
		String version = "1.0.1.M1";

		then(projectVersion(version).isMilestone()).isTrue();
	}

	@Test
	public void should_return_false_when_checking_milestone_version_against_non_milestone() {
		String version = "1.0.1.RC1";

		then(projectVersion(version).isMilestone()).isFalse();
	}

	@Test
	public void should_return_true_when_checking_rc_version_against_rc() {
		String version = "1.0.1.RC3";

		then(projectVersion(version).isRc()).isTrue();
	}

	@Test
	public void should_return_true_when_checking_ga_version_against_ga() {
		then(projectVersion("1.0.1.RELEASE").isReleaseOrServiceRelease()).isTrue();
		then(projectVersion("1.0.1.SR1").isReleaseOrServiceRelease()).isTrue();
	}

	@Test
	public void should_return_False_when_checking_ga_version_against_non_ga() {
		then(projectVersion("1.0.1.BUILD-SNAPSHOT").isReleaseOrServiceRelease())
				.isFalse();
		then(projectVersion("1.0.1.M4").isReleaseOrServiceRelease()).isFalse();
		then(projectVersion("1.0.1.RC4").isReleaseOrServiceRelease()).isFalse();
	}

	@Test
	public void should_return_false_when_checking_rc_version_against_non_rc() {
		String version = "1.0.1.M1";

		then(projectVersion(version).isRc()).isFalse();
	}

	@Test
	public void should_return_true_when_versions_are_from_same_minor() {
		String thisVersion = "1.3.1.RC3";
		String thatVersion = "1.3.2.SR3";

		then(projectVersion(thisVersion).isSameMinor(thatVersion)).isTrue();
	}

	@Test
	public void should_return_false_when_versions_of_different_sizes() {
		String thisVersion = "1.3.1.RC3";
		String thatVersion = "1.3.RC3";

		then(projectVersion(thisVersion).isSameMinor(thatVersion)).isFalse();
	}

	@Test
	public void should_return_false_when_versions_not_of_same_minor() {
		String thisVersion = "1.3.1.RC3";
		String thatVersion = "1.4.2.RC3";

		then(projectVersion(thisVersion).isSameMinor(thatVersion)).isFalse();
	}

	@Test
	public void should_return_equal_when_versions_are_the_same() {
		String thisVersion = "1.3.1.SR3";
		String thatVersion = "1.3.1.SR3";

		then(projectVersion(thisVersion).compareTo(projectVersion(thatVersion))).isZero();
	}

	@Test
	public void should_return_greater_when_versions_this_version_is_greater_than_the_other() {
		String thisVersion = "1.3.2.SR3";
		String thatVersion = "1.3.1.SR3";

		then(projectVersion(thisVersion).compareTo(projectVersion(thatVersion)))
				.isPositive();
	}

	@Test
	public void should_return_lower_when_versions_this_version_is_lower_than_the_other() {
		String thisVersion = "1.3.0.RC3";
		String thatVersion = "1.3.1.RC3";

		then(projectVersion(thisVersion).compareTo(projectVersion(thatVersion)))
				.isNegative();
	}

	@Test
	public void should_return_empty_group_id_when_it_is_missing() {
		ProjectVersion projectVersion = projectVersion("1.0.0.RC1");

		then(projectVersion.groupId()).isEmpty();
	}

	@Test
	public void should_return_true_when_release_train_names_are_the_same() {
		String thisVersion = "Finchley.SR1";
		String thatVersion = "Finchley.SR2";

		then(projectVersion(thisVersion).isSameReleaseTrainName(thatVersion)).isTrue();
	}

	@Test
	public void should_return_true_when_sr_has_two_digits() {
		String thisVersion = "Finchley.SR1";
		String thatVersion = "Finchley.SR10";

		then(projectVersion(thatVersion).compareToReleaseTrainName(thisVersion))
				.isPositive();
	}

	@Test
	public void should_return_false_when_release_train_name_are_different() {
		String thisVersion = "Finchley.SR1";
		String thatVersion = "Greenwich.SR1";

		then(projectVersion(thisVersion).isSameReleaseTrainName(thatVersion)).isFalse();
	}

	@Test
	public void should_return_positive_when_release_train_is_greater_than_the_other_one() {
		String thisVersion = "Finchley.SR2";
		String thatVersion = "Finchley.SR1";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion))
				.isPositive();

		thisVersion = "Greenwich.SR1";
		thatVersion = "Finchley.SR2";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion))
				.isPositive();
	}

	@Test
	public void should_return_0_when_release_train_is_equal_than_the_other_one() {
		String thisVersion = "Finchley.SR1";
		String thatVersion = "Finchley.SR1";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion)).isZero();
	}

	@Test
	public void should_return_minus_1_when_release_train_is_smaller_than_the_other_one() {
		String thisVersion = "Finchley.SR1";
		String thatVersion = "Finchley.SR2";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion))
				.isNegative();

		thisVersion = "Finchley.SR2";
		thatVersion = "Greenwich.SR1";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion))
				.isNegative();
	}

	@Test
	public void should_return_minus_1_when_this_train_is_empty() {
		String thisVersion = "";
		String thatVersion = "Finchley.SR2";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion))
				.isNegative();

		thisVersion = "";
		thatVersion = "";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion)).isZero();
	}

	@Test
	public void should_return_plus_1_when_that_train_is_empty() {
		String thisVersion = "Finchley.SR1";
		String thatVersion = "";

		then(projectVersion(thisVersion).compareToReleaseTrainName(thatVersion))
				.isPositive();
	}

	@Test
	public void should_return_no_unacceptable_patterns_for_a_snapshot_version() {
		then(projectVersion("1.0.0.BUILD-SNAPSHOT").unacceptableVersionPatterns())
				.isEmpty();
		then(projectVersion("1.0.0.SNAPSHOT").unacceptableVersionPatterns()).isEmpty();
	}

	@Test
	public void should_return_snapshot_unacceptable_patterns_for_a_milestone_or_rc_version() {
		List<Pattern> milestonePatterns = projectVersion("1.0.0.M1")
				.unacceptableVersionPatterns();
		then(milestonePatterns).isNotEmpty();
		then(milestonePatterns.get(0).pattern()).contains("SNAPSHOT");

		List<Pattern> rcPatterns = projectVersion("1.0.0.RC1")
				.unacceptableVersionPatterns();
		then(rcPatterns).isNotEmpty();
		then(rcPatterns.get(0).pattern()).contains("SNAPSHOT");

		List<Pattern> milestonePatternsWithASlash = projectVersion("1.0.0-M1")
				.unacceptableVersionPatterns();
		then(milestonePatterns).isNotEmpty();
		then(milestonePatterns.get(0).pattern()).contains("SNAPSHOT");

		List<Pattern> rcPatternsWithASlash = projectVersion("1.0.0-RC1")
				.unacceptableVersionPatterns();
		then(rcPatterns).isNotEmpty();
		then(rcPatterns.get(0).pattern()).contains("SNAPSHOT");
	}

	@Test
	public void should_return_snapshot_milestone_rc_unacceptable_patterns_for_a_ga_or_sr_version() {
		List<Pattern> gaPatterns = projectVersion("1.0.0.RELEASE")
				.unacceptableVersionPatterns();
		thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(gaPatterns);

		gaPatterns = projectVersion("1.0.0").unacceptableVersionPatterns();
		thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(gaPatterns);

		gaPatterns = projectVersion("1.0.0-RELEASE").unacceptableVersionPatterns();
		thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(gaPatterns);

		List<Pattern> srPatterns = projectVersion("1.0.0.SR1")
				.unacceptableVersionPatterns();
		thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(srPatterns);

		List<Pattern> srPatternsWithASlash = projectVersion("1.0.0-SR1")
				.unacceptableVersionPatterns();
		thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(srPatterns);

		List<Pattern> unknownTypeOfVersion = projectVersion("1.0.0.SOMETHING")
				.unacceptableVersionPatterns();
		thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(
				unknownTypeOfVersion);
	}

	@Test
	public void should_return_v100RELEASE_when_tag_name_is_requested() {
		then(projectVersion("1.0.0.RELEASE").releaseTagName())
				.isEqualTo("v1.0.0.RELEASE");
	}

	@Test
	public void should_return_empty_when_tag_name_is_non_ga() {
		then(projectVersion("1.0.0.BUILD-SNAPSHOT").releaseTagName()).isEmpty();
	}

	private void thenPatternsForSnapshotMilestoneAndReleaseCandidateArePresent(
			List<Pattern> unknownTypeOfVersion) {
		then(unknownTypeOfVersion).isNotEmpty();
		then(unknownTypeOfVersion.get(0).pattern()).contains("SNAPSHOT");
		then(unknownTypeOfVersion.get(1).pattern()).contains("M[0-9]");
		then(unknownTypeOfVersion.get(2).pattern()).contains("RC");

		then(unknownTypeOfVersion.get(0).matcher("SomeName-BUILD-SNAPSHOT").matches())
				.isTrue();
		then(unknownTypeOfVersion.get(0).matcher("SomeName.BUILD-SNAPSHOT").matches())
				.isTrue();

		then(unknownTypeOfVersion.get(1).matcher("SomeName-M3").matches()).isTrue();
		then(unknownTypeOfVersion.get(1).matcher("SomeName.M3").matches()).isTrue();

		then(unknownTypeOfVersion.get(2).matcher("SomeName-RC3").matches()).isTrue();
		then(unknownTypeOfVersion.get(2).matcher("SomeName.RC3").matches()).isTrue();
	}

	private ProjectVersion projectVersion(String version) {
		return new ProjectVersion("foo", version);
	}

}
