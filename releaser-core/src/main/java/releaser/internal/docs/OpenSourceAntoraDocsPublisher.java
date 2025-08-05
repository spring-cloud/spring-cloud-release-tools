/*
 * Copyright 2013-2025 the original author or authors.
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

package releaser.internal.docs;

import java.io.File;

import releaser.internal.ReleaserProperties;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProjectCommandExecutor;

/**
 * @author Ryan Baxter
 */
public class OpenSourceAntoraDocsPublisher implements AntoraDocsPublisher {

	private final ProjectCommandExecutor projectCommandExecutor;

	private final ProjectGitHandler projectGitHandler;

	public OpenSourceAntoraDocsPublisher(ProjectCommandExecutor projectCommandExecutor,
			ProjectGitHandler projectGitHandler) {
		this.projectCommandExecutor = projectCommandExecutor;
		this.projectGitHandler = projectGitHandler;
	}

	@Override
	public void publish(File project, ReleaserProperties properties) {
		File springDocsActionsProject = this.projectGitHandler.cloneAndCheckoutSpringDocsActions();
		this.projectCommandExecutor.publishAntoraDocs(springDocsActionsProject, project, properties);

	}

}
