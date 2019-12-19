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

package releaser.internal.tasks.release;

import releaser.internal.Releaser;
import releaser.internal.spring.Arguments;
import releaser.internal.spring.ExecutionResult;
import releaser.internal.tasks.ReleaseReleaserTask;

public class PushChangesReleaseTask implements ReleaseReleaserTask {

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = 60;

	private final Releaser releaser;

	public PushChangesReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "push";
	}

	@Override
	public String shortName() {
		return "p";
	}

	@Override
	public String header() {
		return "PUSHING CHANGES";
	}

	@Override
	public String description() {
		return "Push the commits";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		this.releaser.pushCurrentBranch(args.project);
		return ExecutionResult.success();
	}

	@Override
	public int getOrder() {
		return PushChangesReleaseTask.ORDER;
	}

}
