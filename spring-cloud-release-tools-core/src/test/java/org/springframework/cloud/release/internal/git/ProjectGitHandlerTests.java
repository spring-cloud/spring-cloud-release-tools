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

package org.springframework.cloud.release.internal.git;

import java.io.File;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectGitHandlerTests {

	@Mock
	GitRepo gitRepo;

	ReleaserProperties properties = new ReleaserProperties();

	ProjectGitHandler updater = new ProjectGitHandler(this.properties) {
		@Override
		GitRepo gitRepo(File workingDir) {
			return ProjectGitHandlerTests.this.gitRepo;
		}

		@Override
		File cloneProject(String url) {
			return new File(".");
		}
	};

	File file = new File("");

	@Test
	public void should_only_commit_without_pushing_changes_when_version_is_snapshot() {
		this.updater.commitAndTagIfApplicable(this.file,
				projectVersion("1.0.0.BUILD-SNAPSHOT"));

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
		ProjectVersion bumped = new ProjectVersion("name",
				projectVersion("1.0.0.BUILD-SNAPSHOT").bumpedVersion());
		this.updater.commitAfterBumpingVersions(this.file, bumped);

		then(this.gitRepo).should()
				.commit(eq("Bumping versions to 1.0.1.BUILD-SNAPSHOT after release"));
		then(this.gitRepo).should(never()).tag(anyString());
	}

	@Test
	public void should_not_commit_when_non_snapshot_version_is_present() {
		this.updater.commitAfterBumpingVersions(this.file,
				projectVersion("1.0.0.RELEASE"));

		then(this.gitRepo).should(never()).commit(eq("Bumping versions after release"));
		then(this.gitRepo).should(never()).tag(anyString());
	}

	@Test
	public void should_not_revert_changes_for_snapshots() {
		this.updater.revertChangesIfApplicable(this.file,
				projectVersion("1.0.0.BUILD-SNAPSHOT"));

		then(this.gitRepo).should(never()).revert(anyString());
	}

	@Test
	public void should_revert_changes_when_version_is_not_snapshot() {
		this.updater.revertChangesIfApplicable(this.file,
				projectVersion("1.0.0.RELEASE"));

		then(this.gitRepo).should().revert(eq("Going back to snapshots"));
	}

	@Test
	public void should_push_current_branch() {
		this.updater.pushCurrentBranch(this.file);

		then(this.gitRepo).should().pushCurrentBranch();
	}

	@Test
	public void should_throw_exception_when_no_fixed_version_passed_for_the_project() {
		BDDAssertions
				.thenThrownBy(
						() -> this.updater.cloneProjectFromOrg("spring-cloud-sleuth"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("You haven't provided a version");
	}

	@Test
	public void should_not_check_out_a_branch_if_it_does_not_exist_when_cloning_from_org() {
		this.properties.getFixedVersions().put("spring-cloud-sleuth", "2.3.4.RELEASE");
		given(this.gitRepo.hasBranch(anyString())).willReturn(true);
		given(this.gitRepo.hasBranch("2.3.x")).willReturn(false);

		this.updater.cloneProjectFromOrg("spring-cloud-sleuth");

		then(this.gitRepo).should(never()).checkout("2.3.x");
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
		given(this.gitRepo.hasBranch("Finchley")).willReturn(true);

		this.updater.cloneProjectFromOrg("spring-cloud-release");

		then(this.gitRepo).should().checkout("Finchley");
	}

	@Test
	public void should_not_check_out_a_branch_if_it_does_not_exist_when_cloning_and_guessing_branch() {
		given(this.gitRepo.hasBranch(anyString())).willReturn(true);
		given(this.gitRepo.hasBranch("2.3.x")).willReturn(false);

		this.updater.cloneAndGuessBranch(new File(".").getAbsolutePath(),
				"2.3.4.RELEASE");

		then(this.gitRepo).should(never()).checkout("2.3.x");
	}

	@Test
	public void should_check_out_a_branch_if_it_exists_when_cloning_and_guessing_branch() {
		given(this.gitRepo.hasBranch(anyString())).willReturn(false);
		given(this.gitRepo.hasBranch("2.3.x")).willReturn(true);

		this.updater.cloneAndGuessBranch(new File(".").getAbsolutePath(),
				"2.3.4.RELEASE");

		then(this.gitRepo).should().checkout("2.3.x");
	}

	@Test
	public void should_check_out_a_branch_if_it_exists_when_cloning_and_guessing_release_train_branch() {
		given(this.gitRepo.hasBranch(anyString())).willReturn(false);
		given(this.gitRepo.hasBranch("Finchley")).willReturn(true);

		this.updater.cloneAndGuessBranch(new File(".").getAbsolutePath(), "Finchley.SR6");

		then(this.gitRepo).should().checkout("Finchley");
	}

	@Test
	public void should_check_out_a_branch_if_one_of_it_exists() {
		given(this.gitRepo.hasBranch(anyString())).willReturn(false);
		given(this.gitRepo.hasBranch("Finchley")).willReturn(true);

		this.updater.cloneAndGuessBranch(new File(".").getAbsolutePath(), "2.0.0.RELEASE",
				"Finchley.SR6");

		then(this.gitRepo).should(never()).checkout("2.0.0");
		then(this.gitRepo).should().checkout("Finchley");
	}

	private ProjectVersion projectVersion(String version) {
		return new ProjectVersion("foo", version);
	}

}
