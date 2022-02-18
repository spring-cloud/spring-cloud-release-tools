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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import releaser.internal.ReleaserProperties;
import releaser.internal.project.Project;

public class VersionsFromBomBuilder {

	private ReleaserProperties releaserProperties;

	private Set<Project> projects = new HashSet<>();

	private VersionsFromBom[] versionsFromBom = new VersionsFromBom[0];

	private List<CustomBomParser> parsers = new ArrayList<>();

	private File thisProjectRoot;

	public VersionsFromBomBuilder thisProjectRoot(File thisProjectRoot) {
		this.thisProjectRoot = thisProjectRoot;
		return this;
	}

	public VersionsFromBomBuilder releaserProperties(ReleaserProperties releaserProperties) {
		this.releaserProperties = releaserProperties;
		return this;
	}

	public VersionsFromBomBuilder projects(Set<Project> projects) {
		this.projects = projects;
		return this;
	}

	public VersionsFromBomBuilder projects(VersionsFromBom... versionsFromBom) {
		this.versionsFromBom = versionsFromBom;
		return this;
	}

	public VersionsFromBomBuilder parsers(List<CustomBomParser> parsers) {
		this.parsers = parsers;
		return this;
	}

	public VersionsFromBom merged() {
		CustomBomParser bomParser = parser();
		if (!this.projects.isEmpty()) {
			return new VersionsFromBom(this.releaserProperties, bomParser, this.projects);
		}
		return new VersionsFromBom(this.releaserProperties, bomParser, this.versionsFromBom);
	}

	public VersionsFromBom retrieveFromBom() {
		File thisProjectRoot = thisProjectRoot();
		CustomBomParser bomParser = parser();
		VersionsFromBom versionsFromBom = versionsFromBom(bomParser);
		VersionsFromBom customParsing = customParsing(thisProjectRoot);
		return new VersionsFromBom(this.releaserProperties, bomParser, versionsFromBom, customParsing);
	}

	private File thisProjectRoot() {
		return this.thisProjectRoot != null ? this.thisProjectRoot : new File(this.releaserProperties.getWorkingDir());
	}

	private CustomBomParser parser() {
		return this.parsers.isEmpty() ? CustomBomParser.NO_OP : this.parsers.get(0);
	}

	private VersionsFromBom versionsFromBom(CustomBomParser bomParser) {
		if (!this.projects.isEmpty()) {
			return new VersionsFromBom(this.releaserProperties, bomParser, this.projects);
		}
		else if (this.versionsFromBom.length != 0) {
			return new VersionsFromBom(this.releaserProperties, bomParser, this.versionsFromBom);
		}
		return new VersionsFromBom(this.releaserProperties, bomParser);
	}

	private VersionsFromBom customParsing(File thisProjectRoot) {
		return this.parsers.stream().map(p -> p.parseBom(thisProjectRoot, this.releaserProperties))
				.reduce((versionsFromBom, versionsFromBom2) -> new VersionsFromBomBuilder().parsers(this.parsers)
						.thisProjectRoot(thisProjectRoot).releaserProperties(this.releaserProperties)
						.projects(versionsFromBom, versionsFromBom2).merged())
				.orElse(VersionsFromBom.EMPTY_VERSION);
	}

}