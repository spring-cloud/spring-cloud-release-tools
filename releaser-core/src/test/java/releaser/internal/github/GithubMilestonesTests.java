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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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

	private static final String USER = "marcingrzejszczak";

	private static final String TOKEN = "TOKEN";

	GitHub github;

	@BeforeEach
	public void setup() throws IOException {
		this.github = GitHub.connectToEnterpriseWithOAuth("http://localhost:12345", USER, TOKEN);
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

	@Test
	void should_create_a_release_for_release_train_with_links_to_other_releases(CapturedOutput capturedOutput)
			throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, metaReleaser());

		milestones.createReleaseNotesForMilestone(releaseTrain());

		then(capturedOutput.toString()).contains("Created a new release");
	}

	@Test
	void should_create_a_release_for_a_single_project(CapturedOutput capturedOutput) throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override
			String readChangelogFromGeneratorOutput(ProjectVersion version) throws IOException {
				return "FOOOO";
			}

			@Override
			void downloadChangeLog() {

			}
		};

		milestones.createReleaseNotesForMilestone(gaContractVersion());

		then(capturedOutput.toString()).contains("Created a new release");
	}

	ProjectVersion nonGaSleuthProject() {
		return new ProjectVersion("test-repo", "0.0.1-SNAPSHOT");
	}

	ProjectVersion releaseTrain() {
		return new ProjectVersion("spring-cloud-release", "2021.0.8");
	}

	ProjectVersion gaContractVersion() {
		return new ProjectVersion("spring-cloud-contract", "3.1.8");
	}

	ProjectVersion notMatchingProjectVersion() {
		return new ProjectVersion("test-repo", "0.0.100-SNAPSHOT");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken(TOKEN);
		properties.getGit().setOrgName(ORG);
		properties.getGit().setUsername(USER);
		return properties;
	}

	ReleaserProperties metaReleaser() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.setFixedVersions(v2021_0_8());
		properties.getGit().setOauthToken(TOKEN);
		properties.getGit().setOrgName(ORG);
		properties.getGit().setUsername(USER);
		properties.getMetaRelease()
				.setReleaseTrainDependencyNames(Arrays.asList("spring-cloud", "spring-cloud-release"));
		return properties;
	}

	public Map<String, String> v2021_0_8() {
		Map<String, String> versions = new LinkedHashMap<>();
		versions.put("spring-boot", "2.6.15");
		versions.put("spring-cloud-build", "3.1.8");
		versions.put("spring-cloud-commons", "3.1.7");
		versions.put("spring-cloud-function", "3.2.11");
		versions.put("spring-cloud-stream", "3.2.9");
		versions.put("spring-cloud-bus", "3.1.2");
		versions.put("spring-cloud-task", "2.4.6");
		versions.put("spring-cloud-config", "3.1.8");
		versions.put("spring-cloud-netflix", "3.1.7");
		versions.put("spring-cloud-cloudfoundry", "3.1.3");
		versions.put("spring-cloud-openfeign", "3.1.8");
		versions.put("spring-cloud-consul", "3.1.4");
		versions.put("spring-cloud-circuitbreaker", "2.1.7");
		versions.put("spring-cloud-gateway", "3.1.8");
		versions.put("spring-cloud-sleuth", "3.1.9");
		versions.put("spring-cloud-zookeeper", "3.1.4");
		versions.put("spring-cloud-contract", "3.1.8");
		versions.put("spring-cloud-kubernetes", "2.1.8");
		versions.put("spring-cloud-vault", "3.1.3");
		versions.put("spring-cloud-release", "2021.0.8");
		versions.put("spring-cloud-cli", "3.1.1");
		return versions;
	}

	ReleaserProperties withThreshold() {
		ReleaserProperties properties = withToken();
		properties.getGit().setNumberOfCheckedMilestones(0);
		properties.getGit().setOauthToken(TOKEN);
		properties.getGit().setOrgName(ORG);
		return properties;
	}

}
