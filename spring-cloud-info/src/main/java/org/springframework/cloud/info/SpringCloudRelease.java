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

package org.springframework.cloud.info;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Milestone;
import com.jcabi.http.response.JsonResponse;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.info.exceptions.SpringCloudMilestoneNotFoundException;
import org.springframework.cloud.info.exceptions.SpringCloudVersionNotFoundException;

/**
 * @author Ryan Baxter
 */
public abstract class SpringCloudRelease implements SpringCloudInfoService {

	/**
	 * Coordinates to Spring Cloud Release in Github.
	 */
	public static final String SPRING_CLOUD_RELEASE_COORDINATES = "spring-cloud/spring-cloud-release";

	/**
	 * Github tags path.
	 */
	public static final String SPRING_CLOUD_RELEASE_TAGS_PATH = "repos/"
			+ SPRING_CLOUD_RELEASE_COORDINATES + "/tags";

	private static final String SPRING_CLOUD_RELEASE_RAW = "https://raw.githubusercontent.com/"
			+ SPRING_CLOUD_RELEASE_COORDINATES;

	/**
	 * URL to the raw spring-cloud-dependencies file on Github.
	 */
	public static final String SPRING_CLOUD_RELEASE_DEPENDENCIES_RAW = SPRING_CLOUD_RELEASE_RAW
			+ "/%s/spring-cloud-dependencies/pom.xml";

	/**
	 * URL to the raw spring-cloud-starter-parent file on Github.
	 */
	public static final String SPRING_CLOUD_STARTER_PARENT_RAW = SPRING_CLOUD_RELEASE_RAW
			+ "/%s/spring-cloud-starter-parent/pom.xml";

	private Github github;

	private GithubPomReader reader;

	public SpringCloudRelease(Github github, GithubPomReader reader) {
		this.github = github;
		this.reader = reader;
	}

	@Override
	@Cacheable("springCloudVersions")
	public Collection<String> getSpringCloudVersions() throws IOException {
		List<String> releaseVersions = new ArrayList<>();
		JsonReader reader = github.entry().uri().path(SPRING_CLOUD_RELEASE_TAGS_PATH)
				.back().fetch().as(JsonResponse.class).json();
		JsonArray tags = reader.readArray();
		reader.close();
		List<JsonObject> tagsList = tags.getValuesAs(JsonObject.class);
		for (JsonObject obj : tagsList) {
			releaseVersions.add(obj.getString("name").replaceFirst("v", ""));
		}
		return releaseVersions;
	}

	@Override
	@Cacheable("releaseVersions")
	public Map<String, String> getReleaseVersions(String bomVersion)
			throws SpringCloudVersionNotFoundException, IOException {
		bomVersion = formatBomVersion(bomVersion);
		if (!getSpringCloudVersions().contains(bomVersion)) {
			throw new SpringCloudVersionNotFoundException();
		}
		try {
			Map<String, String> versions = new HashMap<>();
			Model model = reader.readPomFromUrl(
					String.format(SPRING_CLOUD_RELEASE_DEPENDENCIES_RAW, bomVersion));
			for (String name : model.getProperties().stringPropertyNames()) {
				if (name.startsWith("spring-cloud-")) {
					versions.put(name.replace(".version", ""),
							model.getProperties().getProperty(name));
				}
			}
			versions.put("spring-boot", getSpringCloudBootVersion(bomVersion));
			return versions;
		}
		catch (XmlPullParserException e) {
			throw new SpringCloudVersionNotFoundException(e);
		}
	}

	@Override
	@Cacheable("milestones")
	public Collection<String> getMilestones() throws IOException {
		Set<String> milestones = new HashSet<>();
		Iterable<com.jcabi.github.Milestone> githubMilestones = getMilestonesFromGithub();
		for (com.jcabi.github.Milestone milestone : githubMilestones) {
			JsonObject json = milestone.json();
			milestones.add(json.getString("title"));
		}
		return milestones;
	}

	@Override
	@Cacheable("milestoneDueDate")
	public Milestone getMilestoneDueDate(String name)
			throws SpringCloudMilestoneNotFoundException, IOException {
		Iterable<com.jcabi.github.Milestone> milestones = getMilestonesFromGithub();
		for (com.jcabi.github.Milestone milestone : milestones) {
			JsonObject json = milestone.json();
			if (json.getString("title").equalsIgnoreCase(name)) {
				if (json.isNull("due_on")) {
					return new Milestone("No Due Date");
				}
				else {
					Instant instant = Instant.parse(json.getString("due_on"));
					return new Milestone(LocalDateTime
							.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()))
							.toLocalDate().toString());
				}
			}
		}
		throw new SpringCloudMilestoneNotFoundException(name);
	}

	private Iterable<com.jcabi.github.Milestone> getMilestonesFromGithub() {
		Map<String, String> params = new HashMap<>();
		params.put("state", "open");
		return github.repos()
				.get(new Coordinates.Simple(SPRING_CLOUD_RELEASE_COORDINATES))
				.milestones().iterate(params);
	}

	private String getSpringCloudBootVersion(String bomVersion)
			throws IOException, XmlPullParserException {
		Model model = reader.readPomFromUrl(
				String.format(SPRING_CLOUD_STARTER_PARENT_RAW, bomVersion));
		return model.getParent().getVersion();
	}

	private String formatBomVersion(String bomVersion) {
		if (bomVersion.charAt(0) != 'v') {
			return "v" + bomVersion;
		}
		else {
			return bomVersion;
		}
	}

}
