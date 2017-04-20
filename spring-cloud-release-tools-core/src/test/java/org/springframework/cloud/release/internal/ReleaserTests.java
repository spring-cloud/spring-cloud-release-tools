package org.springframework.cloud.release.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.cloud.release.internal.git.ProjectGitUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.project.ProjectBuilder;

import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ReleaserTests {

	@Mock ProjectPomUpdater projectPomUpdater;
	@Mock ProjectBuilder projectBuilder;
	@Mock ProjectGitUpdater projectGitUpdater;
	@Mock TemplateGenerator templateGenerator;
	@InjectMocks Releaser releaser;
	File pom;

	@Before
	public void setup() throws URISyntaxException {
		URI pomUri = ReleaserTests.class.getResource("/projects/project/pom.xml").toURI();
		this.pom = new File(pomUri);
	}

	@Test
	public void should_not_bump_versions_for_original_release_project() throws Exception {
		this.releaser.rollbackReleaseVersion(this.pom,
				new ProjectVersion("original", "1.0.0.RELEASE"),
				new ProjectVersion("changed", "1.0.0.RELEASE"));

		then(this.projectBuilder).should(never()).bumpVersions(anyString());
	}

	@Test
	public void should_not_bump_versions_for_original_snapshot_project_and_current_snapshot() throws Exception {
		this.releaser.rollbackReleaseVersion(this.pom,
				new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("changed", "1.0.0.BUILD-SNAPSHOT"));

		then(this.projectBuilder).should(never()).bumpVersions(anyString());
	}

	@Test
	public void should_bump_versions_for_original_snapshot_project() throws Exception {
		this.releaser.rollbackReleaseVersion(this.pom,
				new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("changed", "1.0.0.RELEASE"));

		then(this.projectBuilder).should().bumpVersions(anyString());
	}

	@Test
	public void should_not_generate_email_for_snapshot_version() throws Exception {
		this.releaser.createEmail(new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"));

		then(this.templateGenerator).should(never()).email();
	}

	@Test
	public void should_generate_email_for_release_version() throws Exception {
		this.releaser.createEmail(new ProjectVersion("original", "1.0.0.RELEASE"));

		then(this.templateGenerator).should().email();
	}

	@Test
	public void should_not_close_milestone_for_snapshots() throws Exception {
		this.releaser.closeMilestone(new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"));

		then(this.projectGitUpdater).should(never()).closeMilestone(any(ProjectVersion.class));
	}

	@Test
	public void should_not_rollback_for_snapshots() throws Exception {
		this.releaser.rollbackReleaseVersion(null, null, new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"));

		then(this.projectGitUpdater).should(never()).revertChangesIfApplicable(any(File.class), any(ProjectVersion.class));
	}

}