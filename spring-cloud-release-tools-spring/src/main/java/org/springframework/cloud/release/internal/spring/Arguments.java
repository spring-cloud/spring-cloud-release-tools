/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import java.io.File;
import java.util.List;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.project.ProcessedProject;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;

public class Arguments {

	public final File project;

	public final Projects projects;

	public final ProjectVersion originalVersion;

	public final ProjectVersion versionFromBom;

	public final ReleaserProperties properties;

	public final List<ProcessedProject> processedProjects;

	public final Options options;

	public Arguments(File project, Projects projects, ProjectVersion originalVersion, ProjectVersion versionFromBom, ReleaserProperties properties, List<ProcessedProject> processedProjects, Options options) {
		this.project = project;
		this.projects = projects;
		this.originalVersion = originalVersion;
		this.versionFromBom = versionFromBom;
		this.properties = properties;
		this.processedProjects = processedProjects;
		this.options = options;
	}
}
