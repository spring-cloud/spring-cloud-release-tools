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

import java.util.ArrayList;
import java.util.List;

import releaser.internal.Releaser;
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesUpdater;
import releaser.internal.buildsystem.GradleUpdater;
import releaser.internal.buildsystem.ProjectPomUpdater;
import releaser.internal.docs.CustomProjectDocumentationUpdater;
import releaser.internal.docs.DocumentationUpdater;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.github.CustomGithubIssues;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.options.Parser;
import releaser.internal.postrelease.PostReleaseActions;
import releaser.internal.project.ProjectCommandExecutor;
import releaser.internal.sagan.SaganClient;
import releaser.internal.sagan.SaganUpdater;
import releaser.internal.tasks.ReleaserTask;
import releaser.internal.tasks.SingleProjectReleaserTask;
import releaser.internal.template.TemplateGenerator;
import releaser.internal.versions.VersionsFetcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReleaserProperties.class)
class ReleaserConfiguration {

	@Bean
	@ConditionalOnMissingBean
	OptionsAndPropertiesFactory optionsAndPropertiesFactory() {
		return new OptionsAndPropertiesFactory();
	}

	@Bean
	@ConditionalOnMissingBean
	VersionsToBumpFactory versionsToBumpFactory(Releaser releaser,
			ReleaserProperties properties) {
		return new VersionsToBumpFactory(releaser, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectsToRunFactory projectsToRunFactory(VersionsToBumpFactory versionsToBumpFactory,
			Releaser releaser, ReleaserPropertiesUpdater updater) {
		return new ProjectsToRunFactory(versionsToBumpFactory, releaser, updater);
	}

	@Bean
	@ConditionalOnMissingBean
	TasksToRunFactory tasksToRunFactory(ApplicationContext context) {
		return new TasksToRunFactory(context);
	}

	@Bean
	@ConditionalOnMissingBean
	SpringReleaser springReleaser(OptionsAndPropertiesFactory optionsAndPropertiesFactory,
			ProjectsToRunFactory projectsToRunFactory,
			TasksToRunFactory tasksToRunFactory, FlowRunner flowRunner,
			ReleaserProperties properties) {
		return new DefaultSpringReleaser(properties, optionsAndPropertiesFactory,
				projectsToRunFactory, tasksToRunFactory, flowRunner);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectCommandExecutor projectBuilder() {
		return new ProjectCommandExecutor();
	}

	@Bean
	@ConditionalOnMissingBean
	VersionsFetcher versionsFetcher(ProjectPomUpdater updater,
			ReleaserProperties properties) {
		return new VersionsFetcher(properties, updater);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectGitHandler projectGitHandler(ReleaserProperties properties) {
		return new ProjectGitHandler(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	ProjectGitHubHandler projectGitHubHandler(
			@Autowired(required = false) List<CustomGithubIssues> customGithubIssues,
			ReleaserProperties properties) {
		return new ProjectGitHubHandler(properties,
				customGithubIssues != null ? customGithubIssues : new ArrayList<>());
	}

	@Bean
	@ConditionalOnMissingBean
	TemplateGenerator templateGenerator(ProjectGitHubHandler handler,
			ReleaserProperties properties) {
		return new TemplateGenerator(properties, handler);
	}

	@Bean
	@ConditionalOnMissingBean
	GradleUpdater gradleUpdater() {
		return new GradleUpdater();
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
			ReleaserProperties releaserProperties, VersionsFetcher versionsFetcher,
			ReleaserPropertiesUpdater releaserPropertiesUpdater) {
		return new PostReleaseActions(handler, pomUpdater, gradleUpdater,
				projectCommandExecutor, releaserProperties, versionsFetcher,
				releaserPropertiesUpdater);
	}

	@Bean
	@ConditionalOnMissingBean
	DocumentationUpdater documentationUpdater(ProjectGitHandler projectGitHandler,
			ReleaserProperties properties, TemplateGenerator templateGenerator,
			@Autowired(
					required = false) List<CustomProjectDocumentationUpdater> customProjectDocumentationUpdaters) {
		return new DocumentationUpdater(projectGitHandler, properties, templateGenerator,
				customProjectDocumentationUpdaters != null
						? customProjectDocumentationUpdaters : new ArrayList<>());
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
	ReleaserPropertiesUpdater releaserPropertiesUpdater() {
		return new ReleaserPropertiesUpdater();
	}

	@Bean
	@ConditionalOnMissingBean
	Parser optionsParser(List<ReleaserTask> allTasks,
			List<SingleProjectReleaserTask> singleProjectReleaserTasks,
			ConfigurableApplicationContext context) {
		return new OptionsParser(allTasks, singleProjectReleaserTasks, context);
	}

}
