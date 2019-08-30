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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.release.internal.ReleaserProperties;

class GradleBomParser implements BomParser {

	private final ReleaserProperties properties;

	GradleBomParser(ReleaserProperties releaserProperties) {
		this.properties = releaserProperties;
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
		VersionsFromBom versionsFromBom = new VersionsFromBom(this.properties);
		properties.forEach((key, value) -> {
			String projectName = projectName(substitution, key);
			versionsFromBom.setVersion(projectName, value.toString());
		});
		return versionsFromBom;
	}

	String projectName(Map<String, String> substitution, Object key) {
		String projectName = key.toString();
		if (substitution.containsKey(key)) {
			projectName = substitution.get(key);
		}
		else {
			Pattern versionPattern = Pattern.compile("^([a-zA-Z]+)Version$");
			Matcher matcher = versionPattern.matcher(projectName);
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

	private String asString(Path path) {
		try {
			return new String(Files.readAllBytes(path));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
