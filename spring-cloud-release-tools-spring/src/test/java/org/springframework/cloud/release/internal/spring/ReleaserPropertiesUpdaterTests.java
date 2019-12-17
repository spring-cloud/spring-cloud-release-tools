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

	@Test
	public void should_update_properties() {
		ReleaserProperties original = originalReleaserProperties();
		Aware aware = new Aware();
		BDDMockito.given(this.context.getBeansOfType(BDDMockito.any(Class.class)))
				.willReturn(beansOfType(aware));
		ReleaserPropertiesUpdater updater = new ReleaserPropertiesUpdater(this.context);

		ReleaserProperties props = updater.updateProperties(original,
				this.relaserUpdater);

		BDDAssertions.then(aware.properties).isNotNull();
		BDDAssertions.then(props.getMaven().getBuildCommand()).isEqualTo("maven_build");
		BDDAssertions.then(props.getGradle().getBuildCommand()).isEqualTo("gradle_build");
		BDDAssertions.then(props.getBash().getBuildCommand()).isEqualTo("bash_build");
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

		@Override
		public void setReleaserProperties(ReleaserProperties properties) {
			this.properties = properties;
		}

	}

}
