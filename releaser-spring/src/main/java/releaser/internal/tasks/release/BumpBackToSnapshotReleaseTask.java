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
import releaser.internal.tasks.ReleaseReleaserTask;
import releaser.internal.tech.ExecutionResult;

public class BumpBackToSnapshotReleaseTask implements ReleaseReleaserTask {

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = 70;

	private final Releaser releaser;

	public BumpBackToSnapshotReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "snapshots";
	}

	@Override
	public String shortName() {
		return "s";
	}

	@Override
	public String header() {
		return "REVERTING CHANGES & BUMPING VERSION (RELEASE ONLY)";
	}

	@Override
	public String description() {
		return "Go back to snapshots and bump originalVersion by patch";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		return this.releaser.rollbackReleaseVersion(args.project, args.projects, args.versionFromBom);
	}

	@Override
	public int getOrder() {
		return BumpBackToSnapshotReleaseTask.ORDER;
	}

}
