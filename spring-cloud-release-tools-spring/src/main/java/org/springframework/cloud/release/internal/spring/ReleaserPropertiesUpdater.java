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
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Marcin Grzejszczak
 */
class ReleaserPropertiesUpdater {

	private static final Logger log = LoggerFactory
			.getLogger(ReleaserPropertiesUpdater.class);

	private final ApplicationContext context;

	ReleaserPropertiesUpdater(ApplicationContext context) {
		this.context = context;
	}

	ReleaserProperties updateProperties(ReleaserProperties properties,
			File clonedProjectFromOrg) {
		ReleaserProperties props = updatePropertiesFromFile(properties,
				clonedProjectFromOrg);
		log.info("Updated properties [\n\n{}\n\n]", props);
		updateProperties(props);
		return props;
	}

	void updateProperties(ReleaserProperties props) {
		Map<String, ReleaserPropertiesAware> beans = this.context
				.getBeansOfType(ReleaserPropertiesAware.class);
		beans.values().forEach(aware -> aware.setReleaserProperties(props));
	}

	private ReleaserProperties updatePropertiesFromFile(ReleaserProperties copy,
			File clonedProjectFromOrg) {
		File releaserConfig = releaserConfig(clonedProjectFromOrg);
		if (releaserConfig.exists()) {
			try {
				YamlPropertiesFactoryBean yamlProcessor = new YamlPropertiesFactoryBean();
				yamlProcessor.setResources(new FileSystemResource(releaserConfig));
				Properties properties = yamlProcessor.getObject();
				ReleaserProperties releaserProperties = new Binder(
						new MapConfigurationPropertySource(properties.entrySet().stream()
								.collect(Collectors.toMap(e -> e.getKey().toString(),
										e -> e.getValue().toString()))))
												.bind("releaser",
														ReleaserProperties.class)
												.get();
				log.info("config/releaser.yml found. Will update the current properties");
				copy.getMaven()
						.setBuildCommand(releaserProperties.getMaven().getBuildCommand());
				copy.getMaven().setDeployCommand(
						releaserProperties.getMaven().getDeployCommand());
				copy.getGradle().setGradlePropsSubstitution(
						releaserProperties.getGradle().getGradlePropsSubstitution());
				copy.getGradle().setIgnoredGradleRegex(
						releaserProperties.getGradle().getIgnoredGradleRegex());
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		else {
			log.info(
					"No config/releaser.yml found. Will NOT update the current properties");
		}
		log.info("Updating working directory to [{}]",
				clonedProjectFromOrg.getAbsolutePath());
		copy.setWorkingDir(clonedProjectFromOrg.getAbsolutePath());
		return copy;
	}

	File releaserConfig(File clonedProjectFromOrg) {
		return new File(clonedProjectFromOrg, "config/releaser.yml");
	}

}
