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

package org.springframework.cloud.release.internal.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.gradle.GradleUpdater;
import org.springframework.cloud.release.internal.options.Parser;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.post.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.cloud.release.internal.versions.VersionsFetcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReleaserProperties.class)
class ReleaserConfiguration {

	@Autowired
	ReleaserProperties properties;

	@Bean
	TaskCollector taskCollector() {
		return new TaskCollector();
	}

	@Bean
	SpringReleaser springReleaser(Releaser releaser, ReleaserPropertiesUpdater updater,
			ApplicationEventPublisher applicationEventPublisher) {
		return new SpringReleaser(releaser, this.properties, updater,
				applicationEventPublisher);
	}

	@Bean
	ProjectBuilder projectBuilder() {
		return new ProjectBuilder(this.properties);
	}

	@Bean
	ProjectPomUpdater pomUpdater() {
		return new ProjectPomUpdater(this.properties);
	}

	@Bean
	VersionsFetcher versionsFetcher(ProjectPomUpdater updater) {
		return new VersionsFetcher(this.properties, updater);
	}

	@Bean
	ProjectGitHandler projectGitHandler() {
		return new ProjectGitHandler(this.properties);
	}

	@Bean
	TemplateGenerator templateGenerator(ProjectGitHandler handler) {
		return new TemplateGenerator(this.properties, handler);
	}

	@Bean
	GradleUpdater gradleUpdater() {
		return new GradleUpdater(this.properties);
	}

	@Bean
	SaganUpdater saganUpdater(SaganClient saganClient,
			ReleaserProperties releaserProperties) {
		return new SaganUpdater(saganClient, releaserProperties);
	}

	@Bean
	PostReleaseActions postReleaseActions(ProjectGitHandler handler,
			ProjectPomUpdater pomUpdater, GradleUpdater gradleUpdater,
			ProjectBuilder projectBuilder, ReleaserProperties releaserProperties) {
		return new PostReleaseActions(handler, pomUpdater, gradleUpdater, projectBuilder,
				releaserProperties, versionsFetcher);
	}

	@Bean
	DocumentationUpdater documentationUpdater(ProjectGitHandler projectGitHandler,
			ReleaserProperties properties, TemplateGenerator templateGenerator) {
		return new DocumentationUpdater(projectGitHandler, properties, templateGenerator);
	}

	@Bean
	Releaser releaser(ProjectPomUpdater projectPomUpdater, ProjectBuilder projectBuilder,
			ProjectGitHandler projectGitHandler, TemplateGenerator templateGenerator,
			GradleUpdater gradleUpdater, SaganUpdater saganUpdater,
			DocumentationUpdater documentationUpdater,
			PostReleaseActions postReleaseActions) {
		return new Releaser(projectPomUpdater, projectBuilder, projectGitHandler,
				templateGenerator, gradleUpdater, saganUpdater, documentationUpdater,
				postReleaseActions);
	}

	@Bean
	ReleaserPropertiesUpdater releaserPropertiesUpdater(ApplicationContext context) {
		return new ReleaserPropertiesUpdater(context);
	}

	@Bean
	Parser optionsParser() {
		return new OptionsParser();
	}

}
