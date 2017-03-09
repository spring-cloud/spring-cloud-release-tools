package org.springframework.cloud.release.internal.git;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectGitUpdaterTests {

	@Mock GitRepo gitRepo;
	ProjectGitUpdater updater = new ProjectGitUpdater() {
		@Override GitRepo gitRepo(File workingDir) {
			return ProjectGitUpdaterTests.this.gitRepo;
		}
	};
	File file = new File("");

	@Test
	public void should_only_commit_without_pushing_changes_when_version_is_snapshot() {
		this.updater.commitAndTagIfApplicable(file, new ProjectVersion("1.0.0.BUILD-SNAPSHOT"));

		then(this.gitRepo).should().commit(any(File.class), anyString());
		then(this.gitRepo).should(never()).tag(any(File.class), anyString());
	}

	@Test
	public void should_commit_tag_and_push_tag_when_version_is_not_snapshot() {
		this.updater.commitAndTagIfApplicable(file, new ProjectVersion("1.0.0.RELEASE"));

		then(this.gitRepo).should().commit(any(File.class), anyString());
		then(this.gitRepo).should().tag(any(File.class), eq("v1.0.0.RELEASE"));
		then(this.gitRepo).should().pushTag(any(File.class), eq("v1.0.0.RELEASE"));
	}
}