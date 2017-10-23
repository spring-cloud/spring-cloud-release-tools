package org.springframework.cloud.release.internal.sagan;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
class SaganConfiguration {

	@Bean
	SaganClient saganClient(ReleaserProperties properties) {
		RestTemplate restTemplate = restTemplate(properties);
		return new RestTemplateSaganClient(restTemplate, properties);
	}

	private RestTemplate restTemplate(ReleaserProperties properties) {
		return new RestTemplateBuilder()
				.basicAuthorization(properties.getGit().getOauthToken(), "")
				.build();
	}
}
