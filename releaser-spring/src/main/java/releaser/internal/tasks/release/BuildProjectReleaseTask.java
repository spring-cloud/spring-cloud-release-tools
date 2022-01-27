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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.Releaser;
import releaser.internal.spring.Arguments;
import releaser.internal.tasks.DryRunReleaseReleaserTask;
import releaser.internal.tech.ExecutionResult;

public class BuildProjectReleaseTask implements DryRunReleaseReleaserTask {

	private static final Logger log = LoggerFactory
			.getLogger(BuildProjectReleaseTask.class);

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = 10;

	private final Releaser releaser;

	public BuildProjectReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "build";
	}

	@Override
	public String shortName() {
		return "b";
	}

	@Override
	public String header() {
		return "BUILD PROJECT";
	}

	@Override
	public String description() {
		return "Build the project";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		log.info("Arguments for this release task: " + args);
		return this.releaser.buildProject(args.properties, args.originalVersion,
				args.versionFromBom);
	}

	@Override
	public int getOrder() {
		return BuildProjectReleaseTask.ORDER;
	}

}
