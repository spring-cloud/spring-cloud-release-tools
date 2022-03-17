/*
 * Copyright 2013-2022 the original author or authors.
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

package releaser.cloud.github;

import java.io.IOException;
import java.util.Collections;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issue;
import com.jcabi.github.Repo;
import com.jcabi.github.Repos;
import com.jcabi.github.mock.MkGithub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import releaser.cloud.SpringCloudReleaserProperties;
import releaser.internal.ReleaserProperties;
import releaser.internal.github.CustomGithubIssues;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class SpringCloudGithubIssuesTests {

	ReleaserProperties properties = SpringCloudReleaserProperties.get();

	MkGithub github;

	Repo repo;

	@BeforeEach
	public void setup() throws IOException {
		this.github = github("spring-guides");
		this.properties.getGit().setOauthToken("a");
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
		CustomGithubIssues githubIssues = new SpringCloudGithubIssues(github, properties);

		githubIssues.fileIssueInSpringGuides(
				new Projects(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
						new ProjectVersion("spring-cloud-build", "2.0.0.BUILD-SNAPSHOT")),
				new ProjectVersion("sc-release", "Edgware.BUILD-SNAPSHOT"));

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	@Test
	public void should_file_an_issue_for_release_version() throws IOException {
		CustomGithubIssues issues = new SpringCloudGithubIssues(github, properties);
		properties.getPom().setBranch("vEdgware.RELEASE");

		issues.fileIssueInSpringGuides(
				new Projects(new ProjectVersion("spring-cloud-foo", "1.0.0.RELEASE"),
						new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE"),
						new ProjectVersion("bar", "2.0.0.RELEASE"), new ProjectVersion("baz", "3.0.0.RELEASE")),
				new ProjectVersion("sc-release", "Edgware.RELEASE"));

		Issue issue = this.github.repos().get(new Coordinates.Simple("spring-guides", "getting-started-guides"))
				.issues().get(1);
		then(issue.exists()).isTrue();
		Issue.Smart smartIssue = new Issue.Smart(issue);
		then(smartIssue.title()).isEqualTo("Upgrade to Spring Cloud Edgware.RELEASE");
		then(smartIssue.body()).contains(
				"Release train [spring-cloud-release] in version [Edgware.RELEASE] released with the following projects")
				.contains("spring-cloud-foo : `1.0.0.RELEASE`").contains("bar : `2.0.0.RELEASE`")
				.contains("baz : `3.0.0.RELEASE`");
	}

	@Test
	public void should_throw_exception_when_no_token_was_passed() {
		properties.getGit().setOauthToken("");
		CustomGithubIssues issues = new SpringCloudGithubIssues(github, properties);

		thenThrownBy(() -> issues.fileIssueInSpringGuides(
				new Projects(Collections.singletonList(new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE"))),
				nonGaSleuthProject())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
						"You have to pass Github OAuth token for milestone closing to be operational");
	}

	@Test
	public void should_not_do_anything_for_non_release_train_version_when_updating_startspringio() throws IOException {
		setupStartSpringIo();
		Github github = BDDMockito.mock(Github.class);
		CustomGithubIssues issues = new SpringCloudGithubIssues(github, properties);

		issues.fileIssueInStartSpringIo(
				new Projects(new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
						new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE")),
				new ProjectVersion("sc-release", "Edgware.BUILD-SNAPSHOT"));

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	@Test
	public void should_file_an_issue_for_release_version_when_updating_startspringio() throws IOException {
		setupStartSpringIo();
		CustomGithubIssues issues = new SpringCloudGithubIssues(github, properties);
		properties.getPom().setBranch("vEdgware.RELEASE");

		issues.fileIssueInStartSpringIo(new Projects(new ProjectVersion("spring-cloud-foo", "1.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE"), new ProjectVersion("bar", "2.0.0.RELEASE"),
				new ProjectVersion("baz", "3.0.0.RELEASE"), new ProjectVersion("spring-boot", "1.2.3.RELEASE")),
				new ProjectVersion("sc-release", "Edgware.RELEASE"));

		Issue issue = this.github.repos().get(new Coordinates.Simple("spring-io", "start.spring.io")).issues().get(1);
		then(issue.exists()).isTrue();
		Issue.Smart smartIssue = new Issue.Smart(issue);
		then(smartIssue.title()).isEqualTo("Upgrade to Spring Cloud Edgware.RELEASE");
		then(smartIssue.body()).contains(
				"Release train [spring-cloud-release] in version [Edgware.RELEASE] released with the Spring Boot version [`1.2.3.RELEASE`]");
	}

	@Test
	public void should_throw_exception_when_no_token_was_passed_when_updating_startspringio() throws IOException {
		setupStartSpringIo();
		properties.getGit().setOauthToken("");
		CustomGithubIssues issues = new SpringCloudGithubIssues(github, properties);

		thenThrownBy(() -> issues.fileIssueInStartSpringIo(
				new Projects(Collections.singletonList(new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE"))),
				nonGaSleuthProject())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
						"You have to pass Github OAuth token for milestone closing to be operational");
	}

	private Repo createGettingStartedGuides(MkGithub github) throws IOException {
		return github.repos().create(new Repos.RepoCreate("getting-started-guides", false));
	}

	private Repo createStartSpringIo(MkGithub github) throws IOException {
		return github.repos().create(new Repos.RepoCreate("start.spring.io", false));
	}

	private ProjectVersion nonGaSleuthProject() {
		return new ProjectVersion("spring-cloud-sleuth", "0.2.0.BUILD-SNAPSHOT");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		properties.getGit().setOauthToken("foo");
		properties.getPom().setBranch("vEdgware.RELEASE");
		properties.getGit().setUpdateSpringGuides(true);
		properties.getGit().setUpdateStartSpringIo(true);
		return properties;
	}

}
