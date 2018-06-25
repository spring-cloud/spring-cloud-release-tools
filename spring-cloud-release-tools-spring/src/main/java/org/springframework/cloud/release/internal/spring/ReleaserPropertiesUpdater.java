package org.springframework.cloud.release.internal.spring;

import java.util.Map;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.context.ApplicationContext;

/**
 * @author Marcin Grzejszczak
 */
class ReleaserPropertiesUpdater {
	private final ApplicationContext context;

	public ReleaserPropertiesUpdater(ApplicationContext context) {
		this.context = context;
	}

	public void updateProperties(ReleaserProperties properties) {
		Map<String, ReleaserPropertiesAware> beans = this.context
				.getBeansOfType(ReleaserPropertiesAware.class);
		beans.values().forEach(aware -> aware.setReleaserProperties(properties));
	}
}
