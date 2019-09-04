/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.buildsystem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.release.internal.ReleaserProperties;

class GradleBomParserTests {

	@Test
	void should_read_versions_from_bom_from_properties() {
		GradleBomParser parser = new GradleBomParser(new ReleaserProperties(),
				new ArrayList<>()) {
			@Override
			public boolean isApplicable(File clonedBom) {
				return true;
			}

			@Override
			File file(File clonedBom, String child) {
				return clonedBom;
			}

			@Override
			Properties loadProps(File file) {
				Properties properties = new Properties();
				properties.setProperty("springCloudContractVersion", "1.0.0.RELEASE");
				return properties;
			}
		};

		VersionsFromBom versionsFromBom = parser.versionsFromBom(new File("."));

		BDDAssertions.then(versionsFromBom.versionForProject("spring-cloud-contract"))
				.isEqualTo("1.0.0.RELEASE");
	}

	@Test
	void should_return_a_version_from_bom_with_substitution() {
		Map<String, String> gradleSubstitution = new HashMap<>();
		gradleSubstitution.put("verifierVersion", "spring-cloud-contract");
		ReleaserProperties releaserProperties = new ReleaserProperties();
		releaserProperties.getGradle().setGradlePropsSubstitution(gradleSubstitution);
		GradleBomParser parser = new GradleBomParser(releaserProperties,
				new ArrayList<>()) {
			@Override
			public boolean isApplicable(File clonedBom) {
				return true;
			}

			@Override
			File file(File clonedBom, String child) {
				return clonedBom;
			}

			@Override
			Properties loadProps(File file) {
				Properties properties = new Properties();
				properties.setProperty("verifierVersion", "1.0.0.RELEASE");
				return properties;
			}
		};

		VersionsFromBom versionsFromBom = parser.versionsFromBom(new File("."));

		BDDAssertions.then(versionsFromBom.versionForProject("spring-cloud-contract"))
				.isEqualTo("1.0.0.RELEASE");
	}

	@Test
	void should_be_not_applicable_when_no_build_gradle_is_present() {
		GradleBomParser parser = new GradleBomParser(new ReleaserProperties(),
				new ArrayList<>());

		BDDAssertions.then(parser.isApplicable(new File("."))).isFalse();
	}

	@Test
	void should_be_applicable_when_build_gradle_is_present() {
		GradleBomParser parser = new GradleBomParser(new ReleaserProperties(),
				new ArrayList<>()) {
			@Override
			File file(File clonedBom, String child) {
				return clonedBom;
			}
		};

		BDDAssertions.then(parser.isApplicable(new File("."))).isTrue();
	}

	@Test
	void should_return_empty_version_when_no_gradle_properties_is_present() {
		GradleBomParser parser = new GradleBomParser(new ReleaserProperties(),
				new ArrayList<>());

		VersionsFromBom versionsFromBom = parser.versionsFromBom(new File("."));

		BDDAssertions.then(versionsFromBom).isSameAs(VersionsFromBom.EMPTY_VERSION);
	}

}