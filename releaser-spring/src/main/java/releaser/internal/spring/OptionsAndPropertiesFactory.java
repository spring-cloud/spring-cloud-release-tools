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

package releaser.internal.spring;

import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;

class OptionsAndPropertiesFactory {

	OptionsAndProperties get(ReleaserProperties properties, Options options) {
		if (options.metaRelease) {
			properties.getGit().setFetchVersionsFromGit(false);
			properties.getMetaRelease().setEnabled(true);
			options.fullRelease = true;
		}
		return new OptionsAndProperties(properties, options);
	}

}
