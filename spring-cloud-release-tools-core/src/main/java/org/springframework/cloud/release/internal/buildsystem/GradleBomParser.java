/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.buildsystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.release.internal.ReleaserProperties;

class GradleBomParser implements BomParser {

	private static final Pattern VERSION_PATTERN = Pattern
			.compile("^([a-zA-Z0-9]+)Version$");

	private final ReleaserProperties properties;

	private final List<CustomBomParser> customParsers;

	GradleBomParser(ReleaserProperties releaserProperties, List<CustomBomParser> customParsers) {
		this.properties = releaserProperties;
		this.customParsers = customParsers;
	}

	@Override
	public boolean isApplicable(File clonedBom) {
		return file(clonedBom, "build.gradle").exists();
	}

	File file(File clonedBom, String child) {
		return new File(clonedBom, child);
	}

	@Override
	public VersionsFromBom versionsFromBom(File thisProjectRoot) {
		File gradleProperties = file(thisProjectRoot, "gradle.properties");
		if (!gradleProperties.exists()) {
			return VersionsFromBom.EMPTY_VERSION;
		}
		Properties properties = loadProps(gradleProperties);
		final Map<String, String> substitution = this.properties.getGradle()
				.getGradlePropsSubstitution();
		VersionsFromBom versionsFromBom = new VersionsFromBomBuilder().releaserProperties(this.properties)
				.parsers(this.customParsers)
				.versionsFromBom();
		properties.forEach((key, value) -> {
			String projectName = projectName(substitution, key);
			versionsFromBom.setVersion(projectName, value.toString());
		});
		return versionsFromBom;
	}

	private String projectName(Map<String, String> substitution, Object key) {
		String projectName = key.toString();
		if (substitution.containsKey(key)) {
			projectName = substitution.get(key);
		}
		else {
			Matcher matcher = VERSION_PATTERN.matcher(projectName);
			boolean versionMatches = matcher.matches();
			if (versionMatches) {
				projectName = matcher.group(1);
			}
		}
		projectName = projectName.replaceAll("([A-Z])", "-$1").toLowerCase();
		return projectName;
	}

	Properties loadProps(File file) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(file));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return props;
	}

}
