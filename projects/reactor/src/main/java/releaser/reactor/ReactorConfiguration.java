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

import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import com.jcabi.http.wire.RetryWire;
import org.cloudfoundry.operations.CloudFoundryOperations;
import releaser.internal.Releaser;
import releaser.internal.ReleaserProperties;
import releaser.internal.git.ProjectGitHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("production")
class ReactorConfiguration {

	@Bean
	CfClient cfClient(CloudFoundryOperations cloudFoundryOperations) {
		return new CfClient(cloudFoundryOperations);
	}

	@Bean
	RestartSiteProjectPostReleaseTask restartSiteProjectPostReleaseTask(Releaser releaser,
			CfClient cfClient, @Value("${cf.reactorAppName}") String reactorAppName) {
		return new RestartSiteProjectPostReleaseTask(releaser, cfClient, reactorAppName);
	}

	@Bean
	Github githubClient(ReleaserProperties properties) {
		return new RtGithub(new RtGithub(properties.getGit().getOauthToken()).entry()
				.through(RetryWire.class));
	}

	@Bean
	GenerateReleaseNotesTask releaseNotesTask(Github github,
			ProjectGitHandler gitHandler) {
		return new GenerateReleaseNotesTask(github, gitHandler);
	}

}
