package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.context.ApplicationContext;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringReleaserTests {

	private static final Logger log = LoggerFactory.getLogger(SpringReleaserTests.class);

	@Mock Releaser releaser;
	ReleaserProperties properties = properties();
	@Mock OptionsProcessor optionsProcessor;
	@Mock ApplicationContext context;
	Aware1 aware1 = new Aware1();
	Aware2 aware2 = new Aware2();
	ReleaserPropertiesUpdater updater;
	File releaserUpdater = new File(ReleaserPropertiesUpdaterTests.class
			.getResource("/projects/releaser-updater/config/releaser.yml").toURI());

	public SpringReleaserTests() throws URISyntaxException {
	}

	@Before
	public void setup() {
		BDDMockito.given(this.releaser.clonedProjectFromOrg(BDDMockito.anyString()))
				.willReturn(new File("/whatever"));
		BDDMockito.given(this.context.getBeansOfType(ReleaserPropertiesAware.class))
				.willReturn(awareBeans());
		this.updater = new ReleaserPropertiesUpdater(this.context) {

			int counter = 0;

			@Override
			File releaserConfig(File clonedProjectFromOrg) {
				if (this.counter == 0) {
					log.info("First run");
					this.counter = this.counter + 1;
					return SpringReleaserTests.this.releaserUpdater;
				}
				log.info("Second run");
				return new File("does/not/exist");
			}
		};
	}

	private Map<String, ReleaserPropertiesAware> awareBeans() {
		Map<String, ReleaserPropertiesAware> aware = new HashMap<>();
		aware.put("aware1", this.aware1);
		aware.put("aware2", this.aware2);
		return aware;
	}

	@Test
	public void should_make_a_copy_of_properties() {
		SpringReleaser releaser = stubbedSpringReleaser();

		releaser.release(new OptionsBuilder().metaRelease(true).options());

		assertBuildCommand(this.aware1.properties);
		assertBuildCommand(this.aware2.properties);
	}

	@Test
	public void should_only_call_post_release() {
		SpringReleaser releaser = stubbedSpringReleaser();
		this.properties.setPostReleaseTasksOnly(true);

		releaser.release(new OptionsBuilder().metaRelease(false).options());

		thenOnlyCallsPostRelease();
	}

	private void thenOnlyCallsPostRelease() {
		BDDMockito.then(this.optionsProcessor).should().postReleaseOptions(
				BDDMockito.any(Options.class), BDDMockito.any(Args.class));
		BDDMockito.then(this.optionsProcessor).should(BDDMockito.never())
				.processOptions(BDDMockito.any(Options.class), BDDMockito.any(Args.class));
	}

	private void assertBuildCommand(Queue<ReleaserProperties> properties) {
		BDDAssertions.then(properties.poll().getMaven().getBuildCommand())
				.isEqualTo("./scripts/noIntegration.sh");
		BDDAssertions.then(properties.poll().getMaven().getBuildCommand())
				.isEqualTo("build");
	}

	private SpringReleaser stubbedSpringReleaser() {
		return new SpringReleaser(this.releaser, this.properties,
				this.optionsProcessor, this.updater) {

			@Override
			Args postReleaseOptionsAgs(Options options, ProjectsAndVersion projectsAndVersion) {
				return new Args(TaskType.RELEASE);
			}

			@Override
			List<String> metaReleaseProjects(Options options) {
				return Arrays.asList("aware1", "aware2");
			}

			@Override
			ProjectsAndVersion processProject(Options options, File project, TaskType taskType) {
				return null;
			}
		};
	}

	private ReleaserProperties properties() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMaven().setBuildCommand("build");
		return properties;
	}
}

class Aware1 implements ReleaserPropertiesAware {

	Queue<ReleaserProperties> properties = new LinkedBlockingQueue<>();

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties.add(properties);
	}
}

class Aware2 implements ReleaserPropertiesAware {

	Queue<ReleaserProperties> properties = new LinkedBlockingQueue<>();

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties.add(properties);
	}
}