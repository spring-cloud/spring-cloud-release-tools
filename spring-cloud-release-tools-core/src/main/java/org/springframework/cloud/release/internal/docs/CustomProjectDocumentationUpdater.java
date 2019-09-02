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

package org.springframework.cloud.release.internal.docs;

import java.io.File;

import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;

/**
 * @author Marcin Grzejszczak
 */
public interface CustomProjectDocumentationUpdater {

	/**
	 * Different projects can have different documentation updaters. This method will tell
	 * whether the current updater should be applied or not. Updates the documentation
	 * repository.
	 * @param clonedDocumentationProject path to the cloned documentation project
	 * @param currentProject project to update the docs repo for
	 * @param bomBranch the bom project branch
	 * @return {@code true} if the parser should be applied.
	 */
	boolean isApplicable(File clonedDocumentationProject, ProjectVersion currentProject,
			String bomBranch);

	/**
	 * Updates the documentation repository.
	 * @param clonedDocumentationProject path to the cloned documentation project
	 * @param currentProject project to update the docs repo for
	 * @param bomBranch the bom project branch
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used
	 */
	File updateDocsRepo(File clonedDocumentationProject, ProjectVersion currentProject,
			String bomBranch);

	/**
	 * NO OP implementation of the updater.
	 */
	CustomProjectDocumentationUpdater NO_OP = new CustomProjectDocumentationUpdater() {
		@Override
		public boolean isApplicable(File clonedDocumentationProject,
				ProjectVersion currentProject, String bomBranch) {
			return true;
		}

		@Override
		public File updateDocsRepo(File clonedDocumentationProject,
				ProjectVersion currentProject, String bomBranch) {
			return clonedDocumentationProject;
		}
	};

}
