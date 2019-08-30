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

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;

class CompositeBomParser implements BomParser, ReleaserPropertiesAware {

	private ReleaserProperties properties;

	CompositeBomParser(ReleaserProperties releaserProperties) {
		this.properties = releaserProperties;
	}

	@Override
	public boolean isApplicable(File clonedBom) {
		return new MavenBomParser(this.properties).isApplicable(clonedBom)
				|| new GradleBomParser().isApplicable(clonedBom);
	}

	@Override
	public VersionsFromBom versionsFromBom(File thisProjectRoot) {
		return firstMatching(thisProjectRoot).versionsFromBom(thisProjectRoot);
	}

	private BomParser firstMatching(File thisProjectRoot) {
		BomParser gradle = new GradleBomParser();
		if (new GradleBomParser().isApplicable(thisProjectRoot)) {
			return gradle;
		}
		return new MavenBomParser(this.properties);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}
