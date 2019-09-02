/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.buildsystem;

import java.io.File;

/**
 * Parses the bom and returns all parsed versions.
 */
interface BomParser {

	/**
	 * @param clonedBom - location of the cloned BOM repository
	 * @return {@code true} - when this BOM parser can be applied
	 */
	boolean isApplicable(File clonedBom);

	/**
	 * @param thisProjectRoot - root of the clone project
	 * @return versions from BOM
	 */
	VersionsFromBom versionsFromBom(File thisProjectRoot);

}
