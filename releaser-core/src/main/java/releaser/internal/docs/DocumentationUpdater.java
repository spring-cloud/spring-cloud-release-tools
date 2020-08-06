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

package releaser.internal.docs;

import java.io.File;
import java.util.List;

import releaser.internal.ReleaserProperties;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.Projects;
import releaser.internal.template.TemplateGenerator;

/**
 * @author Marcin Grzejszczak
 */
public class DocumentationUpdater {

	private final ProjectDocumentationUpdater projectDocumentationUpdater;

	private final ReleaseTrainContentsUpdater releaseTrainContentsUpdater;

	public DocumentationUpdater(ProjectGitHandler gitHandler,
			ReleaserProperties properties, TemplateGenerator templateGenerator,
			List<CustomProjectDocumentationUpdater> updaters) {
		this.projectDocumentationUpdater = new ProjectDocumentationUpdater(properties,
				gitHandler, updaters);
		this.releaseTrainContentsUpdater = new ReleaseTrainContentsUpdater(properties,
				gitHandler, templateGenerator);
	}

	DocumentationUpdater(ProjectDocumentationUpdater updater,
			ReleaseTrainContentsUpdater contentsUpdater) {
		this.projectDocumentationUpdater = updater;
		this.releaseTrainContentsUpdater = contentsUpdater;
	}

	/**
	 * Updates the release train wiki page.
	 * @param projects list of projects to update versions for
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used or the switch is turned off
	 */
	public File updateReleaseTrainWiki(Projects projects) {
		return this.releaseTrainContentsUpdater.updateReleaseTrainWiki(projects);
	}

}
