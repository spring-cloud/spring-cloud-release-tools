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

package org.springframework.cloud.release.internal.tasks.postrelease;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.spring.ExecutionResult;
import org.springframework.cloud.release.internal.tasks.TrainPostReleaseReleaserTask;

public class UpdateReleaseTrainDocsTrainPostReleaseTask
		implements TrainPostReleaseReleaserTask {

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = 120;

	private final Releaser releaser;

	public UpdateReleaseTrainDocsTrainPostReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "updateReleaseTrainDocs";
	}

	@Override
	public String shortName() {
		return "ur";
	}

	@Override
	public String header() {
		return "UPDATE RELEASE TRAIN DOCS";
	}

	@Override
	public String description() {
		return "Updating release train documentation";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		this.releaser.generateReleaseTrainDocumentation(args.projects);
		return ExecutionResult.success();
	}

	@Override
	public int getOrder() {
		return UpdateReleaseTrainDocsTrainPostReleaseTask.ORDER;
	}

}
