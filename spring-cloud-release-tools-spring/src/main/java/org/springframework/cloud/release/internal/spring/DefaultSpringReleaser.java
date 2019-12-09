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

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;

/**
 * Releaser that gets input from console.
 *
 * @author Marcin Grzejszczak
 */
public class DefaultSpringReleaser implements SpringReleaser {

	private final ReleaserProperties properties;

	private final OptionsAndPropertiesFactory optionsAndPropertiesFactory;

	private final ProjectsToRunFactory projectsToRunFactory;

	private final TasksToRunFactory tasksToRunFactory;

	private final FlowRunner flowRunner;

	public DefaultSpringReleaser(ReleaserProperties properties,
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

	// SETUP
	// from options, modify the releaser properties

	// RELEASE
	// which projects to run
	// * for meta-release take from the options
	// * for single will return a list of one project
	// which tasks to run
	// * for meta-release will run everything
	// * for single will take from the options
	// POST-RELEASE
	// not applicable for dry-run
	// * for single project will run the project post-release tasks
	// * for meta-release will pick projects, set the branch to release train version

	// **** take from the options
	// * Start from
	// * Range
	// * Task names
	// * Interactive
	// * In this mode you haven’t picked what you want to do yet
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
		ExecutionResult postReleaseTasksExecutionResult = runPostReleaseTasks(
				optionsAndProperties, postReleaseTrainTasksToRun);
		return releaseTasksExecutionResult.merge(postReleaseTasksExecutionResult);
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
		return this.flowRunner.runPostReleaseTasks(optionsAndProperties.options,
				optionsAndProperties.properties, "postRelease", postReleaseTasksToRun);
	}

}
