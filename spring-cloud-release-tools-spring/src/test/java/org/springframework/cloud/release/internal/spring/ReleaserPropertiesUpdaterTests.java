package org.springframework.cloud.release.internal.spring;

import java.io.File;
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

	@Test public void should_update_properties() {
		Aware aware = new Aware();
		BDDMockito.given(this.context.getBeansOfType(BDDMockito.any(Class.class)))
				.willReturn(beansOfType(aware));
		ReleaserPropertiesUpdater updater = new ReleaserPropertiesUpdater(this.context);

		updater.updateProperties(new ReleaserProperties(), new File("."));

		BDDAssertions.then(aware.properties).isNotNull();
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