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

package releaser.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
class ReleaseNotesTemplateGenerator {

	private final Template template;

	private final String releaseVersion;

	private final File blogOutput;

	private final Projects projects;

	private final NotesGenerator notesGenerator;

	ReleaseNotesTemplateGenerator(Template template, String releaseVersion,
			File blogOutput, Projects projects, ProjectGitHubHandler handler) {
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
			throw new IllegalStateException(
					"Exception occurred while trying to generate release notes", e);
		}
	}

}
