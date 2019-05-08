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

import java.util.HashMap;
import java.util.Map;

import com.jcabi.github.Github;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.info.exceptions.InitializrParseException;
import org.springframework.cloud.info.exceptions.SpringCloudVersionNotFoundException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ryan Baxter
 */
public class InitializrSpringCloudInfoService extends SpringCloudRelease {

	private static final String INITIALIZR_URL = "https://start.spring.io/actuator/info";

	private RestTemplate rest;

	public InitializrSpringCloudInfoService(RestTemplate rest, Github github,
			GithubPomReader reader) {
		super(github, reader);
		this.rest = rest;
	}

	@Override
	@Cacheable("springCloudViaBoot")
	public SpringCloudVersion getSpringCloudVersion(String springBootVersion)
			throws SpringCloudVersionNotFoundException {
		Map<String, SpringBootAndCloudVersion> cache = new HashMap<>();
		Map<String, Object> response = rest.getForObject(INITIALIZR_URL, Map.class);
		if (!response.containsKey("bom-ranges")) {
			throw new SpringCloudVersionNotFoundException(new InitializrParseException(
					"bom-ranges key not found in Initializr info endpoint"));
		}
		Map<String, Object> bomRanges = (Map<String, Object>) response.get("bom-ranges");
		if (!bomRanges.containsKey("spring-cloud")) {
			throw new SpringCloudVersionNotFoundException(new InitializrParseException(
					"spring-cloud key not found in Initializr info endpoint"));
		}

		Map<String, String> springCloud = (Map<String, String>) bomRanges
				.get("spring-cloud");
		for (String key : springCloud.keySet()) {
			String rangeString = springCloud.get(key);
			cache.put(key, parseRangeString(rangeString, key));
		}
		for (String key : cache.keySet()) {
			if (cache.get(key).matchesSpringBootVersion(springBootVersion)) {
				return new SpringCloudVersion(key);
			}
		}
		throw new SpringCloudVersionNotFoundException(springBootVersion);
	}

	private SpringBootAndCloudVersion parseRangeString(String rangeString,
			String springCloudVersion) {
		// Example of rangeString Spring Boot >=2.0.0.M3 and <2.0.0.M5
		String versions = rangeString.substring(13);
		boolean startVersionInclusive = true;
		if (versions.charAt(0) == '=') {
			versions = versions.substring(1);

		}
		else {
			startVersionInclusive = false;
		}
		// Example of versions 2.0.0.M3 and <2.0.0.M5 or 2.0.0.M3 and <=2.0.0.M5
		String[] cleanedVersions;
		boolean endVersionInclusive = true;
		if (versions.contains("=")) {
			cleanedVersions = versions.split(" and <=");
		}
		else {
			endVersionInclusive = false;
			cleanedVersions = versions.split(" and <");
		}
		if (cleanedVersions.length == 1) {
			return new SpringBootAndCloudVersion(cleanedVersions[0],
					startVersionInclusive, "99999.99999.99999.RELEASE",
					endVersionInclusive, springCloudVersion);
		}
		return new SpringBootAndCloudVersion(cleanedVersions[0], startVersionInclusive,
				cleanedVersions[1], endVersionInclusive, springCloudVersion);
	}

}
