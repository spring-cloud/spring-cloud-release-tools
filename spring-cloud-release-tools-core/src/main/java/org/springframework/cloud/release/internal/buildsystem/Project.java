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

/**
 * Represents a single project.
 *
 * @author Marcin Grzejszczak
 */
public class Project {

	public static Project EMPTY_PROJECT = new Project("", "");

	public final String name;

	public final String version;

	public Project(String name, String version) {
		this.name = name;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Project project = (Project) o;
		if (this.name != null ? !this.name.equals(project.name) : project.name != null) {
			return false;
		}
		return this.version != null ? this.version.equals(project.version)
				: project.version == null;
	}

	@Override
	public int hashCode() {
		int result = this.name != null ? this.name.hashCode() : 0;
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "name=[" + this.name + "], version=[" + this.version + ']';
	}

}
