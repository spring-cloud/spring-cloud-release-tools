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
import org.springframework.cloud.release.internal.tasks.ReleaserTask;

/**
 * Knows how to run a flow of tasks.
 */
public interface FlowRunner {

	/**
	 * Decide what to do before running a task.
	 * @param options - options prepared for the task
	 * @param properties - specific releaser properties for this task
	 * @param releaserTask - the task to run
	 * @return the decision on what to do with the task
	 */
	default Decision beforeTask(Options options, ReleaserProperties properties,
			ReleaserTask releaserTask) {
		return Decision.CONTINUE;
	}

	/**
	 * Decide what to do after running a task.
	 * @param options - options prepared for the task
	 * @param properties - specific releaser properties for this task
	 * @param releaserTask - the task to run
	 * @return the decision on what to do with the task
	 */
	default Decision afterTask(Options options, ReleaserProperties properties,
			ReleaserTask releaserTask) {
		return Decision.CONTINUE;
	}

	/**
	 * Runs the release tasks.
	 * @param options - options coming from the input
	 * @param properties - releaser properties coming from the input
	 * @param projectToRuns - set of projects to run (for a single release it's simple)
	 * @param tasksToRun - set of release tasks to run for each project
	 * @return the result of release execution
	 */
	ExecutionResult runReleaseTasks(Options options, ReleaserProperties properties,
			ProjectsToRun projectToRuns, TasksToRun tasksToRun);

	/**
	 * Runs the post release tasks.
	 * @param options - options coming from the input
	 * @param properties - releaser properties coming from the input
	 * @param executingTaskName - name for the post release task
	 * @param tasksToRun - set of post release tasks to run for each project
	 * @return the result of release execution
	 */
	ExecutionResult runPostReleaseTasks(Options options, ReleaserProperties properties,
			String executingTaskName, TasksToRun tasksToRun);

	/**
	 * Decision to be taken before and after running a task.
	 */
	enum Decision {

		/**
		 * Allows to continue to the next task.
		 */
		CONTINUE,

		/**
		 * Skips the current task and goes to the next one.
		 */
		SKIP,

		/**
		 * Aborts the execution of the whole release.
		 */
		ABORT

	}

}
