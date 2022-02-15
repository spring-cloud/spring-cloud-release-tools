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

package releaser.internal.github;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.jcabi.github.Github;
import com.jcabi.github.Repo;
import com.jcabi.github.Repos;
import com.jcabi.github.mock.MkGithub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.BDDMockito;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * @author Marcin Grzejszczak
 */
@ExtendWith(OutputCaptureExtension.class)
public class GithubIssuesTests {

	@TempDir
	public File folder;

	MkGithub github;

	Repo repo;

	@BeforeEach
	public void setup() throws IOException {
		this.github = github("spring-guides");
		this.repo = createGettingStartedGuides(this.github);
	}

	public void setupStartSpringIo() throws IOException {
		this.github = github("spring-io");
		this.repo = createStartSpringIo(this.github);
	}

	private MkGithub github(String login) throws IOException {
		return new MkGithub(login);
	}

	@Test
	public void should_not_do_anything_for_non_release_train_version() {
		Github github = BDDMockito.mock(Github.class);
		GithubIssues issues = new GithubIssues(Collections.emptyList());

		issues.fileIssueInSpringGuides(
				new Projects(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
						new ProjectVersion("spring-cloud-build", "2.0.0.BUILD-SNAPSHOT")),
				new ProjectVersion("sc-release", "Edgware.BUILD-SNAPSHOT"));

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	@Test
	public void should_not_do_anything_if_not_applicable() {
		Github github = BDDMockito.mock(Github.class);
		GithubIssues issues = new GithubIssues(Collections.emptyList());

		issues.fileIssueInSpringGuides(
				new Projects(new ProjectVersion("foo", "1.0.0.RELEASE"), new ProjectVersion("bar", "2.0.0.RELEASE"),
						new ProjectVersion("baz", "3.0.0.RELEASE")),
				new ProjectVersion("sc-release", "Edgware.RELEASE"));

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	@Test
	public void should_not_do_anything_for_non_release_train_version_when_updating_startspringio() throws IOException {
		setupStartSpringIo();
		Github github = BDDMockito.mock(Github.class);
		GithubIssues issues = new GithubIssues(Collections.emptyList());

		issues.fileIssueInStartSpringIo(
				new Projects(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
						new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE")),
				new ProjectVersion("sc-release", "Edgware.BUILD-SNAPSHOT"));

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	@Test
	public void should_not_do_anything_if_not_applicable_when_updating_startspringio() throws IOException {
		setupStartSpringIo();
		Github github = BDDMockito.mock(Github.class);
		GithubIssues issues = new GithubIssues(Collections.emptyList());

		issues.fileIssueInStartSpringIo(
				new Projects(new ProjectVersion("foo", "1.0.0.RELEASE"), new ProjectVersion("bar", "2.0.0.RELEASE"),
						new ProjectVersion("baz", "3.0.0.RELEASE")),
				new ProjectVersion("sc-release", "Edgware.RELEASE"));

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	private Repo createGettingStartedGuides(MkGithub github) throws IOException {
		return github.repos().create(new Repos.RepoCreate("getting-started-guides", false));
	}

	private Repo createStartSpringIo(MkGithub github) throws IOException {
		return github.repos().create(new Repos.RepoCreate("start.spring.io", false));
	}

}
