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

package org.springframework.cloud.release.internal.buildsystem;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BuildsystemConfiguration {

	@Autowired
	ReleaserProperties releaserProperties;

	@Autowired(required = false)
	List<CustomBomParser> customBomParsers = new ArrayList<>();

	@Bean
	BomParser mavenBomParser() {
		return new MavenBomParser(this.releaserProperties, this.customBomParsers);
	}

	@Bean
	BomParser gradleBomParser() {
		return new MavenBomParser(this.releaserProperties, this.customBomParsers);
	}

	@Bean
	ProjectPomUpdater pomUpdater(List<BomParser> bomParsers) {
		return new ProjectPomUpdater(this.releaserProperties, bomParsers);
	}

}
