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
import java.net.URL;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GitHub;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@ExtendWith(OutputCaptureExtension.class)
@WireMockTest(httpPort = 12345)
class GithubMilestonesTests {

	private static final String ORG = "marcingrzejszczak";

	private static final String TOKEN = "foo";

	GitHub github;

	@BeforeEach
	public void setup() throws IOException {
		this.github = GitHub.connectToEnterprise("http://localhost:12345", TOKEN);
	}

	@Test
	void should_close_milestone_if_there_is_one(CapturedOutput capturedOutput) throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken());

		milestones.closeMilestone(nonGaSleuthProject());

		then(capturedOutput.toString()).doesNotContain("No matching milestone was found");
	}

	@Test
	void should_close_milestone_when_the_milestone_contains_numeric_version_only_and_version_is_ga(
			CapturedOutput capturedOutput) throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken());

		milestones.closeMilestone(gaProject());

		then(capturedOutput.toString()).doesNotContain("No matching milestone was found");
	}

	private ProjectVersion closedProject() {
		return new ProjectVersion("test-repo", "0.1.0");
	}

	private ProjectVersion gaProject() {
		return new ProjectVersion("test-repo", "0.0.1");
	}

	@Test
	void should_not_close_milestone_when_the_milestone_contains_numeric_version_only(CapturedOutput capturedOutput)
			throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken());

		milestones.closeMilestone(notMatchingProjectVersion());

		then(capturedOutput.toString()).contains("No matching milestone was found");
	}

	@Test
	void should_fetch_url_of_a_closed_matching_milestone() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken());

		String url = milestones.milestoneUrl(closedProject());

		then(url).isEqualTo("https://github.com/marcingrzejszczak/test-repo/milestone/3?closed=1");
	}

	@Test
	void should_fetch_url_of_a_closed_matching_milestone_from_cache() {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override
			GHMilestone matchingMilestone(String tagVersion, Iterable<GHMilestone> milestones) {
				throw new AssertionError("This should not be called");
			}
		};
		GithubMilestones.MILESTONE_URL_CACHE.put(gaProject(),
				"https://github.com/marcingrzejszczak/test-repo/milestone/3?closed=1");

		String url = milestones.milestoneUrl(gaProject());

		then(url).isEqualTo("https://github.com/marcingrzejszczak/test-repo/milestone/3?closed=1");
	}

	@Test
	void should_return_null_if_no_matching_milestone_was_found() {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override
			String milestoneTitle(GHMilestone milestone) {
				return "0.9.0";
			}

			@Override
			URL foundMilestoneUrl(GHMilestone milestone) throws IOException {
				return new URL("http://www.foo.com/bar");
			}
		};

		String url = milestones.milestoneUrl(gaProject());

		then(url).isEmpty();
	}

	@Test
	void should_return_null_if_no_matching_milestone_was_found_within_threshold(CapturedOutput capturedOutput)
			throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withThreshold());

		milestones.closeMilestone(gaProject());

		then(capturedOutput.toString()).contains("No matching milestones were found within the provided threshold [0]");
	}

	private ProjectVersion nonGaSleuthProject() {
		return new ProjectVersion("test-repo", "0.0.1-SNAPSHOT");
	}

	private ProjectVersion notMatchingProjectVersion() {
		return new ProjectVersion("test-repo", "0.0.100-SNAPSHOT");
	}

	@Test
	void should_throw_exception_when_there_is_no_matching_milestone(CapturedOutput capturedOutput) throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override
			String org() {
				return ORG;
			}
		};

		milestones.closeMilestone(notMatchingProjectVersion());
		then(capturedOutput.toString()).contains("No matching milestone was found");
	}

	@Test
	void should_print_that_no_milestones_were_found_when_io_problems_occurred(CapturedOutput capturedOutput)
			throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {

			@Override
			GHMilestone matchingMilestone(String tagVersion, Iterable<GHMilestone> milestones) {
				return null;
			}
		};

		milestones.closeMilestone(nonGaSleuthProject());

		then(capturedOutput.toString()).contains("No matching milestone was found");
	}

	@Test
	void should_throw_exception_when_no_token_was_passed() {
		GithubMilestones milestones = new GithubMilestones(new ReleaserProperties());

		thenThrownBy(() -> milestones.closeMilestone(nonGaSleuthProject())).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("You must set the value of the OAuth token");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken(TOKEN);
		properties.getGit().setOrgName(ORG);
		return properties;
	}

	ReleaserProperties withThreshold() {
		ReleaserProperties properties = withToken();
		properties.getGit().setNumberOfCheckedMilestones(0);
		properties.getGit().setOauthToken(TOKEN);
		properties.getGit().setOrgName(ORG);
		return properties;
	}

}
