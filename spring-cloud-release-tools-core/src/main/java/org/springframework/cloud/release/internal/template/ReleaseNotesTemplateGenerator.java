package org.springframework.cloud.release.internal.template;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class ReleaseNotesTemplateGenerator {

	private static final Logger log = LoggerFactory.getLogger(ReleaseNotesTemplateGenerator.class);

	private final Template template;
	private final String releaseVersion;
	private final File blogOutput;
	private final Projects projects;
	private final ProjectGitHandler handler;

	ReleaseNotesTemplateGenerator(Template template, String releaseVersion,
			File blogOutput, Projects projects, ProjectGitHandler handler) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.blogOutput = blogOutput;
		this.projects = projects;
		this.handler = handler;
	}

	File releseNotes() {
		try {
			Map<String, Object> map = ImmutableMap.<String, Object>builder()
					.put("date", LocalDate.now().format(DateTimeFormatter.ISO_DATE))
					.put("releaseVersion", this.releaseVersion)
					.put("projects", fromProjects())
					.build();
			String blog = this.template.apply(map);
			Files.write(this.blogOutput.toPath(), blog.getBytes());
			return this.blogOutput;
		}
		catch (IOException e) {
			log.warn("Exception occurred while trying to generate release notes", e);
			return null;
		}
	}

	private Set<Notes> fromProjects() {
		return this.projects.stream().filter(projectVersion ->
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
