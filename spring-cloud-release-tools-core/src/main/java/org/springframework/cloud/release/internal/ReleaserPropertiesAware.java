package org.springframework.cloud.release.internal;

/**
 * @author Marcin Grzejszczak
 */
public interface ReleaserPropertiesAware {

	void setReleaserProperties(ReleaserProperties properties);
}
