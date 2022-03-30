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

import java.util.ArrayList;
import java.util.List;

import releaser.internal.ReleaserProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BuildsystemConfiguration {

	@Bean
	@ConditionalOnMissingBean
	BomParser mavenBomParser(ReleaserProperties releaserProperties,
			@Autowired(required = false) List<CustomBomParser> customBomParsers) {
		return new MavenBomParser(releaserProperties, customBomParsers(customBomParsers));
	}

	private List<CustomBomParser> customBomParsers(List<CustomBomParser> customBomParsers) {
		return customBomParsers == null ? new ArrayList<>() : customBomParsers;
	}

	@Bean
	@ConditionalOnMissingBean
	BomParser gradleBomParser(ReleaserProperties releaserProperties,
			@Autowired(required = false) List<CustomBomParser> customBomParsers) {
		return new GradleBomParser(releaserProperties, customBomParsers(customBomParsers));
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectPomUpdater pomUpdater(ReleaserProperties releaserProperties, List<BomParser> bomParsers) {
		return new ProjectPomUpdater(releaserProperties, bomParsers);
	}

}
