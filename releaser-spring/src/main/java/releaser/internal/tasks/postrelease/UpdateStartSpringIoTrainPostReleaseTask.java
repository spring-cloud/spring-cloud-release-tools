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

package releaser.internal.tasks.postrelease;

import releaser.internal.Releaser;
import releaser.internal.spring.Arguments;
import releaser.internal.tasks.TrainPostReleaseReleaserTask;
import releaser.internal.tech.ExecutionResult;

public class UpdateStartSpringIoTrainPostReleaseTask implements TrainPostReleaseReleaserTask {

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = 90;

	private final Releaser releaser;

	public UpdateStartSpringIoTrainPostReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "updateStartSpringIo";
	}

	@Override
	public String shortName() {
		return "us";
	}

	@Override
	public String header() {
		return "UPDATE START.SPRING.IO";
	}

	@Override
	public String description() {
		return "Updating start.spring.io";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		return this.releaser.updateStartSpringIo(args.versionFromBom, args.projects);
	}

	@Override
	public int getOrder() {
		return UpdateStartSpringIoTrainPostReleaseTask.ORDER;
	}

}
