package org.springframework.cloud.release.internal.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import javax.json.Json;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Issue;
import com.jcabi.github.Milestone;
import com.jcabi.github.Repo;
import com.jcabi.github.mock.MkGithub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class GithubIssuesTests {

	MkGithub github;
	Repo repo;
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	@Rule public OutputCapture capture = new OutputCapture();

	@Before
	public void setup() throws URISyntaxException, IOException  {
		this.github = new MkGithub("spring-guides");
		this.repo = createGettingStartedGuides(this.github);
	}

	@Test
	public void should_not_do_anything_for_non_release_train_version() throws IOException {
		GithubIssues issues = new GithubIssues(this.github, withToken());

		issues.fileIssue(new Projects(
				new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT")
				), new ProjectVersion("sc-release", "Edgware.BUILD-SNAPSHOT"));

		then(this.capture.toString()).contains("Guide issue creation will occur only");
	}

	@Test
	public void should_file_an_issue_for_release_version() throws IOException {
		GithubIssues issues = new GithubIssues(this.github, withToken());

		issues.fileIssue(new Projects(
				new ProjectVersion("foo", "1.0.0.RELEASE"),
				new ProjectVersion("bar", "2.0.0.RELEASE"),
				new ProjectVersion("baz", "3.0.0.RELEASE")
				), new ProjectVersion("sc-release", "Edgware.RELEASE"));

		then(this.capture.toString()).doesNotContain("Guide issue creation will occur only");
		Issue issue = this.github.repos()
				.get(new Coordinates.Simple("spring-guides", "getting-started-guides"))
				.issues().get(1);
		then(issue.exists()).isTrue();
		Issue.Smart smartIssue = new Issue.Smart(issue);
		then(smartIssue.title()).isEqualTo("Edgware.RELEASE Spring Cloud Release took place");
		then(smartIssue.body())
				.contains("Spring Cloud [Edgware.RELEASE]")
				.contains("foo : `1.0.0.RELEASE`")
				.contains("bar : `2.0.0.RELEASE`")
				.contains("baz : `3.0.0.RELEASE`");
	}

	@Test
	public void should_throw_exception_when_no_token_was_passed() {
		GithubIssues issues = new GithubIssues(new ReleaserProperties());

		thenThrownBy(() -> issues.fileIssue(new Projects(Collections.emptySet()),
				nonGaSleuthProject()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("You have to pass Github OAuth token for milestone closing to be operational");
	}

	private Repo createGettingStartedGuides(MkGithub github) throws IOException {
		return github.repos().create(
				Json.createObjectBuilder().add(
						"name",
						"getting-started-guides"
				).build()
		);
	}

	private ProjectVersion nonGaSleuthProject() {
		return new ProjectVersion("spring-cloud-sleuth", "0.2.0.BUILD-SNAPSHOT");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken("foo");
		properties.getPom().setBranch("vEdgware.RELEASE");
		return properties;
	}
}