package org.springframework.cloud.release.internal.sagan;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.release.internal.ReleaserProperties;
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
	SaganClient saganClient(ReleaserProperties properties) {
		RestTemplate restTemplate = restTemplate(properties);
		return new RestTemplateSaganClient(restTemplate, properties);
	}

	private RestTemplate restTemplate(ReleaserProperties properties) {
		Assert.hasText(properties.getGit().getOauthToken(), "In order to connect to Sagan you need to pass the Github OAuth token");
		return new RestTemplateBuilder()
				.basicAuthorization(properties.getGit().getOauthToken(), "")
				.build();
	}
}
