/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.tasks.release;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReleaseTasksConfiguration {
	@Bean
	@ConditionalOnMissingBean
	BuildProjectReleaseTask buildProjectReleaseTask(Releaser releaser) {
		return new BuildProjectReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	BumpBackToSnapshotReleaseTask bumpBackToSnapshotReleaseTask(Releaser releaser) {
		return new BumpBackToSnapshotReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	CloseMilestonesReleaseTask closeMilestonesReleaseTask(Releaser releaser) {
		return new CloseMilestonesReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	CommitReleaseTask commitReleaseTask(Releaser releaser) {
		return new CommitReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	DeployArtifactsReleaseTask deployArtifactsReleaseTask(Releaser releaser) {
		return new DeployArtifactsReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	PublishDocsReleaseTask publishDocsReleaseTask(Releaser releaser) {
		return new PublishDocsReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	PushChangesReleaseTask pushChangesReleaseTask(Releaser releaser) {
		return new PushChangesReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	UpdatingPomsReleaseTask updatingPomsReleaseTask(Releaser releaser) {
		return new UpdatingPomsReleaseTask(releaser);
	}
}
