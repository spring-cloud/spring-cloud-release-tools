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

package releaser.internal.buildsystem;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import releaser.internal.ReleaserProperties;
import releaser.internal.project.Project;

import org.springframework.lang.Nullable;

/**
 * Allows to pass in some additional gradle files parser.
 */
public interface CustomBomParser {

	CustomBomParser NO_OP = new CustomBomParser() {
		@Override
		public boolean isApplicable(File thisProjectRoot, ReleaserProperties properties,
				Set<Project> projects) {
			return true;
		}

		@Override
		public VersionsFromBom parseBom(File thisProjectRoot,
				ReleaserProperties properties) {
			return VersionsFromBom.EMPTY_VERSION;
		}

	};

	/**
	 * Different projects can have different parsers. This method will tell whether the
	 * current parser should be applied or not.
	 * @param thisProjectRoot - location of the cloned project
	 * @param properties - releaser properties
	 * @param projects - parsed projects from the BOM
	 * @return {@code true} if the parser should be applied.
	 */
	boolean isApplicable(File thisProjectRoot, ReleaserProperties properties,
			@Nullable Set<Project> projects);

	/**
	 * When parsing a part of the BOM pom, one can add custom logic to perform project
	 * specific parsing.
	 * @param thisProjectRoot - location of the cloned project
	 * @param properties - releaser properties
	 * @return - versions retrieved from the BOM. Can be
	 * {@link VersionsFromBom#EMPTY_VERSION} if nothing was found.
	 */
	VersionsFromBom parseBom(File thisProjectRoot, ReleaserProperties properties);

	/**
	 * Allows to hook in custom logic for versions setting.
	 * @param projects - set of projects
	 * @param projectName - name of the project
	 * @param version - version of the project
	 * @return - a new collection with the modified versions from bom
	 */
	default Set<Project> setVersion(Set<Project> projects, String projectName,
			String version) {
		return new LinkedHashSet<>(projects);
	}

	default boolean isMaven(File thisProjectRoot) {
		return new File(thisProjectRoot, "pom.xml").exists();
	}

	default boolean isGradle(File thisProjectRoot) {
		return new File(thisProjectRoot, "build.gradle").exists();
	}

}
