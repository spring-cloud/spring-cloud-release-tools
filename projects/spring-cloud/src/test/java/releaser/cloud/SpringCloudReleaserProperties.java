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

package releaser.cloud;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.stream.Collectors;

import releaser.internal.ReleaserProperties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.FileSystemResource;

public class SpringCloudReleaserProperties {

	public static ReleaserProperties get() {
		try {
			File releaserConfig = new File(SpringCloudReleaserProperties.class.getResource("/application.yml").toURI());
			YamlPropertiesFactoryBean yamlProcessor = new YamlPropertiesFactoryBean();
			yamlProcessor.setResources(new FileSystemResource(releaserConfig));
			Properties properties = yamlProcessor.getObject();
			ReleaserProperties releaserProperties = new Binder(new MapConfigurationPropertySource(properties.entrySet()
					.stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()))))
							.bind("releaser", ReleaserProperties.class).get();
			return releaserProperties;
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}

	}

}
