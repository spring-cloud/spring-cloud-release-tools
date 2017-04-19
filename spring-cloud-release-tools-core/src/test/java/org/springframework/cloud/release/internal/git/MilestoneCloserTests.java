package org.springframework.cloud.release.internal.git;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.json.Json;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

import com.jcabi.github.Milestone;
import com.jcabi.github.Repo;
import com.jcabi.github.mock.MkGithub;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class MilestoneCloserTests {

	MkGithub github;
	Repo repo;
	@Rule public OutputCapture capture = new OutputCapture();

	@Before
	public void setup() throws URISyntaxException, IOException  {
		this.github = new MkGithub();
		this.repo = createSleuthRepo(this.github);
	}

	@Test
	public void should_close_milestone_if_there_is_one() throws IOException {
		MilestoneCloser closer = new MilestoneCloser(this.github, withToken()) {
			@Override String org() {
				return repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone)
					throws IOException {
				return "0.2.0.BUILD-SNAPSHOT";
			}
		};
		repo.milestones().create("0.2.0.BUILD-SNAPSHOT");

		closer.closeMilestone(sleuthProject());

		then(this.capture.toString()).doesNotContain("No matching milestone was found");
	}

	@Test
	public void should_close_milestone_when_the_milestone_contains_numeric_version_only() throws IOException {
		MilestoneCloser closer = new MilestoneCloser(this.github, withToken()) {
			@Override String org() {
				return repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone)
					throws IOException {
				return "0.2.0";
			}
		};
		repo.milestones().create("0.2.0");

		closer.closeMilestone(sleuthProject());

		then(this.capture.toString()).doesNotContain("No matching milestone was found");
	}

	private ProjectVersion sleuthProject() {
		return new ProjectVersion("spring-cloud-sleuth", "0.2.0.BUILD-SNAPSHOT");
	}

	@Test
	public void should_throw_exception_when_there_is_no_matching_milestone() throws IOException {
		MilestoneCloser closer = new MilestoneCloser(this.github, withToken()) {
			@Override String org() {
				return repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone)
					throws IOException {
				return "0.1.0.BUILD-SNAPSHOT";
			}
		};
		repo.milestones().create("v0.2.0.BUILD-SNAPSHOT");

		closer.closeMilestone(sleuthProject());
		then(this.capture.toString()).contains("No matching milestone was found");
	}

	@Test
	public void should_throw_exception_when_io_problems_occurred() throws IOException {
		MilestoneCloser closer = new MilestoneCloser(this.github, withToken()) {
			@Override String org() {
				return repo.coordinates().user();
			}

			@Override String milestoneTitle(Milestone.Smart milestone)
					throws IOException {
				throw new IOException("foo");
			}
		};
		repo.milestones().create("v0.2.0.BUILD-SNAPSHOT");

		thenThrownBy(() -> closer.closeMilestone(sleuthProject()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("foo");
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
		MilestoneCloser closer = new MilestoneCloser(new ReleaserProperties());

		thenThrownBy(() -> closer.closeMilestone(sleuthProject()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("You have to pass Github OAuth token for milestone closing to be operational");
	}

	ReleaserProperties withToken() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken("foo");
		return properties;
	}
}