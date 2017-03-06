package org.springframework.cloud.release;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents versions taken out from Spring Cloud Release pom
 *
 * @author Marcin Grzejszczak
 */
class Versions {

	String boot;
	String build;
	Set<Project> projects = new HashSet<>();

	Versions(String boot) {
		this.boot = boot;
	}

	Versions(String build, Set<Project> projects) {
		this.build = build;
		this.projects = projects;
	}

	Versions(String boot, String build, Set<Project> projects) {
		this.boot = boot;
		this.build = build;
		this.projects = projects;
	}
}

class Project {
	final String name;
	final String version;

	Project(String name, String version) {
		this.name = name;
		this.version = version;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Project project = (Project) o;
		if (this.name != null ? !this.name.equals(project.name) : project.name != null)
			return false;
		return this.version != null ?
				this.version.equals(project.version) :
				project.version == null;
	}

	@Override public int hashCode() {
		int result = this.name != null ? this.name.hashCode() : 0;
		result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
		return result;
	}

	@Override public String toString() {
		return "Project{" + "name='" + this.name + '\'' + ", version='" + this.version + '\'' + '}';
	}
}