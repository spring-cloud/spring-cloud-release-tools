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

import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.options.OptionsBuilder;
import releaser.internal.tech.ExecutionResult;

/**
 * Releaser that gets input from console.
 *
 * @author Marcin Grzejszczak
 */
class DefaultSpringReleaser implements SpringReleaser {

	private final ReleaserProperties properties;

	private final OptionsAndPropertiesFactory optionsAndPropertiesFactory;

	private final ProjectsToRunFactory projectsToRunFactory;

	private final TasksToRunFactory tasksToRunFactory;

	private final FlowRunner flowRunner;

	DefaultSpringReleaser(ReleaserProperties properties,
			OptionsAndPropertiesFactory optionsAndPropertiesFactory,
			ProjectsToRunFactory projectsToRunFactory,
			TasksToRunFactory tasksToRunFactory, FlowRunner flowRunner) {
		this.properties = properties;
		this.optionsAndPropertiesFactory = optionsAndPropertiesFactory;
		this.projectsToRunFactory = projectsToRunFactory;
		this.tasksToRunFactory = tasksToRunFactory;
		this.flowRunner = flowRunner;
	}

	/**
	 * Default behaviour - interactive mode.
	 */
	@Override
	public ExecutionResult release() {
		return release(new OptionsBuilder().options());
	}

	@Override
	public ExecutionResult release(Options options) {
		OptionsAndProperties optionsAndProperties = prepareOptionsAndProperties(options,
				this.properties);
		// order matters! Tasks will mutate options and properties
		TasksToRun releaseTasksToRun = releaseTasksFromOptions(optionsAndProperties);
		ProjectsToRun projectsToRun = releaseProjects(optionsAndProperties);
		ExecutionResult releaseTasksExecutionResult = runReleaseTasks(
				optionsAndProperties, projectsToRun, releaseTasksToRun);
		TasksToRun postReleaseTrainTasksToRun = postReleaseTrainTasksFromOptions(
				optionsAndProperties);
		ExecutionResult postReleaseTrainTasksExecutionResult = runPostReleaseTasks(
				optionsAndProperties, postReleaseTrainTasksToRun);
		return releaseTasksExecutionResult.merge(postReleaseTrainTasksExecutionResult);
	}

	private OptionsAndProperties prepareOptionsAndProperties(Options options,
			ReleaserProperties properties) {
		return this.optionsAndPropertiesFactory.get(properties, options);
	}

	private ProjectsToRun releaseProjects(OptionsAndProperties options) {
		return this.projectsToRunFactory.release(options);
	}

	private TasksToRun releaseTasksFromOptions(OptionsAndProperties options) {
		return this.tasksToRunFactory.release(options);
	}

	private TasksToRun postReleaseTrainTasksFromOptions(
			OptionsAndProperties optionsAndProperties) {
		return this.tasksToRunFactory.postRelease(optionsAndProperties.options);
	}

	private ExecutionResult runReleaseTasks(OptionsAndProperties optionsAndProperties,
			ProjectsToRun projectsToRun, TasksToRun tasksToRun) {
		return this.flowRunner.runReleaseTasks(optionsAndProperties.options,
				optionsAndProperties.properties, projectsToRun, tasksToRun);
	}

	private ExecutionResult runPostReleaseTasks(OptionsAndProperties optionsAndProperties,
			TasksToRun postReleaseTasksToRun) {
		return this.flowRunner.runPostReleaseTrainTasks(optionsAndProperties.options,
				optionsAndProperties.properties, "postRelease", postReleaseTasksToRun);
	}

}
