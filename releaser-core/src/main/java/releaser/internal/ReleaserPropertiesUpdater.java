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

package releaser.internal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaserPropertiesUpdater implements Closeable {

	private static final Logger log = LoggerFactory
			.getLogger(ReleaserPropertiesUpdater.class);

	private static final Map<File, ReleaserProperties> CACHE = new ConcurrentHashMap<>();

	public ReleaserProperties updateProperties(ReleaserProperties properties,
			File clonedProjectFromOrg) {
		return CACHE.computeIfAbsent(clonedProjectFromOrg, file -> {
			ReleaserProperties props = updatePropertiesFromFile(properties.copy(), file);
			props.setWorkingDir(clonedProjectFromOrg.getAbsolutePath());
			log.trace("Updated properties [\n\n{}\n\n]", props);
			return props;
		});
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
				overrideProperties(releaserProperties, copy);
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

	private void overrideProperties(ReleaserProperties fromProject,
			ReleaserProperties copy) {
		overrideCommandIfPresent(fromProject.getMaven(), copy.getMaven());
		overrideCommandIfPresent(fromProject.getGradle(), copy.getGradle());
		overrideMapIfPresent(() -> fromProject.getGradle().getGradlePropsSubstitution(),
				s -> copy.getGradle().setGradlePropsSubstitution(s));
		overrideListIfPresent(() -> fromProject.getGradle().getIgnoredGradleRegex(),
				s -> copy.getGradle().setIgnoredGradleRegex(s));
		overrideCommandIfPresent(fromProject.getBash(), copy.getBash());
	}

	private void overrideCommandIfPresent(ReleaserProperties.Command fromProject,
			ReleaserProperties.Command commandCopy) {
		overrideStringIfPresent(fromProject::getBuildCommand,
				commandCopy::setBuildCommand);
		overrideStringIfPresent(fromProject::getDeployCommand,
				commandCopy::setDeployCommand);
		overrideStringIfPresent(fromProject::getGenerateReleaseTrainDocsCommand,
				commandCopy::setGenerateReleaseTrainDocsCommand);
		overrideArrayIfPresent(fromProject::getPublishDocsCommands,
				commandCopy::setPublishDocsCommands);
		overrideStringIfPresent(fromProject::getDeployGuidesCommand,
				commandCopy::setDeployGuidesCommand);
		overrideStringIfPresent(fromProject::getSystemProperties,
				commandCopy::setSystemProperties);
	}

	private void overrideStringIfPresent(Supplier<String> predicate,
			Consumer<String> consumer) {
		if (StringUtils.hasText(predicate.get())) {
			consumer.accept(predicate.get());
		}
	}

	private void overrideArrayIfPresent(Supplier<String[]> predicate,
			Consumer<String[]> consumer) {
		String[] strings = predicate.get();
		if (strings.length > 0) {
			consumer.accept(strings);
		}
	}

	private void overrideMapIfPresent(Supplier<Map<String, String>> predicate,
			Consumer<Map<String, String>> consumer) {
		Map<String, String> strings = predicate.get();
		if (!strings.isEmpty()) {
			consumer.accept(strings);
		}
	}

	private void overrideListIfPresent(Supplier<List<String>> predicate,
			Consumer<List<String>> consumer) {
		List<String> strings = predicate.get();
		if (!strings.isEmpty()) {
			consumer.accept(strings);
		}
	}

	File releaserConfig(File clonedProjectFromOrg) {
		return new File(clonedProjectFromOrg, "config/releaser.yml");
	}

	@Override
	public void close() throws IOException {
		CACHE.clear();
	}

}
