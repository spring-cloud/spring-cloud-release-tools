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

package releaser.internal.versions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.spring.initializr.metadata.BillOfMaterials;
import io.spring.initializr.metadata.InitializrProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesAware;
import releaser.internal.buildsystem.ProjectPomUpdater;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.StringUtils;

/**
 * Knows how to fetch the data related to versions. E.g. the latest release train, is this
 * project's version the latest one etc.
 *
 * @author Marcin Grzejszczak
 */
public class VersionsFetcher implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory.getLogger(VersionsFetcher.class);

	private final ProjectPomUpdater projectPomUpdater;

	private final ToPropertiesConverter toPropertiesConverter;

	private ReleaserProperties properties;

	public VersionsFetcher(ReleaserProperties properties,
			ProjectPomUpdater projectPomUpdater) {
		this.properties = properties;
		this.projectPomUpdater = projectPomUpdater;
		this.toPropertiesConverter = new ToPropertiesConverter(new RawGithubRetriever());
	}

	/**
	 * Checks if the given project version is the latest GA available.
	 * @param version version to check
	 * @return {@code true} if this version is the latest GA
	 */
	public boolean isLatestGa(ProjectVersion version) {
		if (!version.isReleaseOrServiceRelease()) {
			if (log.isDebugEnabled()) {
				log.debug("Version [" + version.toString()
						+ "] is non GA, will not fetch any versions.");
			}
			return false;
		}
		if (!this.properties.getGit().isUpdateSpringGuides()) {
			log.info("Will not file an issue to Spring Guides, since the switch to do so "
					+ "is off. Set [releaser.git.update-spring-guides] to [true] to change that");
			return false;
		}
		String latestVersionsUrl = this.properties.getVersions().getAllVersionsFileUrl();
		InitializrProperties initializrProperties = this.toPropertiesConverter
				.toProperties(latestVersionsUrl);
		if (initializrProperties == null) {
			return false;
		}
		ProjectVersion bomVersion = latestBomVersion();
		if (bomVersion == null) {
			log.info("No BOM mapping with name [{}] found",
					this.properties.getVersions().getBomName());
			return false;
		}
		Projects projectVersions = null;
		try {
			projectVersions = this.projectPomUpdater.retrieveVersionsFromReleaseTrainBom(
					"v" + bomVersion.toString(), false);
		}
		catch (Exception ex) {
			log.error(
					"Failed to check the project versions. Will return that the project is not GA",
					ex);
			return false;
		}
		boolean containsProject = projectVersions.containsProject(version.projectName);
		if (containsProject) {
			return bomVersion.compareTo(projectVersions.forName(version.projectName)) > 0;
		}
		log.info("The project [" + version.projectName
				+ "] is not present in the BOM with projects [" + projectVersions.stream()
						.map(v -> v.projectName).collect(Collectors.joining(", "))
				+ "]");
		return false;
	}

	private ProjectVersion latestBomVersion() {
		String latestVersionsUrl = this.properties.getVersions().getAllVersionsFileUrl();
		InitializrProperties initializrProperties = this.toPropertiesConverter
				.toProperties(latestVersionsUrl);
		if (initializrProperties == null) {
			return null;
		}
		ProjectVersion springCloudVersion = initializrProperties.getEnv().getBoms()
				.getOrDefault(
						this.properties.getVersions().getBomName(), new BillOfMaterials())
				.getMappings().stream()
				.map(mapping -> new ProjectVersion(
						this.properties.getVersions().getBomName(), mapping.getVersion()))
				.filter(ProjectVersion::isReleaseOrServiceRelease)
				.max(ProjectVersion::compareTo).orElse(new ProjectVersion(
						this.properties.getVersions().getBomName(), ""));
		log.info("Latest BOM version is [{}]", springCloudVersion.version);
		return springCloudVersion;
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}

class RawGithubRetriever {

	private static final Logger LOG = LoggerFactory.getLogger(RawGithubRetriever.class);

	String raw(String stringUrl) {
		try {
			URL url = new URL(stringUrl);
			URLConnection con = url.openConnection();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
					con.getInputStream(), StandardCharsets.UTF_8))) {
				return reader.lines().collect(Collectors.joining("\n"));
			}
		}
		catch (IOException e) {
			LOG.warn("Exception occurred while trying to fetch the URL [" + stringUrl
					+ "] contents");
			return null;
		}
	}

}

class ToPropertiesConverter {

	private static final Map<String, InitializrProperties> CACHE = new ConcurrentHashMap<>();

	private final RawGithubRetriever rawGithubRetriever;

	ToPropertiesConverter(RawGithubRetriever rawGithubRetriever) {
		this.rawGithubRetriever = rawGithubRetriever;
	}

	InitializrProperties toProperties(String url) {
		return CACHE.computeIfAbsent(url, s -> {
			String retrievedFile = this.rawGithubRetriever.raw(s);
			if (StringUtils.isEmpty(retrievedFile)) {
				return null;
			}
			YamlPropertiesFactoryBean yamlProcessor = new YamlPropertiesFactoryBean();
			yamlProcessor.setResources(new InputStreamResource(new ByteArrayInputStream(
					retrievedFile.getBytes(StandardCharsets.UTF_8))));
			Properties properties = yamlProcessor.getObject();
			return new Binder(
					new MapConfigurationPropertySource(properties.entrySet().stream()
							.collect(Collectors.toMap(e -> e.getKey().toString(),
									e -> e.getValue().toString()))))
											.bind("initializr",
													InitializrProperties.class)
											.get();
		});
	}

}
