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

package releaser.internal.tasks.composite;

import releaser.internal.tasks.ConditionalOnDefaultFlowEnabled;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnDefaultFlowEnabled
class CompositeTasksConfiguration {

	@Bean
	@ConditionalOnMissingBean
	DryRunCompositeTask dryRunCompositeTask(ApplicationContext context) {
		return new DryRunCompositeTask(context);
	}

	@Bean
	@ConditionalOnMissingBean
	MetaReleaseCompositeTask metaReleaseCompositeTask(ApplicationContext context) {
		return new MetaReleaseCompositeTask(context);
	}

	@Bean
	@ConditionalOnMissingBean
	MetaReleaseDryRunCompositeTask metaReleaseDryRunCompositeTask(
			ApplicationContext context) {
		return new MetaReleaseDryRunCompositeTask(context);
	}

	@Bean
	@ConditionalOnMissingBean
	ReleaseCompositeTask releaseCompositeTask(ApplicationContext context) {
		return new ReleaseCompositeTask(context);
	}

	@Bean
	@ConditionalOnMissingBean
	ReleaseVerboseCompositeTask releaseVerboseCompositeTask(ApplicationContext context) {
		return new ReleaseVerboseCompositeTask(context);
	}

	@Bean
	@ConditionalOnMissingBean
	TrainPostReleaseCompositeTask trainPostReleaseCompositeTask(
			ApplicationContext context) {
		return new TrainPostReleaseCompositeTask(context);
	}

}
