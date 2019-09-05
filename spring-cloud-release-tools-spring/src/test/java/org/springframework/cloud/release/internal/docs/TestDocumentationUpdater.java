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

import edu.emory.mathcs.backport.java.util.Collections;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.template.TemplateGenerator;

/**
 * @author Marcin Grzejszczak
 */
public class TestDocumentationUpdater extends DocumentationUpdater {

	public TestDocumentationUpdater(ReleaserProperties properties,
			CustomProjectDocumentationUpdater updater, ProjectGitHandler handler,
			TestReleaseContentsUpdater testRelease) {
		super(properties, new ProjectDocumentationUpdater(properties, handler,
				Collections.singletonList(updater)), testRelease);
	}

	public static class TestReleaseContentsUpdater extends ReleaseTrainContentsUpdater {

		public TestReleaseContentsUpdater(ReleaserProperties properties,
				ProjectGitHandler handler, TemplateGenerator templateGenerator) {
			super(properties, handler, templateGenerator);
		}

		@Override
		public File updateProjectRepo(Projects projects) {
			return super.updateProjectRepo(projects);
		}

	}

}
