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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.buildsystem.GradleUpdater;
import org.springframework.cloud.release.internal.buildsystem.ProjectPomUpdater;
import org.springframework.cloud.release.internal.docs.CustomProjectDocumentationUpdater;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.github.CustomGithubIssues;
import org.springframework.cloud.release.internal.github.ProjectGitHubHandler;
import org.springframework.cloud.release.internal.options.Parser;
import org.springframework.cloud.release.internal.postrelease.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectCommandExecutor;
import org.springframework.cloud.release.internal.sagan.SaganClient;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.cloud.release.internal.tasks.SingleProjectReleaserTask;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.cloud.release.internal.versions.VersionsFetcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReleaserProperties.class)
class ReleaserConfiguration {

	@Autowired
	ReleaserProperties properties;

	@Bean
	@ConditionalOnMissingBean
	OptionsAndPropertiesFactory optionsAndPropertiesFactory() {
		return new OptionsAndPropertiesFactory();
	}

	@Bean
	@ConditionalOnMissingBean
	VersionsToBumpFactory versionsToBumpFactory(Releaser releaser) {
		return new VersionsToBumpFactory(releaser, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectsToRunFactory projectsToRunFactory(VersionsToBumpFactory versionsToBumpFactory, Releaser releaser, ReleaserPropertiesUpdater updater) {
		return new ProjectsToRunFactory(versionsToBumpFactory, releaser, updater);
	}

	@Bean
	@ConditionalOnMissingBean
	TasksToRunFactory tasksToRunFactory(ApplicationContext context) {
		return new TasksToRunFactory(context);
	}

	@Bean
	@ConditionalOnMissingBean
	FlowRunner flowRunner(StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher) {
		return new SpringBatchFlowRunner(stepBuilderFactory, jobBuilderFactory, projectsToRunFactory, jobLauncher);
	}

	@Bean
	@ConditionalOnMissingBean
	SpringReleaser springReleaser(OptionsAndPropertiesFactory optionsAndPropertiesFactory, ProjectsToRunFactory projectsToRunFactory, TasksToRunFactory tasksToRunFactory, FlowRunner flowRunner) {
		return new DefaultSpringReleaser(properties, optionsAndPropertiesFactory, projectsToRunFactory, tasksToRunFactory, flowRunner);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectCommandExecutor projectBuilder() {
		return new ProjectCommandExecutor(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	VersionsFetcher versionsFetcher(ProjectPomUpdater updater) {
		return new VersionsFetcher(this.properties, updater);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectGitHandler projectGitHandler() {
		return new ProjectGitHandler(this.properties);
	}
	@Bean
	@ConditionalOnMissingBean
	ProjectGitHubHandler projectGitHubHandler(@Autowired(required = false)
			List<CustomGithubIssues> customGithubIssues) {
		return new ProjectGitHubHandler(this.properties, customGithubIssues != null ? customGithubIssues : new ArrayList<>());
	}

	@Bean
	@ConditionalOnMissingBean
	TemplateGenerator templateGenerator(ProjectGitHubHandler handler) {
		return new TemplateGenerator(this.properties, handler);
	}

	@Bean
	@ConditionalOnMissingBean
	GradleUpdater gradleUpdater() {
		return new GradleUpdater(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	SaganUpdater saganUpdater(SaganClient saganClient,
			ReleaserProperties releaserProperties) {
		return new SaganUpdater(saganClient, releaserProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	PostReleaseActions postReleaseActions(ProjectGitHandler handler,
			ProjectPomUpdater pomUpdater, GradleUpdater gradleUpdater,
			ProjectCommandExecutor projectCommandExecutor,
			ReleaserProperties releaserProperties, VersionsFetcher versionsFetcher) {
		return new PostReleaseActions(handler, pomUpdater, gradleUpdater,
				projectCommandExecutor, releaserProperties, versionsFetcher);
	}
	@Bean
	@ConditionalOnMissingBean
	DocumentationUpdater documentationUpdater(ProjectGitHandler projectGitHandler,
			ReleaserProperties properties, TemplateGenerator templateGenerator, @Autowired(required = false)
			List<CustomProjectDocumentationUpdater> customProjectDocumentationUpdaters) {
		return new DocumentationUpdater(projectGitHandler, properties, templateGenerator,
				customProjectDocumentationUpdaters != null ? customProjectDocumentationUpdaters : new ArrayList<>());
	}

	@Bean
	@ConditionalOnMissingBean
	Releaser releaser(ProjectPomUpdater projectPomUpdater,
			ProjectCommandExecutor projectCommandExecutor,
			ProjectGitHandler projectGitHandler,
			ProjectGitHubHandler projectGitHubHandler,
			TemplateGenerator templateGenerator, GradleUpdater gradleUpdater,
			SaganUpdater saganUpdater, DocumentationUpdater documentationUpdater,
			PostReleaseActions postReleaseActions,
			ReleaserProperties releaserProperties) {
		return new Releaser(releaserProperties, projectPomUpdater, projectCommandExecutor,
				projectGitHandler, projectGitHubHandler, templateGenerator, gradleUpdater,
				saganUpdater, documentationUpdater, postReleaseActions);
	}

	@Bean
	@ConditionalOnMissingBean
	ReleaserPropertiesUpdater releaserPropertiesUpdater(ApplicationContext context) {
		return new ReleaserPropertiesUpdater(context);
	}

	@Bean
	@ConditionalOnMissingBean
	Parser optionsParser(List<ReleaserTask> allTasks,
			List<SingleProjectReleaserTask> singleProjectReleaserTasks) {
		return new OptionsParser(allTasks, singleProjectReleaserTasks);
	}

}
