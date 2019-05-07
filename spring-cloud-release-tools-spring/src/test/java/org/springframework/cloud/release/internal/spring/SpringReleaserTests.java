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
import org.springframework.cloud.release.internal.pom.ProcessedProject;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringReleaserTests {

	private static final Logger log = LoggerFactory.getLogger(SpringReleaserTests.class);

	@Mock
	Releaser releaser;

	ReleaserProperties properties = properties();

	@Mock
	OptionsProcessor optionsProcessor;

	@Mock
	ApplicationContext context;

	Aware1 aware1 = new Aware1();

	Aware2 aware2 = new Aware2();

	ReleaserPropertiesUpdater updater;

	@Mock
	ApplicationEventPublisher applicationEventPublisher;

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
		BDDMockito.then(this.optionsProcessor).should(BDDMockito.never()).processOptions(
				BDDMockito.any(Options.class), BDDMockito.any(Args.class));
	}

	private void assertBuildCommand(Queue<ReleaserProperties> properties) {
		BDDAssertions.then(properties.poll().getMaven().getBuildCommand())
				.isEqualTo("./scripts/noIntegration.sh");
		BDDAssertions.then(properties.poll().getMaven().getBuildCommand())
				.isEqualTo("build");
	}

	private SpringReleaser stubbedSpringReleaser() {
		return new SpringReleaser(this.releaser, this.properties, this.optionsProcessor,
				this.updater, this.applicationEventPublisher) {

			@Override
			Args postReleaseOptionsAgs(Options options,
					ProjectsAndVersion projectsAndVersion,
					List<ProcessedProject> processedProjects) {
				return new Args(TaskType.RELEASE);
			}

			@Override
			List<String> metaReleaseProjects(Options options) {
				return Arrays.asList("aware1", "aware2");
			}

			@Override
			ProjectsAndVersion processProject(Options options, File project,
					TaskType taskType) {
				return new ProjectsAndVersion(sampleProjects(),
						new ProjectVersion("spring-cloud-foo", "1.0.0.BUILD-SNAPSHOT"));
			}

			@Override
			ProjectVersion assertNoSnapshotsForANonSnapshotProject(File project,
					Projects projectsToUpdate) {
				return sampleVersion();
			}
		};
	}

	private Projects sampleProjects() {
		return new Projects(sampleVersion());
	}

	private ProjectVersion sampleVersion() {
		return new ProjectVersion("spring-cloud-foo", "1.0.0.RELEASE");
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
