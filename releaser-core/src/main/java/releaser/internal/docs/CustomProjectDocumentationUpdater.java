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

import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public interface CustomProjectDocumentationUpdater {

	/**
	 * NO OP implementation of the updater.
	 */
	CustomProjectDocumentationUpdater NO_OP = new CustomProjectDocumentationUpdater() {

		@Override
		public File updateDocsRepoForReleaseTrain(File clonedDocumentationProject, ProjectVersion currentProject,
				Projects projects, String bomBranch) {
			return clonedDocumentationProject;
		}

		@Override
		public File updateDocsRepoForSingleProject(File clonedDocumentationProject, ProjectVersion currentProject,
				Projects projects) {
			return clonedDocumentationProject;
		}
	};

	/**
	 * Updates the documentation repository for a release train.
	 * @param clonedDocumentationProject path to the cloned documentation project
	 * @param currentProject project to update the docs repo for
	 * @param projects list of projects to update versions for
	 * @param bomBranch the bom project branch
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used
	 */
	File updateDocsRepoForReleaseTrain(File clonedDocumentationProject, ProjectVersion currentProject,
			Projects projects, String bomBranch);

	/**
	 * Updates the documentation repository for a single project.
	 * @param clonedDocumentationProject path to the cloned documentation project
	 * @param currentProject project to update the docs repo for
	 * @param projects list of projects to update versions for
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used
	 */
	File updateDocsRepoForSingleProject(File clonedDocumentationProject, ProjectVersion currentProject,
			Projects projects);

}
