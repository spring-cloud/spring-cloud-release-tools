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

package releaser.internal.buildsystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import releaser.internal.ReleaserProperties;

class GradleBomParser implements BomParser {

	private final ReleaserProperties properties;

	private final List<CustomBomParser> customParsers;

	private final GradleProjectNameExtractor extractor = new GradleProjectNameExtractor();

	GradleBomParser(ReleaserProperties releaserProperties,
			List<CustomBomParser> customParsers) {
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
		VersionsFromBom versionsFromBom = new VersionsFromBomBuilder()
				.thisProjectRoot(thisProjectRoot).releaserProperties(this.properties)
				.parsers(this.customParsers).retrieveFromBom();
		properties.forEach((key, value) -> {
			String projectName = this.extractor.projectName(substitution, key);
			versionsFromBom.setVersion(projectName, value.toString());
		});
		return versionsFromBom;
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

	@Override
	public List<CustomBomParser> customBomParsers() {
		return this.customParsers;
	}

}
