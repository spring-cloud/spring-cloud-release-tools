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

package releaser.internal.github;

import java.util.List;

import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
class GithubIssues {

	private final List<CustomGithubIssues> customGithubIssues;

	GithubIssues(List<CustomGithubIssues> customGithubIssues) {
		this.customGithubIssues = customGithubIssues;
	}

	private CustomGithubIssues customGithubIssues() {
		return this.customGithubIssues.isEmpty() ? CustomGithubIssues.NO_OP : this.customGithubIssues.get(0);
	}

	void fileIssueInSpringGuides(Projects projects, ProjectVersion version) {
		customGithubIssues().fileIssueInSpringGuides(projects, version);
	}

	void fileIssueInStartSpringIo(Projects projects, ProjectVersion version) {
		customGithubIssues().fileIssueInStartSpringIo(projects, version);
	}

}
