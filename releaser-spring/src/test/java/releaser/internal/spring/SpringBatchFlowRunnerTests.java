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

package releaser.internal.spring;

import java.io.File;
import java.nio.charset.Charset;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.options.OptionsBuilder;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.tasks.ProjectPostReleaseReleaserTask;
import releaser.internal.tasks.TrainPostReleaseReleaserTask;
import releaser.internal.tech.ExecutionResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest
@ActiveProfiles("batch")
class SpringBatchFlowRunnerTests {

	ReleaserProperties releaserProperties = new ReleaserProperties();

	@Test
	void should_not_fail_the_release_when_project_post_release_task_fails(@Autowired SpringBatchFlowRunner runner) {
		Options options = new OptionsBuilder().interactive(false).options();
		ProjectsToRun projectsToRun = projectsToRun(options, releaserProperties);
		TasksToRun tasks = new TasksToRun(new MyProjectPostReleaseTask());

		ExecutionResult executionResult = runner.runReleaseTasks(options, releaserProperties, projectsToRun, tasks);

		BDDAssertions.then(executionResult.isUnstable()).isTrue();
	}

	@Test
	void should_execute_post_train_tasks_when_such_property_is_set(@Autowired SpringBatchFlowRunner runner,
			@Autowired ProjectsToRunFactory factory) {
		Options options = new OptionsBuilder().interactive(false).metaRelease(true).options();
		releaserProperties.setPostReleaseTasksOnly(true);
		releaserProperties.getMetaRelease().setReleaseTrainProjectName("foo");
		ProjectsToRun projectsToRun = projectsToRun(options, releaserProperties);
		BDDMockito.given(factory.postReleaseTrain(BDDMockito.any())).willReturn(projectsToRun);
		TasksToRun tasks = new TasksToRun(new MyPostTrainReleaseTask());

		ExecutionResult executionResult = runner.runReleaseTasks(options, releaserProperties, projectsToRun, tasks);

		BDDAssertions.then(executionResult.isSkipped()).isTrue();

		executionResult = runner.runPostReleaseTrainTasks(options, releaserProperties, "post-release", tasks);

		BDDAssertions.then(executionResult.isSuccess()).isTrue();
	}

	private ProjectsToRun projectsToRun(Options options, ReleaserProperties releaserProperties) {
		return new ProjectsToRun(
				new ProjectToRun.ProjectToRunSupplier("test", () -> projectToRun(options, releaserProperties)));
	}

	private ProjectToRun projectToRun(Options options, ReleaserProperties releaserProperties) {
		return new ProjectToRun(new File("."), new ProjectsFromBom(new Projects(), new ProjectVersion("foo", "0.0.1")),
				new ProjectVersion("foo", "0.0.1"), releaserProperties, options);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class Config extends BatchConfiguration {

		@Bean
		ProjectsToRunFactory projectsToRunFactory() {
			return BDDMockito.mock(ProjectsToRunFactory.class);
		}

		@Bean
		ReleaserProperties releaserProperties() {
			return new ReleaserProperties();
		}

	}

}

class MyProjectPostReleaseTask implements ProjectPostReleaseReleaserTask {

	@Override
	public String name() {
		return "post-release-task";
	}

	@Override
	public String shortName() {
		return name();
	}

	@Override
	public String header() {
		return name();
	}

	@Override
	public String description() {
		return name();
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		return ExecutionResult.unstable(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "foo",
				new HttpHeaders(), "".getBytes(), Charset.defaultCharset()));
	}

	@Override
	public int getOrder() {
		return 0;
	}

}

class MyPostTrainReleaseTask implements TrainPostReleaseReleaserTask {

	@Override
	public String name() {
		return "train-post-release-task";
	}

	@Override
	public String shortName() {
		return name();
	}

	@Override
	public String header() {
		return name();
	}

	@Override
	public String description() {
		return name();
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		return ExecutionResult.success();
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
