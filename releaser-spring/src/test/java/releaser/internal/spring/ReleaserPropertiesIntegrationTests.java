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

package releaser.internal.spring;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesAware;

import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	List<ReleaserPropertiesAware> propertiesAware;

	@Autowired
	ApplicationContext context;

	@Test
	public void should_update_properties() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getPom().setBranch("fooooo");

		new ReleaserPropertiesUpdater(this.context).updateProperties(properties,
				new File("."));

		BDDAssertions.then(this.propertiesAware).hasSize(2);
		this.propertiesAware.forEach(aware -> BDDAssertions
				.then(((ReleaserPropertiesHaving) aware).properties.getPom().getBranch())
				.isEqualTo("fooooo"));
	}

	@Test
	public void should_update_properties_including_existing_releaser_config() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getPom().setBranch("barrrr");
		URL resource = ReleaserPropertiesIntegrationTests.class
				.getResource("/projects/project-with-config");

		new ReleaserPropertiesUpdater(this.context).updateProperties(properties,
				new File(resource.getFile()));

		BDDAssertions.then(this.propertiesAware).hasSize(2);
		this.propertiesAware.forEach(aware -> {
			ReleaserPropertiesHaving having = ((ReleaserPropertiesHaving) aware);
			BDDAssertions.then(having.properties.getPom().getBranch())
					.isEqualTo("barrrr");
			BDDAssertions.then(having.properties.getMaven().getBuildCommand())
					.isEqualTo("./scripts/noIntegration.sh");
		});
	}

	@Test
	public void should_update_properties_including_existing_releaser_config_for_netflix() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getPom().setBranch("bazzzz");
		URL resource = ReleaserPropertiesIntegrationTests.class
				.getResource("/projects/project-with-netflix-config");

		new ReleaserPropertiesUpdater(this.context).updateProperties(properties,
				new File(resource.getFile()));

		BDDAssertions.then(this.propertiesAware).hasSize(2);
		this.propertiesAware.forEach(aware -> {
			ReleaserPropertiesHaving having = ((ReleaserPropertiesHaving) aware);
			BDDAssertions.then(having.properties.getPom().getBranch())
					.isEqualTo("bazzzz");
			BDDAssertions.then(having.properties.getMaven().getBuildCommand())
					.isEqualTo("./scripts/build.sh  {{systemProps}}");
		});
	}

	@Configuration
	static class Config {

		@Bean
		ReleaserPropertiesAware aware1() {
			return new ReleaserPropertiesHaving();
		}

		@Bean
		ReleaserPropertiesAware aware2() {
			return new ReleaserPropertiesHaving();
		}

	}

	static class ReleaserPropertiesHaving implements ReleaserPropertiesAware {

		ReleaserProperties properties;

		@Override
		public void setReleaserProperties(ReleaserProperties properties) {
			this.properties = properties;
		}

		ReleaserProperties getProps() {
			return this.properties;
		}

	}

}
