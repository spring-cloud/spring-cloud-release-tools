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

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.project.ProjectVersion;

/**
 * A single project to be released. Contains all the information necessary to release a
 * project.
 */
public class ProjectToRun implements Serializable {

	/**
	 * Cloned location of this project.
	 */
	public final File thisProjectFolder;

	/**
	 * All projects taken from the BOM.
	 */
	public final ProjectsFromBom allProjectsFromBom;

	/**
	 * The original version of the project.
	 */
	public final ProjectVersion originalVersion;

	/**
	 * The project version of this project taken from the BOM.
	 */
	public final ProjectVersion thisProjectVersionFromBom;

	/**
	 * {@link ReleaserProperties} updated for this project.
	 */
	public final ReleaserProperties thisProjectReleaserProperties;

	/**
	 * {@link Options} updated for this project.
	 */
	public final Options options;

	public ProjectToRun(File thisProjectFolder, ProjectsFromBom allProjectsFromBom,
			ProjectVersion originalVersion,
			ReleaserProperties thisProjectReleaserProperties, Options options) {
		this.thisProjectFolder = thisProjectFolder;
		this.allProjectsFromBom = allProjectsFromBom;
		this.originalVersion = originalVersion;
		this.thisProjectReleaserProperties = thisProjectReleaserProperties;
		this.options = options;
		this.thisProjectVersionFromBom = allProjectsFromBom.currentProjectFromBom;
	}

	public String name() {
		return this.thisProjectFolder.getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProjectToRun that = (ProjectToRun) o;
		return Objects.equals(originalVersion, that.originalVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(originalVersion);
	}

	/**
	 * Supplier of a project to run. Since retrieval of a project may require cloning it,
	 * we will cache the cloned location instead of cloning it each time.
	 */
	public static class ProjectToRunSupplier
			implements Supplier<ProjectToRun>, Closeable {

		private static final Map<String, ProjectToRun> CACHE = new ConcurrentHashMap<>();

		private final String projectName;

		private final Supplier<ProjectToRun> project;

		public ProjectToRunSupplier(String projectName, Supplier<ProjectToRun> project) {
			this.projectName = projectName;
			this.project = project;
		}

		@Override
		public ProjectToRun get() {
			return CACHE.computeIfAbsent(this.projectName, s -> this.project.get());
		}

		public String projectName() {
			return this.projectName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ProjectToRunSupplier that = (ProjectToRunSupplier) o;
			return Objects.equals(this.projectName, that.projectName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.projectName);
		}

		@Override
		public void close() {
			CACHE.clear();
		}

	}

}
