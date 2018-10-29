package org.springframework.cloud.release.internal.template;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class NotesGenerator {

	private final ProjectGitHandler handler;

	NotesGenerator(ProjectGitHandler handler) {
		this.handler = handler;
	}

	Set<Notes> fromProjects(Projects projects) {
		return projects.stream().filter(projectVersion ->
				!projectVersion.projectName.toLowerCase().contains("boot")
		).map(projectVersion -> {
			String name = projectVersion.projectName;
			String version = projectVersion.version;
			String closedMilestoneUrl = this.handler.milestoneUrl(projectVersion);
			String convertedName = Arrays.stream(name.split("-"))
					.map(StringUtils::capitalize).collect(Collectors.joining(" "));
			return new Notes(convertedName, version, closedMilestoneUrl);
		}).collect(Collectors.toSet());
	}
}

class Notes {
	private final String name;
	private final String version;
	private final String closedMilestoneUrl;

	Notes(String name, String version, String closedMilestoneUrl) {
		this.name = name;
		this.version = version;
		this.closedMilestoneUrl = closedMilestoneUrl;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getClosedMilestoneUrl() {
		return closedMilestoneUrl;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Notes notes = (Notes) o;
		if (name != null ? !name.equals(notes.name) : notes.name != null)
			return false;
		return version != null ?
				version.equals(notes.version) :
				notes.version == null;
	}

	@Override public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}
}
