package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.context.ApplicationContext;

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
		Map<String, ReleaserPropertiesAware> beans = this.context
				.getBeansOfType(ReleaserPropertiesAware.class);
		beans.values().forEach(aware -> aware.setReleaserProperties(props));
	}

	private ReleaserProperties updatePropertiesFromFile(ReleaserProperties copy,
			File clonedProjectFromOrg) {
		File releaserConfig = new File(clonedProjectFromOrg, "config/releaser.yml");
		if (releaserConfig.exists()) {
			try {
				Object read = new YamlReader(new FileReader(releaserConfig)).read();
				ReleaserProperties releaserProperties = new Binder(
						new MapConfigurationPropertySource((Map) read))
						.bind("releaser", ReleaserProperties.class).get();
				log.info("config/releaser.yml found. Will update the current properties");
				copy.setMaven(releaserProperties.getMaven());
				copy.setGradle(releaserProperties.getGradle());
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