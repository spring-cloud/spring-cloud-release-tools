package org.springframework.cloud.release.internal.git;

import java.io.File;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.sagan.Release;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectGitHandlerTests {

	@Mock GitRepo gitRepo;
	ReleaserProperties properties = new ReleaserProperties();
	ProjectGitHandler updater = new ProjectGitHandler(this.properties) {
		@Override GitRepo gitRepo(File workingDir) {
			return ProjectGitHandlerTests.this.gitRepo;
		}

		@Override File cloneProject(String url) {
			return new File(".");
		}
	};
	File file = new File("");

	@Test
	public void should_only_commit_without_pushing_changes_when_version_is_snapshot() {
		this.updater.commitAndTagIfApplicable(this.file, projectVersion("1.0.0.BUILD-SNAPSHOT"));

		then(this.gitRepo).should().commit(eq("Bumping versions"));
		then(this.gitRepo).should(never()).tag(anyString());
	}

	@Test
	public void should_commit_tag_and_push_tag_when_version_is_not_snapshot() {
		this.updater.commitAndTagIfApplicable(this.file, projectVersion("1.0.0.RELEASE"));

		then(this.gitRepo).should().commit(eq("Update SNAPSHOT to 1.0.0.RELEASE"));
		then(this.gitRepo).should().tag(eq("v1.0.0.RELEASE"));
		then(this.gitRepo).should().pushTag(eq("v1.0.0.RELEASE"));
	}

	@Test
	public void should_commit_when_snapshot_version_is_present_with_post_release_msg() {
		this.updater.commitAfterBumpingVersions(this.file, projectVersion("1.0.0.BUILD-SNAPSHOT"));

		then(this.gitRepo).should().commit(eq("Bumping versions to 1.0.1.BUILD-SNAPSHOT after release"));
		then(this.gitRepo).should(never()).tag(anyString());
	}

	@Test
	public void should_not_commit_when_non_snapshot_version_is_present() {
		this.updater.commitAfterBumpingVersions(this.file, projectVersion("1.0.0.RELEASE"));

		then(this.gitRepo).should(never()).commit(eq("Bumping versions after release"));
		then(this.gitRepo).should(never()).tag(anyString());
	}

	@Test
	public void should_not_revert_changes_for_snapshots() {
		this.updater.revertChangesIfApplicable(this.file, projectVersion("1.0.0.BUILD-SNAPSHOT"));

		then(this.gitRepo).should(never()).revert(anyString());
	}

	@Test
	public void should_revert_changes_when_version_is_not_snapshot() {
		this.updater.revertChangesIfApplicable(this.file, projectVersion("1.0.0.RELEASE"));

		then(this.gitRepo).should().revert(eq("Going back to snapshots"));
	}

	@Test
	public void should_push_current_branch() {
		this.updater.pushCurrentBranch(this.file);

		then(this.gitRepo).should().pushCurrentBranch();
	}

	@Test
	public void should_throw_exception_when_no_fixed_version_passed_for_the_project() {
		BDDAssertions.thenThrownBy(() -> this.updater
				.cloneProjectFromOrg("spring-cloud-sleuth"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("You haven't provided a version");
	}

	@Test
	public void should_not_check_out_a_branch_if_it_does_not_exist_when_cloning_from_org() {
		this.properties.getFixedVersions().put("spring-cloud-sleuth", "2.3.4.RELEASE");
		given(this.gitRepo.hasBranch(anyString())).willReturn(true);
		given(this.gitRepo.hasBranch("2.3.x")).willReturn(false);

		this.updater.cloneProjectFromOrg("spring-cloud-sleuth");

		then(this.gitRepo).should(never()).checkout(anyString());
	}

	@Test
	public void should_check_out_a_branch_if_it_exists_when_cloning_from_org() {
		this.properties.getFixedVersions().put("spring-cloud-sleuth", "2.3.4.RELEASE");
		given(this.gitRepo.hasBranch(anyString())).willReturn(false);
		given(this.gitRepo.hasBranch("2.3.x")).willReturn(true);

		this.updater.cloneProjectFromOrg("spring-cloud-sleuth");

		then(this.gitRepo).should().checkout("2.3.x");
	}

	@Test
	public void should_check_out_a_branch_if_it_exists_when_cloning_from_org_and_its_a_release_train_version() {
		this.properties.getFixedVersions().put("spring-cloud-release", "Finchley.SR6");
		given(this.gitRepo.hasBranch(anyString())).willReturn(false);
		given(this.gitRepo.hasBranch("Finchley.x")).willReturn(true);

		this.updater.cloneProjectFromOrg("spring-cloud-release");

		then(this.gitRepo).should().checkout("Finchley.x");
	}

	private ProjectVersion projectVersion(String version) {
		return new ProjectVersion("foo", version);
	}
}