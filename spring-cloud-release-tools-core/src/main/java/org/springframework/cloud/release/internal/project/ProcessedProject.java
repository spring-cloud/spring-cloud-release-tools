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

package org.springframework.cloud.release.internal.project;

import java.io.Serializable;

import org.springframework.cloud.release.internal.ReleaserProperties;

/**
 * Represents a processed project.
 *
 * @author Marcin Grzejszczak
 */
public class ProcessedProject implements Serializable {

	/**
	 * Updated properties for a given project. When doing a meta-release this will
	 * represent a merge of the global properties with the per-application ones. For
	 * non-meta-release these will be the application properties.
	 */
	public final ReleaserProperties propertiesForProject;

	/**
	 * Version to which the project should be updated.
	 */
	public final ProjectVersion newProjectVersion;

	/**
	 * Version before updating.
	 */
	public final ProjectVersion originalProjectVersion;

	public ProcessedProject(ReleaserProperties propertiesForProject,
			ProjectVersion newProjectVersion, ProjectVersion originalProjectVersion) {
		this.propertiesForProject = propertiesForProject;
		this.newProjectVersion = newProjectVersion;
		this.originalProjectVersion = originalProjectVersion;
	}

	@Override
	public String toString() {
		return "ProcessedProject{" + "name=" + this.newProjectVersion.projectName
				+ ",version=" + this.newProjectVersion.version + '}'
				+ ",originalProjectVersion=" + this.originalProjectVersion + '}';
	}

	public String projectName() {
		return this.newProjectVersion.version;
	}

	public String newVersion() {
		return this.newProjectVersion.version;
	}

	public String originalVersion() {
		return this.originalProjectVersion.version;
	}

}
