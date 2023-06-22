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

package releaser.internal.github;

import java.io.IOException;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.BDDMockito;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@ExtendWith(OutputCaptureExtension.class)
@WireMockTest(httpPort = 12346)
class GithubIssueFillerTests {

	private static final String ORG = "marcingrzejszczak";

	private static final String REPO = "test-repo";

	private static final String TOKEN = "FOO";

	GitHub github;

	ReleaserProperties properties = withToken();

	GithubIssueFiler filer;

	@BeforeEach
	void setup() throws IOException {
		this.github = GitHub.connectToEnterprise("http://localhost:12346", TOKEN);
		this.filer = new GithubIssueFiler(github, properties);
	}

	@Test
	void should_not_do_anything_for_non_release_train_version() {
		GitHub github = BDDMockito.mock(GitHub.class);

		filer.fileAGitHubIssue(ORG, REPO, new ProjectVersion("foo", "1.0.0-SNAPSHOT"), "foo", "bar");

		BDDMockito.then(github).shouldHaveNoInteractions();
	}

	@Test
	void should_file_an_issue_for_release_version(CapturedOutput capturedOutput) throws IOException {
		filer.fileAGitHubIssue(ORG, REPO, new ProjectVersion("foo", "1.0.0"), "foo", "bar");

		BDDAssertions.then(capturedOutput.toString()).contains("Successfully created an issue");
	}

	@Test
	void should_not_file_an_issue_for_release_version_if_one_is_already_created(CapturedOutput capturedOutput)
			throws IOException {
		new GithubIssueFiler(github, properties) {
			@Override
			boolean issueAlreadyFiled(GHRepository springGuides, String issueTitle) throws IOException {
				return true;
			}
		}.fileAGitHubIssue(ORG, REPO, new ProjectVersion("foo", "1.0.0"), "foo", "bar");

		BDDAssertions.then(capturedOutput.toString()).doesNotContain("Successfully created an issue")
				.contains("Issue already filed, will not do that again");
	}

	@Test
	void should_throw_exception_when_no_token_was_passed() {
		properties.getGit().setOauthToken("");

		thenThrownBy(() -> filer.fileAGitHubIssue(ORG, REPO, new ProjectVersion("foo", "1.0.0"), "foo", "bar"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("You have to pass Github OAuth token for milestone closing to be operational");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOrgName(ORG);
		properties.getGit().setOauthToken(TOKEN);
		properties.getPom().setBranch("vEdgware.RELEASE");
		properties.getGit().setUpdateSpringGuides(true);
		properties.getGit().setUpdateStartSpringIo(true);
		return properties;
	}

}
