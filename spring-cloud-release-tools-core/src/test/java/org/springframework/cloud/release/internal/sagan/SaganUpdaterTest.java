package org.springframework.cloud.release.internal.sagan;

import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class SaganUpdaterTest {

	@Mock SaganClient saganClient;
	@InjectMocks SaganUpdater saganUpdater;

	@Test public void should_update_sagan_for_milestone() throws Exception {
		this.saganUpdater.updateSagan("master", version("1.0.0.M1"), version("1.0.0.M1"));

		BDDMockito.then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.M1",
						"http://cloud.spring.io/foo/foo.html", "PRERELEASE")));
	}

	@Test public void should_update_sagan_for_rc() throws Exception {
		this.saganUpdater.updateSagan("master", version("1.0.0.RC1"), version("1.0.0.RC1"));

		BDDMockito.then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.RC1",
						"http://cloud.spring.io/foo/foo.html", "PRERELEASE")));
	}

	private ProjectVersion version(String version) {
		return new ProjectVersion("foo", version);
	}

	@Test public void should_update_sagan_from_master() throws Exception {
		ProjectVersion projectVersion = version("1.0.0.BUILD-SNAPSHOT");

		this.saganUpdater.updateSagan("master", projectVersion, projectVersion);

		BDDMockito.then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.BUILD-SNAPSHOT",
						"http://cloud.spring.io/foo/foo.html", "SNAPSHOT")));
	}

	@Test public void should_update_sagan_from_release_version() throws Exception {
		ProjectVersion projectVersion = version("1.0.0.RELEASE");

		this.saganUpdater.updateSagan("master", projectVersion, projectVersion);

		BDDMockito.then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.0.0.RELEASE",
						"http://cloud.spring.io/spring-cloud-static/foo/{version}/", "GENERAL_AVAILABILITY")));
	}

	@Test public void should_update_sagan_from_non_master() throws Exception {
		ProjectVersion projectVersion = version("1.1.0.BUILD-SNAPSHOT");

		this.saganUpdater.updateSagan("1.1.x", projectVersion, projectVersion);

		BDDMockito.then(this.saganClient).should().updateRelease(BDDMockito.eq("foo"),
				BDDMockito.argThat(withReleaseUpdate("1.1.0.BUILD-SNAPSHOT",
						"http://cloud.spring.io/foo/1.1.x/", "SNAPSHOT")));
	}

	private TypeSafeMatcher<List<ReleaseUpdate>> withReleaseUpdate(final String version,
			final String refDocUrl, final String releaseStatus) {
		return new TypeSafeMatcher<List<ReleaseUpdate>>() {
			@Override protected boolean matchesSafely(List<ReleaseUpdate> items) {
				ReleaseUpdate item = items.get(0);
				return "foo".equals(item.artifactId) &&
						releaseStatus.equals(item.releaseStatus) &&
						version.equals(item.version) &&
						refDocUrl.equals(item.apiDocUrl) &&
						refDocUrl.equals(item.refDocUrl);
			}

			@Override public void describeTo(Description description) {

			}
		};
	}

}