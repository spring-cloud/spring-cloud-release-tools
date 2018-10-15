package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.context.ApplicationContext;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaserPropertiesUpdaterTests {

	ApplicationContext context = BDDMockito.mock(ApplicationContext.class);
	File relaserUpdater;

	public ReleaserPropertiesUpdaterTests() throws URISyntaxException {
		this.relaserUpdater = new File(ReleaserPropertiesUpdaterTests.class
				.getResource("/projects/releaser-updater/").toURI());
	}

	@Test public void should_update_properties() {
		ReleaserProperties original = originalReleaserProperties();
		Aware aware = new Aware();
		BDDMockito.given(this.context.getBeansOfType(BDDMockito.any(Class.class)))
				.willReturn(beansOfType(aware));
		ReleaserPropertiesUpdater updater = new ReleaserPropertiesUpdater(this.context);

		ReleaserProperties props = updater
				.updateProperties(original, this.relaserUpdater);

		BDDAssertions.then(aware.properties).isNotNull();
		BDDAssertions.then(props.getMaven().getSystemProperties()).isEqualTo("-Dfoo=bar");
	}

	ReleaserProperties originalReleaserProperties() {
		ReleaserProperties props = new ReleaserProperties();
		props.getMaven().setSystemProperties("-Dfoo=bar");
		return props;
	}

	private Map<String, Object> beansOfType(Aware aware) {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", aware);
		return map;
	}

	class Aware implements ReleaserPropertiesAware {

		ReleaserProperties properties;

		@Override public void setReleaserProperties(ReleaserProperties properties) {
			this.properties = properties;
		}
	}
}