package org.springframework.cloud.release.internal.sagan;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class SaganUpdaterTest {

	@Mock SaganClient saganClient;
	@InjectMocks SaganUpdater saganUpdater;

	@Before
	public void setup() {
		Project project = new Project();
		project.projectReleases.addAll(Arrays.asList(
				release("1.0.0.RC1"),
				release("1.1.0.BUILD-SNAPSHOT"),
				release("2.0.0.M4"))
		);
		BDDMockito.given(this.saganClient.getProject(anyString()))
				.willReturn(project);
	}

	private Release release(String version) {
		Release release = new Release();
		release.version = version;
		release.current = true;
		return release;
	}

	@Test public void should_update_sagan_for_milestone() throws Exception {
		this.saganUpdater.updateSagan("master", version("1.0.0.M1"), version("1.0.0.M1"));

		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.M1",
						"http://cloud.spring.io/spring-cloud-static/foo/{version}/", "PRERELEASE")));
	}

	@Test public void should_update_sagan_for_rc() throws Exception {
		this.saganUpdater.updateSagan("master", version("1.0.0.RC1"), version("1.0.0.RC1"));

		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.RC1",
						"http://cloud.spring.io/spring-cloud-static/foo/{version}/", "PRERELEASE")));
	}

	private ProjectVersion version(String version) {
		return new ProjectVersion("foo", version);
	}

	@Test public void should_update_sagan_from_master() throws Exception {
		ProjectVersion projectVersion = version("1.0.0.BUILD-SNAPSHOT");

		this.saganUpdater.updateSagan("master", projectVersion, projectVersion);

		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.BUILD-SNAPSHOT",
						"http://cloud.spring.io/foo/foo.html", "SNAPSHOT")));
	}

	@Test public void should_update_sagan_from_release_version() throws Exception {
		ProjectVersion projectVersion = version("1.0.0.RELEASE");

		this.saganUpdater.updateSagan("master", projectVersion, projectVersion);

		then(this.saganClient).should().deleteRelease("foo", "1.0.0.RC1");
		then(this.saganClient).should().deleteRelease("foo", "1.0.0.BUILD-SNAPSHOT");
		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.RELEASE",
						"http://cloud.spring.io/spring-cloud-static/foo/{version}/", "GENERAL_AVAILABILITY")));
		then(this.saganClient).should().deleteRelease("foo", "1.0.0.BUILD-SNAPSHOT");
		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.1.BUILD-SNAPSHOT",
						"http://cloud.spring.io/foo/foo.html", "SNAPSHOT")));
	}

	@Test public void should_update_sagan_from_non_master() throws Exception {
		ProjectVersion projectVersion = version("1.1.0.BUILD-SNAPSHOT");

		this.saganUpdater.updateSagan("1.1.x", projectVersion, projectVersion);

		then(this.saganClient).should(never()).deleteRelease(anyString(), anyString());
		then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.1.0.BUILD-SNAPSHOT",
						"http://cloud.spring.io/foo/1.1.x/", "SNAPSHOT")));
	}

	private ArgumentMatcher<List<ReleaseUpdate>> withReleaseUpdate(final String version,
			final String refDocUrl, final String releaseStatus) {
		return argument ->  {
			ReleaseUpdate item = argument.get(0);
				return "foo".equals(item.artifactId) &&
						releaseStatus.equals(item.releaseStatus) &&
						version.equals(item.version) &&
						refDocUrl.equals(item.apiDocUrl) &&
						refDocUrl.equals(item.refDocUrl) &&
						item.current;
			};
	}

}