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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class Row {

	final String componentName;

	final String lastGaVersion;

	final String currentGaVersion;

	final String currentSnapshotVersion;

	Row(String[] row) {
		int initialIndex = row.length == 4 ? 0 : 1;
		this.componentName = row[initialIndex].trim();
		this.lastGaVersion = row[initialIndex + 1].trim();
		this.currentGaVersion = row[initialIndex + 2].trim();
		this.currentSnapshotVersion = row[initialIndex + 3].trim();
	}

	Row(String componentName, String lastGaVersion, String currentGaVersion,
			String currentSnapshotVersion) {
		this.componentName = componentName.trim();
		this.lastGaVersion = lastGaVersion.trim();
		this.currentGaVersion = currentGaVersion.trim();
		this.currentSnapshotVersion = currentSnapshotVersion.trim();
	}

	static List<Row> fromProjects(Projects projects, boolean lastGa) {
		return projects.stream()
				.map(v -> new Row(v.projectName, lastGa ? versionOrEmptyForGa(v) : "",
						!lastGa ? versionOrEmptyForGa(v) : "",
						v.isSnapshot() ? v.version : ""))
				.collect(Collectors.toCollection(LinkedList::new));
	}

	private static String versionOrEmptyForGa(ProjectVersion v) {
		return v.isReleaseOrServiceRelease() ? v.version : "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Row row = (Row) o;
		return Objects.equals(this.componentName, row.componentName)
				&& Objects.equals(this.lastGaVersion, row.lastGaVersion)
				&& Objects.equals(this.currentGaVersion, row.currentGaVersion) && Objects
						.equals(this.currentSnapshotVersion, row.currentSnapshotVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.componentName, this.lastGaVersion, this.currentGaVersion,
				this.currentSnapshotVersion);
	}

	public String getComponentName() {
		return this.componentName;
	}

	public String getLastGaVersion() {
		return this.lastGaVersion;
	}

	public String getCurrentGaVersion() {
		return this.currentGaVersion;
	}

	public String getCurrentSnapshotVersion() {
		return this.currentSnapshotVersion;
	}

}
