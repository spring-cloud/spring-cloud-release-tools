/*
 * Copyright 2013-2024 the original author or authors.
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
import releaser.internal.project.ProjectVersion;
import releaser.internal.spring.Arguments;
import releaser.internal.tasks.ProjectPostReleaseReleaserTask;
import releaser.internal.tech.BuildUnstableException;
import releaser.internal.tech.ExecutionResult;

/**
 * @author Ryan Baxter
 */
public class CreateProjectReleaseBundlePostReleaseTask implements ProjectPostReleaseReleaserTask {

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = 60;

	private final Releaser releaser;

	public CreateProjectReleaseBundlePostReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "createReleaseBundle";
	}

	@Override
	public String shortName() {
		return "b";
	}

	@Override
	public String header() {
		return "CREATING RELEASE BUNDLE";
	}

	@Override
	public String description() {
		return "Creates a release bundle for the project";
	}

	@Override
	public ExecutionResult runTask(Arguments args) throws BuildUnstableException {
		return this.releaser.createReleaseBundle(args.properties.isCommercial(), args.versionFromBom,
				args.options.dryRun, args.properties.getBundles().getRepos(),
				new ProjectVersion(args.project).projectName);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
