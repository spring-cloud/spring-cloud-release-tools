package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

	static final Map<String, File> CACHE = new ConcurrentHashMap<>();

	ReleaseNotesTemplateGenerator(Template template, String releaseVersion,
			File blogOutput, Projects projects, ProjectGitHandler handler) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.blogOutput = blogOutput;
		this.projects = projects;
		this.notesGenerator = new NotesGenerator(handler);
	}

	File releaseNotes() {
		File cached = CACHE.get(this.releaseVersion);
		if (cached != null && fileSize(cached) > 0) {
			log.info("Found an existing entry [{}] in the cache "
					+ "for version [{}] with size [{}]", cached, this.releaseVersion, fileSize(cached));
			return cached;
		}
		try {
			Map<String, Object> map = ImmutableMap.<String, Object>builder()
					.put("date", LocalDate.now().format(DateTimeFormatter.ISO_DATE))
					.put("releaseVersion", this.releaseVersion)
					.put("projects", this.notesGenerator.fromProjects(this.projects))
					.build();
			String blog = this.template.apply(map);
			Files.write(this.blogOutput.toPath(), blog.getBytes());
			File output = this.blogOutput;
			CACHE.put(this.releaseVersion, output);
			return output;
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
