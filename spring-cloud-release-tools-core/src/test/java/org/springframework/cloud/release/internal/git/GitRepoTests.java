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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.cloud.release.internal.buildsystem.TestUtils;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.springframework.cloud.release.internal.git.GitTestUtils.clonedProject;
import static org.springframework.cloud.release.internal.git.GitTestUtils.setOriginOnProjectToTmp;

/**
 * @author Marcin Grzejszczak
 */
public class GitRepoTests {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	File springCloudReleaseProject;

	File tmpFolder;

	GitRepo gitRepo;

	@Before
	public void setup() throws IOException, URISyntaxException {
		this.tmpFolder = this.tmp.newFolder();
		this.springCloudReleaseProject = new File(
				GitRepoTests.class.getResource("/projects/spring-cloud-release").toURI());
		TestUtils.prepareLocalRepo();
		this.gitRepo = new GitRepo(this.tmpFolder);
	}

	@Test
	public void should_clone_the_project_from_a_given_location() throws IOException {
		URIish uri = new URIish(this.springCloudReleaseProject.toURI().toURL());
		this.gitRepo.cloneProject(uri);

		then(new File(new File(this.tmpFolder, uri.getHumanishName()), ".git")).exists();
	}

	@Test
	public void should_throw_exception_when_there_is_no_repo() {
		thenThrownBy(() -> this.gitRepo.cloneProject(
				new URIish(GitRepoTests.class.getResource("/projects/").toURI().toURL())))
						.isInstanceOf(IllegalStateException.class)
						.hasMessageContaining("Exception occurred while cloning repo");
	}

	@Test
	public void should_throw_an_exception_when_failed_to_initialize_the_repo() {
		thenThrownBy(() -> new GitRepo(this.tmpFolder, new ExceptionThrowingJGitFactory())
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL())))
						.isInstanceOf(IllegalStateException.class)
						.hasMessageContaining("Exception occurred while cloning repo")
						.hasCauseInstanceOf(CustomException.class);
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo() throws IOException {
		URIish uri = new URIish(this.springCloudReleaseProject.toURI().toURL());
		File project = this.gitRepo.cloneProject(uri);
		new GitRepo(project).checkout("vCamden.SR3");

		File pom = new File(new File(this.tmpFolder, uri.getHumanishName()), "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath())
				.anyMatch(s -> s.contains("<version>Camden.SR3</version>"))).isTrue();
	}

	@Test
	public void should_check_out_a_branch_on_cloned_repo2() throws IOException {
		URIish uri = new URIish(this.springCloudReleaseProject.toURI().toURL());
		File project = this.gitRepo.cloneProject(uri);
		new GitRepo(project).checkout("Camden.x");

		File pom = new File(new File(this.tmpFolder, uri.getHumanishName()), "pom.xml");
		then(pom).exists();
		then(Files.lines(pom.toPath())
				.anyMatch(s -> s.contains("<version>Camden.BUILD-SNAPSHOT</version>")))
						.isTrue();
	}

	@Test
	public void should_return_true_if_branch_exists() throws IOException {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));

		then(new GitRepo(project).hasBranch("Camden.x")).isTrue();
	}

	@Test
	public void should_return_false_if_branch_does_not_exist() throws IOException {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));

		then(new GitRepo(project).hasBranch("aksjdhkasjkajshd")).isFalse();
	}

	@Test
	public void should_throw_an_exception_when_checking_out_nonexisting_branch()
			throws IOException {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		try {
			new GitRepo(project).checkout("nonExistingBranch");
			fail("should throw an exception");
		}
		catch (IllegalStateException e) {
			then(e).hasMessageContaining("Ref nonExistingBranch cannot be resolved");
		}
	}

	@Test
	public void should_commit_changes() throws Exception {
		URIish uri = new URIish(this.springCloudReleaseProject.toURI().toURL());
		File project = new GitRepo(this.tmpFolder).cloneProject(uri);
		createNewFile(project);

		new GitRepo(project).commit("some message");

		try (Git git = openGitProject(project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	@Test
	public void should_not_commit_empty_changes() throws Exception {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		createNewFile(project);
		new GitRepo(project).commit("some message");

		new GitRepo(project).commit("empty commit");

		try (Git git = openGitProject(project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isNotEqualTo("empty commit");
		}
	}

	@Test
	public void should_create_a_tag() throws Exception {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		createNewFile(project);
		new GitRepo(project).commit("some message");

		new GitRepo(project).tag("v1.0.0");

		try (Git git = openGitProject(project)) {
			tagIsPresent(git, "v1.0.0");
		}
	}

	private void tagIsPresent(Git git, String tag) throws GitAPIException {
		List<Ref> refs = git.tagList().call();
		System.out.println("All tags" + refs);
		then(refs.stream().anyMatch(ref -> ref.getName().startsWith("refs/tags/" + tag)))
				.isTrue();
	}

	@Test
	public void should_push_changes_to_master_branch() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);
		new GitRepo(project).commit("some message");

		new GitRepo(project).pushBranch("master");

		try (Git git = openGitProject(origin)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	@Test
	public void should_push_changes_to_current_branch() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);
		new GitRepo(project).commit("some message");

		new GitRepo(project).pushCurrentBranch();

		try (Git git = openGitProject(origin)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	@Test
	public void should_return_the_branch_name() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);

		String branch = new GitRepo(project).currentBranch();

		then(branch).isEqualTo("master");
	}

	@Test
	public void should_push_a_tag_to_new_branch_in_origin() throws Exception {
		File origin = clonedProject(this.tmp.newFolder(), this.springCloudReleaseProject);
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		setOriginOnProjectToTmp(origin, project);
		createNewFile(project);
		new GitRepo(project).commit("some message");
		new GitRepo(project).tag("v5.6.7.RELEASE");

		new GitRepo(project).pushTag("v5.6.7.RELEASE");

		try (Git git = openGitProject(origin)) {
			tagIsPresent(git, "v5.6.7");
			git.checkout().setName("v5.6.7.RELEASE").call();
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("some message");
		}
	}

	private Git openGitProject(File project) {
		return new GitRepo.JGitFactory().open(project);
	}

	private void createNewFile(File project) throws Exception {
		File newFile = new File(project, "newFile");
		newFile.createNewFile();
		try (PrintStream out = new PrintStream(new FileOutputStream(newFile))) {
			out.print("foo");
		}
		try (Git git = openGitProject(project)) {
			git.add().addFilepattern("newFile").call();
		}
	}

	@Test
	public void should_revert_changes() throws Exception {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));
		File foo = new File(project, "foo");
		foo.createNewFile();
		new GitRepo(project).commit("Update SNAPSHOT to 1.0.0.RC1");

		new GitRepo(project).revert("Reverting the commit");

		try (Git git = openGitProject(project)) {
			RevCommit revCommit = git.log().call().iterator().next();
			then(revCommit.getShortMessage()).isEqualTo("Reverting the commit");
		}
	}

	@Test
	public void should_not_revert_changes_when_commit_message_is_not_related_to_updating_snapshots()
			throws Exception {
		File project = new GitRepo(this.tmpFolder)
				.cloneProject(new URIish(this.springCloudReleaseProject.toURI().toURL()));

		BDDAssertions.thenThrownBy(() -> new GitRepo(project).revert("some message"))
				.hasMessageContaining("Won't revert the commit with id");
	}

}

class ExceptionThrowingJGitFactory extends GitRepo.JGitFactory {

	@Override
	CloneCommand getCloneCommandByCloneRepository() {
		throw new CustomException("foo");
	}

}

class CustomException extends RuntimeException {

	CustomException(String message) {
		super(message);
	}

}
