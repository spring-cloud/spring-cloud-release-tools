package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * @author Marcin Grzejszczak
 */
class ReleaseNotesTemplateGenerator {

	private static final Logger log = LoggerFactory.getLogger(ReleaseNotesTemplateGenerator.class);

	private final Template template;
	private final String releaseVersion;
	private final File blogOutput;
	private final Projects projects;
	private final NotesGenerator notesGenerator;

	ReleaseNotesTemplateGenerator(Template template, String releaseVersion,
			File blogOutput, Projects projects, ProjectGitHandler handler) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.blogOutput = blogOutput;
		this.projects = projects;
		this.notesGenerator = new NotesGenerator(handler);
	}

	File releaseNotes() {
		try {
			Map<String, Object> map = ImmutableMap.<String, Object>builder()
					.put("date", LocalDate.now().format(DateTimeFormatter.ISO_DATE))
					.put("releaseVersion", this.releaseVersion)
					.put("projects", this.notesGenerator.fromProjects(this.projects))
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

	private int fileSize(File cached) {
		try {
			int length = Files.readAllBytes(cached.toPath()).length;
			if (length == 0) {
				log.warn("Cached file has no contents!");
			}
			return length;
		}
		catch (IOException e) {
			log.warn("Exception [" + e + "] occurred while trying to retrieve file length - will assume it's empty");
			return 0;
		}
	}
}
