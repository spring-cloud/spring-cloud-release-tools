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

package releaser.cloud.github;

import org.kohsuke.github.GitHub;
import releaser.internal.ReleaserProperties;
import releaser.internal.github.CustomGithubIssues;

public class SpringCloudGithubIssuesAccessor {

	public static CustomGithubIssues springCloud(GitHub github, ReleaserProperties releaserProperties) {
		return new SpringCloudGithubIssues(github, releaserProperties);
	}

	public static CustomGithubIssues springCloud(ReleaserProperties releaserProperties) {
		return new SpringCloudGithubIssues(releaserProperties);
	}

}
