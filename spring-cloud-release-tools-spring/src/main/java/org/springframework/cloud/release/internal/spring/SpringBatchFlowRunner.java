/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;

class SpringBatchFlowRunner implements FlowRunner {

	private static final Logger log = LoggerFactory.getLogger(SpringBatchFlowRunner.class);

	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";
	static StepSkipper stepSkipper = new ConsoleInputStepSkipper();

	@Override
	public Decision beforeTask(Options options, ReleaserProperties properties, ReleaserTask releaserTask) {
		return decide(options, releaserTask);
	}

	@Override
	public void executeTasksForProjects(Options options, ReleaserProperties properties, ProjectsToRun projectToRuns, TasksToRun tasksToRun) {

	}

	@Override
	public void executePostReleaseTasks(Options options, ReleaserProperties properties, TasksToRun tasksToRun) {

	}

	private Decision decide(Options options, ReleaserTask task) {
		boolean interactive = options.interactive;
		printLog(interactive, task);
		if (interactive) {
			boolean skipStep = stepSkipper.skipStep();
			return skipStep ? Decision.SKIP : Decision.CONTINUE;
		}
		return Decision.CONTINUE;
	}

	private void printLog(boolean interactive, ReleaserTask task) {
		log.info("\n\n\n=== {} ===\n\n{} {}\n\n", task.header(), task.description(),
				interactive ? MSG : "");
	}
}
