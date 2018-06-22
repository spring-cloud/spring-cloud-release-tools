package org.springframework.cloud.release.internal.spring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.release.internal.ReleaserApplication;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@Import(ReleaserPropertiesIntegrationTests.Config.class)
public class ReleaserPropertiesIntegrationTests {

	@Autowired List<ReleaserPropertiesAware> propertiesAware;
	@Autowired ApplicationContext context;
	
	@Test public void should_update_properties() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getPom().setBranch("fooooo");
		
		new ReleaserPropertiesUpdater(this.context).updateProperties(properties);

		BDDAssertions.then(this.propertiesAware).hasSize(2);
		this.propertiesAware.forEach(aware ->
				BDDAssertions.then(((ReleaserPropertiesHaving) aware)
						.properties.getPom().getBranch()).isEqualTo("fooooo"));
	}

	@Configuration
	static class Config {
		@Bean ReleaserPropertiesAware aware1() {
			return new ReleaserPropertiesHaving();
		}
		@Bean ReleaserPropertiesAware aware2() {
			return new ReleaserPropertiesHaving();
		}
	}

	static class ReleaserPropertiesHaving implements ReleaserPropertiesAware {

		ReleaserProperties properties;

		@Override public void setReleaserProperties(ReleaserProperties properties) {
			this.properties = properties;
		}

		ReleaserProperties getProps() {
			return this.properties;
		}
	}
}