/*
 * Copyright 2013-2020 the original author or authors.
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

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import org.cloudfoundry.uaa.UaaClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Simon Baslé
 */
@Configuration
@Profile("production")
class CfConfiguration {

	@Bean
	DefaultConnectionContext connectionContext(@Value("${cf.apiHost}") String apiHost) {
		return DefaultConnectionContext.builder().apiHost(apiHost).build();
	}

	@Bean
	PasswordGrantTokenProvider tokenProvider(@Value("${cf.username}") String username,
			@Value("${cf.password}") String password) {
		return PasswordGrantTokenProvider.builder().password(password).username(username)
				.build();
	}

	@Bean
	ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder().connectionContext(connectionContext)
				.tokenProvider(tokenProvider).build();
	}

	@Bean
	ReactorDopplerClient dopplerClient(ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		return ReactorDopplerClient.builder().connectionContext(connectionContext)
				.tokenProvider(tokenProvider).build();
	}

	@Bean
	ReactorUaaClient uaaClient(ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		return ReactorUaaClient.builder().connectionContext(connectionContext)
				.tokenProvider(tokenProvider).build();
	}

	@Bean
	DefaultCloudFoundryOperations defaultCloudFoundryOperations(
			CloudFoundryClient cloudFoundryClient, DopplerClient dopplerClient,
			UaaClient uaaClient, @Value("${cf.organization}") String organizationId,
			@Value("${cf.space}") String spaceId) {
		return DefaultCloudFoundryOperations.builder()
				.cloudFoundryClient(cloudFoundryClient).dopplerClient(dopplerClient)
				.uaaClient(uaaClient).organization(organizationId).space(spaceId).build();
	}

}
