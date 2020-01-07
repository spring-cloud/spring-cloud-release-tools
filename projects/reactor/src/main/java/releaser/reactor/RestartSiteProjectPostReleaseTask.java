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

package releaser.reactor;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import releaser.internal.Releaser;
import releaser.internal.spring.Arguments;
import releaser.internal.spring.ExecutionResult;
import releaser.internal.tasks.DryRunReleaseReleaserTask;
import releaser.internal.tasks.release.PublishDocsReleaseTask;

public class RestartSiteProjectPostReleaseTask extends PublishDocsReleaseTask
		implements DryRunReleaseReleaserTask {

	private static final String REACTOR_CORE_PROJECT_NAME = "reactor-core";

	private final CfClient cfClient;

	private final String reactorAppName;

	public RestartSiteProjectPostReleaseTask(Releaser releaser, CfClient cfClient,
			String reactorAppName) {
		super(releaser);
		this.cfClient = cfClient;
		this.reactorAppName = reactorAppName;
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		if (!REACTOR_CORE_PROJECT_NAME.equals(args.projectToRun.name())) {
			return ExecutionResult.success();
		}
		this.cfClient.restartApp(this.reactorAppName);
		return ExecutionResult.success();
	}

}

class CfClient {

	private final CloudFoundryOperations cloudFoundryOperations;

	CfClient(CloudFoundryOperations cloudFoundryOperations) {
		this.cloudFoundryOperations = cloudFoundryOperations;
	}

	void restartApp(String name) {
		this.cloudFoundryOperations.applications()
				.restart(RestartApplicationRequest.builder().name(name).build());
	}

}
