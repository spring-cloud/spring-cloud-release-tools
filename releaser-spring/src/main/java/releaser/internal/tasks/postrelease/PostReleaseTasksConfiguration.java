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

package releaser.internal.tasks.postrelease;

import releaser.internal.Releaser;
import releaser.internal.tasks.ConditionalOnDefaultFlowEnabled;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnDefaultFlowEnabled
@ConditionalOnProperty(value = "releaser.skip-post-release-tasks", havingValue = "false", matchIfMissing = true)
class PostReleaseTasksConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-github-milestones")
	CloseMilestonesProjectPostReleaseTask closeMilestonesReleaseTask(Releaser releaser) {
		return new CloseMilestonesProjectPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.template.enabled")
	CreateTemplatesTrainPostReleaseTask createTemplatesTrainPostReleaseTask(Releaser releaser) {
		return new CreateTemplatesTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.run-updated-samples")
	RunUpdatedSamplesTrainPostReleaseTask runUpdatedSamplesTrainPostReleaseTask(Releaser releaser) {
		return new RunUpdatedSamplesTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-all-test-samples")
	UpdateAllTestSamplesTrainPostReleaseTask updateAllTestSamplesTrainPostReleaseTask(Releaser releaser) {
		return new UpdateAllTestSamplesTrainPostReleaseTask(releaser);
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
	UpdateReleaseTrainDocsTrainPostReleaseTask updateReleaseTrainDocsTrainPostReleaseTask(Releaser releaser) {
		return new UpdateReleaseTrainDocsTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.git.update-release-train-wiki")
	UpdateReleaseTrainWikiTrainPostReleaseTask updateReleaseTrainWikiTrainPostReleaseTask(Releaser releaser) {
		return new UpdateReleaseTrainWikiTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.sagan.update-sagan")
	UpdateSaganProjectPostReleaseTask updateSaganProjectPostReleaseTask(Releaser releaser) {
		return new UpdateSaganProjectPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.sagan.update-start-spring-io")
	UpdateStartSpringIoTrainPostReleaseTask updateStartSpringIoTrainPostReleaseTask(Releaser releaser) {
		return new UpdateStartSpringIoTrainPostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.bundles.create-release-train-release-bundle")
	CreateReleaseTrainReleaseBundlePostReleaseTask createReleaseTrainReleaseBundlePostReleaseTask(Releaser releaser) {
		return new CreateReleaseTrainReleaseBundlePostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.bundles.create-project-release-bundle")
	CreateProjectReleaseBundlePostReleaseTask createProjectReleaseBundlePostReleaseTask(Releaser releaser) {
		return new CreateProjectReleaseBundlePostReleaseTask(releaser);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("releaser.bundles.distribute-release-bundle")
	DistributeReleaseBundleTask distributeReleaseBundleTask(Releaser releaser) {
		return new DistributeReleaseBundleTask(releaser);
	}

}
