package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
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

	private static final Logger log = LoggerFactory.getLogger(ReleaserPropertiesUpdater.class);

	private final ApplicationContext context;

	public ReleaserPropertiesUpdater(ApplicationContext context) {
		this.context = context;
	}

	void updateProperties(ReleaserProperties properties, File clonedProjectFromOrg) {
		ReleaserProperties props = updatePropertiesFromFile(properties, clonedProjectFromOrg);
		updateProperties(props);
	}

	void updateProperties(ReleaserProperties props) {
		Map<String, ReleaserPropertiesAware> beans = this.context
				.getBeansOfType(ReleaserPropertiesAware.class);
		beans.values().forEach(aware -> aware.setReleaserProperties(props));
	}

	private ReleaserProperties updatePropertiesFromFile(ReleaserProperties copy,
			File clonedProjectFromOrg) {
		File releaserConfig = new File(clonedProjectFromOrg, "config/releaser.yml");
		if (releaserConfig.exists()) {
			try {
				YamlPropertiesFactoryBean yamlProcessor = new YamlPropertiesFactoryBean();
				yamlProcessor.setResources(new FileSystemResource(releaserConfig));
				Properties properties = yamlProcessor.getObject();
				ReleaserProperties releaserProperties = new Binder(
						new MapConfigurationPropertySource(properties.entrySet().stream().collect(
								Collectors.toMap(
										e -> e.getKey().toString(),
										e -> e.getValue().toString()
								)
						))).bind("releaser", ReleaserProperties.class).get();
				log.info("config/releaser.yml found. Will update the current properties");
				copy.getMaven().setBuildCommand(releaserProperties.getMaven().getBuildCommand());
				copy.getMaven().setDeployCommand(releaserProperties.getMaven().getDeployCommand());
				copy.getGradle().setGradlePropsSubstitution(releaserProperties.getGradle().getGradlePropsSubstitution());
				copy.getGradle().setIgnoredGradleRegex(releaserProperties.getGradle().getIgnoredGradleRegex());
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		log.info("Updating working directory to [{}]", clonedProjectFromOrg.getAbsolutePath());
		copy.setWorkingDir(clonedProjectFromOrg.getAbsolutePath());
		return copy;
	}
}