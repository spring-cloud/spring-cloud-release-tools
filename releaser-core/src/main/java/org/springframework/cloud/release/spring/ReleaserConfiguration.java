package org.springframework.cloud.release.spring;

/**
 * @author Marcin Grzejszczak
 */

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.release.internal.ProjectUpdater;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReleaserProperties.class)
class ReleaserConfiguration {

	@Bean ProjectUpdater projectUpdater(ReleaserProperties properties) {
		return new ProjectUpdater(properties);
	}
}
