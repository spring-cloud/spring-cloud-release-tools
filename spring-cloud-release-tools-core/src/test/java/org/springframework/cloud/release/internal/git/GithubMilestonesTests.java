package org.springframework.cloud.release.internal.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.json.Json;

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

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class GithubMilestonesTests {

	MkGithub github;
	Repo repo;
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	@Rule public OutputCapture capture = new OutputCapture();

	@Before
	public void setup() throws IOException  {
		this.github = new MkGithub();
		this.repo = createSleuthRepo(this.github);
	}

	@Test
	public void should_close_milestone_if_there_is_one() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.2.0.BUILD-SNAPSHOT";
			}
		};
		this.repo.milestones().create("0.2.0.BUILD-SNAPSHOT");

		milestones.closeMilestone(nonGaSleuthProject());

		then(this.capture.toString()).doesNotContain("No matching milestone was found");
	}

	@Test
	public void should_close_milestone_when_the_milestone_contains_numeric_version_only_and_version_is_ga() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.2.0";
			}
		};
		this.repo.milestones().create("0.2.0");

		milestones.closeMilestone(gaSleuthProject());

		then(this.capture.toString()).doesNotContain("No matching milestone was found");
	}

	private ProjectVersion gaSleuthProject() {
		return new ProjectVersion("spring-cloud-sleuth", "0.2.0.RELEASE");
	}

	@Test
	public void should_not_close_milestone_when_the_milestone_contains_numeric_version_only() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.2.0";
			}
		};
		this.repo.milestones().create("0.2.0");

		milestones.closeMilestone(nonGaSleuthProject());

		then(this.capture.toString()).contains("No matching milestone was found");
	}

	@Test
	public void should_fetch_url_of_a_closed_matching_milestone() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.2.0.RELEASE";
			}

			@Override URL foundMilestoneUrl(Milestone.Smart milestone)
					throws IOException {
				return new URL("https://api.github.com/repos/spring-cloud/spring-cloud-sleuth/milestones/33");
			}
		};
		this.repo.milestones().create("0.2.0.RELEASE");

		String url = milestones.milestoneUrl(gaSleuthProject());

		then(url).isEqualTo("https://github.com/spring-cloud/spring-cloud-sleuth/milestone/33?closed=1");
	}

	@Test
	public void should_fetch_url_of_a_closed_matching_milestone_from_cache() {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken());
		GithubMilestones.MILESTONE_URL_CACHE.put(gaSleuthProject(), "https://github.com/spring-cloud/spring-cloud-sleuth/milestone/33?closed=1");

		String url = milestones.milestoneUrl(gaSleuthProject());

		then(url).isEqualTo("https://github.com/spring-cloud/spring-cloud-sleuth/milestone/33?closed=1");
	}

	@Test
	public void should_return_null_if_no_matching_milestone_was_found() {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.9.0.RELEASE";
			}

			@Override URL foundMilestoneUrl(Milestone.Smart milestone)
					throws IOException {
				return new URL("http://foo.com/bar");
			}
		};

		String url = milestones.milestoneUrl(gaSleuthProject());

		then(url).isEmpty();
	}

	@Test
	public void should_return_null_if_no_matching_milestone_was_found_within_threshold() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withThreshold()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.2.0";
			}
		};
		this.repo.milestones().create("0.2.0");

		milestones.closeMilestone(gaSleuthProject());

		then(this.capture.toString()).contains("No matching milestones were found within the provided threshold [0]");
	}

	private ProjectVersion nonGaSleuthProject() {
		return new ProjectVersion("spring-cloud-sleuth", "0.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_throw_exception_when_there_is_no_matching_milestone() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone) {
				return "0.1.0.BUILD-SNAPSHOT";
			}
		};
		this.repo.milestones().create("v0.2.0.BUILD-SNAPSHOT");

		milestones.closeMilestone(nonGaSleuthProject());
		then(this.capture.toString()).contains("No matching milestone was found");
	}

	@Test
	public void should_print_that_no_milestones_were_found_when_io_problems_occurred() throws IOException {
		GithubMilestones milestones = new GithubMilestones(this.github, withToken()) {
			@Override String org() {
				return GithubMilestonesTests.this.repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone)
					throws IOException {
				throw new IOException("foo");
			}
		};
		this.repo.milestones().create("v0.2.0.BUILD-SNAPSHOT");

		milestones.closeMilestone(nonGaSleuthProject());

		then(this.capture.toString()).contains("No matching milestone was found");
	}

	private Repo createSleuthRepo(MkGithub github) throws IOException {
		return github.repos().create(
				Json.createObjectBuilder().add(
						"name",
						"spring-cloud-sleuth"
				).build()
		);
	}

	@Test
	public void should_throw_exception_when_no_token_was_passed() {
		GithubMilestones milestones = new GithubMilestones(new ReleaserProperties());

		thenThrownBy(() -> milestones.closeMilestone(nonGaSleuthProject()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("You must set the value of the OAuth token");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken("foo");
		return properties;
	}

	ReleaserProperties withThreshold() {
		ReleaserProperties properties = withToken();
		properties.getGit().setNumberOfCheckedMilestones(0);
		return properties;
	}
}