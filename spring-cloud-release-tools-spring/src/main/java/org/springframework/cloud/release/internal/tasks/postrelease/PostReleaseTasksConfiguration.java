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

package org.springframework.cloud.release.internal.tasks.postrelease;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PostReleaseTasksConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CreateTemplatesTrainPostReleaseTask createTemplatesTrainPostReleaseTask(
			Releaser releaser) {
		return new CreateTemplatesTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.run-updated-samples")
	RunUpdatedSamplesTrainPostReleaseTask runUpdatedSamplesTrainPostReleaseTask(
			Releaser releaser) {
		return new RunUpdatedSamplesTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-all-test-samples")
	UpdateAllTestSamplesTrainPostReleaseTask updateAllTestSamplesTrainPostReleaseTask(
			Releaser releaser) {
		return new UpdateAllTestSamplesTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-documentation-repo")
	UpdateDocsRepositoryProjectPostReleaseTask updateDocsRepositoryProjectPostReleaseTask(
			Releaser releaser) {
		return new UpdateDocsRepositoryProjectPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-documentation-repo")
	UpdateDocsRepositoryTrainPostReleaseTask updateDocsRepositoryTrainPostReleaseTask(
			Releaser releaser) {
		return new UpdateDocsRepositoryTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-guides-repo")
	UpdateGuidesTrainPostReleaseTask updateGuidesTrainPostReleaseTask(Releaser releaser) {
		return new UpdateGuidesTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-release-train-docs")
	UpdateReleaseTrainDocsTrainPostReleaseTask updateReleaseTrainDocsTrainPostReleaseTask(
			Releaser releaser) {
		return new UpdateReleaseTrainDocsTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-release-train-wiki")
	UpdateReleaseTrainWikiTrainPostReleaseTask updateReleaseTrainWikiTrainPostReleaseTask(
			Releaser releaser) {
		return new UpdateReleaseTrainWikiTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.sagan.update-sagan")
	UpdateSaganProjectPostReleaseTask updateSaganProjectPostReleaseTask(
			Releaser releaser) {
		return new UpdateSaganProjectPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.sagan.update-start-spring-io")
	UpdateStartSpringIoTrainPostReleaseTask updateStartSpringIoTrainPostReleaseTask(
			Releaser releaser) {
		return new UpdateStartSpringIoTrainPostReleaseTask(releaser);
	}

}
