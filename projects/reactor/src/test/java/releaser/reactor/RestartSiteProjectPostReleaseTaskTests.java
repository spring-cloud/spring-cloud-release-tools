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

package releaser.reactor;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.spring.Arguments;
import releaser.internal.spring.ProjectToRun;
import releaser.internal.spring.ProjectsFromBom;
import releaser.internal.tech.ExecutionResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RestartSiteProjectPostReleaseTaskTests {

	@Autowired
	RestartSiteProjectPostReleaseTask task;

	@Autowired
	CfClient cfClient;

	@Test
	void should_update_the_website() {
		Arguments arguments = Arguments.forProject(reactorCoreProject());

		ExecutionResult result = task.runTask(arguments);

		BDDAssertions.then(result.isSuccess()).isTrue();
		BDDMockito.then(this.cfClient).should()
				.restartApp(BDDMockito.eq("projectreactor"));
	}

	@Test
	void should_not_update_the_website_if_project_not_reactor_core() {
		Arguments arguments = Arguments.forProject(nonReactorCoreProject());

		ExecutionResult result = task.runTask(arguments);

		BDDAssertions.then(result.isSuccess()).isTrue();
		BDDMockito.then(this.cfClient).shouldHaveNoInteractions();
	}

	private ProjectToRun reactorCoreProject() {
		return new ProjectToRun(null,
				new ProjectsFromBom(new Projects(), new ProjectVersion("foo", "1.0.0")),
				new ProjectVersion("foo", "1.0.0"), new ReleaserProperties(),
				BDDMockito.mock(Options.class)) {
			@Override
			public String name() {
				return "reactor-core";
			}
		};
	}

	private ProjectToRun nonReactorCoreProject() {
		return new ProjectToRun(null,
				new ProjectsFromBom(new Projects(), new ProjectVersion("foo", "1.0.0")),
				new ProjectVersion("foo", "1.0.0"), new ReleaserProperties(),
				BDDMockito.mock(Options.class)) {
			@Override
			public String name() {
				return "whatever";
			}
		};
	}

}
