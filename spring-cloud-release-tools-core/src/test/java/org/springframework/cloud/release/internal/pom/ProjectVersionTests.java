package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.release.internal.git.GitRepoTests;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectVersionTests {

	File springCloudReleaseProject;

	@Before
	public void setup() throws URISyntaxException {
		URI scRelease = GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI();
		this.springCloudReleaseProject = new File(scRelease.getPath(), "pom.xml");
	}

	@Test
	public void should_build_version_from_file() {
		ProjectVersion projectVersion = new ProjectVersion(this.springCloudReleaseProject);

		then(projectVersion.version).isEqualTo("Dalston.BUILD-SNAPSHOT");
	}

	@Test
	public void should_throw_exception_if_version_is_not_long_enough() {
		String version = "1.0";

		thenThrownBy(() -> new ProjectVersion(version).bumpedVersion())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Version is invalid");
	}

	@Test
	public void should_bump_version_by_patch_version() {
		String version = "1.0.1.BUILD-SNAPSHOT";

		then(new ProjectVersion(version).bumpedVersion()).isEqualTo("1.0.2.BUILD-SNAPSHOT");
	}

	@Test
	public void should_return_true_for_snapshot_version() {
		String version = "1.0.1.BUILD-SNAPSHOT";

		then(new ProjectVersion(version).isSnapshot()).isTrue();
	}

	@Test
	public void should_return_false_for_snapshot_version() {
		String version = "1.0.1.RELEASE";

		then(new ProjectVersion(version).isSnapshot()).isFalse();
	}

}