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

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("production")
class ReactorConfiguration {

	@Bean
	CfClient cfClient(CloudFoundryOperations cloudFoundryOperations) {
		return new CfClient(cloudFoundryOperations);
	}

	@Bean
	RestartSiteProjectPostReleaseTask restartSiteProjectPostReleaseTask(Releaser releaser, CfClient cfClient,
			@Value("${cf.reactorAppName}") String reactorAppName) {
		return new RestartSiteProjectPostReleaseTask(releaser, cfClient, reactorAppName);
	}

	@Bean
	Github githubClient(ReleaserProperties properties) {
		if (!StringUtils.hasText(properties.getGit().getOauthToken())) {
			throw new BeanInitializationException("You must set the value of the OAuth token. You can do it "
					+ "either via the command line [--releaser.git.oauth-token=...] "
					+ "or put it as an env variable in [~/.bashrc] or "
					+ "[~/.zshrc] e.g. [export RELEASER_GIT_OAUTH_TOKEN=...]");
		}
		return new RtGithub(new RtGithub(properties.getGit().getOauthToken()).entry().through(RetryWire.class));
	}

	@Bean
	GenerateReleaseNotesTask releaseNotesTask(Github github, ProjectGitHandler gitHandler) {
		return new GenerateReleaseNotesTask(github, gitHandler);
	}

}
