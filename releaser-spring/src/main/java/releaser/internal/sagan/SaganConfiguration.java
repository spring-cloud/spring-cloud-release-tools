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

package releaser.internal.sagan;

import java.util.List;

import releaser.internal.ReleaserProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
class SaganConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "releaser.sagan.update-sagan", havingValue = "true")
	SaganClient saganClient(ReleaserProperties properties) {
		RestTemplate restTemplate = restTemplate(properties);
		return new RestTemplateSaganClient(restTemplate, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "releaser.sagan.update-sagan", havingValue = "false", matchIfMissing = true)
	SaganClient noOpSaganClient() {
		return new SaganClient() {
			@Override
			public Project getProject(String projectName) {
				return null;
			}

			@Override
			public Release getRelease(String projectName, String releaseVersion) {
				return null;
			}

			@Override
			public Release deleteRelease(String projectName, String releaseVersion) {
				return null;
			}

			@Override
			public Project updateRelease(String projectName, List<ReleaseUpdate> releaseUpdate) {
				return null;
			}

			@Override
			public Project patchProject(Project project) {
				return null;
			}
		};
	}

	private RestTemplate restTemplate(ReleaserProperties properties) {
		Assert.hasText(properties.getGit().getOauthToken(),
				"In order to connect to Sagan you need to pass the Github OAuth token. "
						+ "You can do it via the [--releaser.git.oauth-token=...] "
						+ "command line argument or an env variable [export RELEASER_GIT_OAUTH_TOKEN=...].");
		return new RestTemplateBuilder().basicAuthentication(properties.getGit().getOauthToken(), "").build();
	}

}
