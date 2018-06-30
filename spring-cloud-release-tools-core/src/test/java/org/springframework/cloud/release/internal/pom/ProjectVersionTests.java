package org.springframework.cloud.release.internal.pom;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.release.internal.git.GitRepoTests;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectVersionTests {

	File springCloudReleaseProject;
	File springCloudContract;

	@Before
	public void setup() throws URISyntaxException {
		URI scRelease = GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI();
		URI scContract = GitRepoTests.class.getResource("/projects/spring-cloud-contract").toURI();
		this.springCloudReleaseProject = new File(scRelease.getPath(), "pom.xml");
		this.springCloudContract = new File(scContract.getPath(), "pom.xml");
	}

	@Test
	public void should_build_version_from_text_when_parent_suffix_is_present() {
		ProjectVersion projectVersion = new ProjectVersion("foo-parent", "1.0.0");

		then(projectVersion.version).isEqualTo("1.0.0");
		then(projectVersion.projectName).isEqualTo("foo");
	}

	@Test
	public void should_build_version_from_text() {
		ProjectVersion projectVersion = new ProjectVersion("foo", "1.0.0");

		then(projectVersion.version).isEqualTo("1.0.0");
		then(projectVersion.projectName).isEqualTo("foo");
	}

	@Test
	public void should_build_version_from_file() {
		ProjectVersion projectVersion = new ProjectVersion(this.springCloudReleaseProject);

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
		then(projectVersion("1.0.1.BUILD-SNAPSHOT").isReleaseOrServiceRelease()).isFalse();
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
	public void should_return_empty_group_id_when_it_is_missing() {
		ProjectVersion projectVersion = projectVersion("1.0.0.RC1");

		then(projectVersion.groupId()).isEmpty();
	}

	private ProjectVersion projectVersion(String version) {
		return new ProjectVersion("foo", version);
	}
}