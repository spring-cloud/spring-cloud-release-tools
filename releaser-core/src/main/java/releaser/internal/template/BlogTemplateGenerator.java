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
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
class BlogTemplateGenerator {

	private static final Logger log = LoggerFactory
			.getLogger(BlogTemplateGenerator.class);

	private static final Pattern RC_PATTERN = Pattern.compile("(.*)(RC)([0-9]+)");

	private static final Pattern MILESTONE_PATTERN = Pattern.compile("(.*)(M)([0-9]+)");

	private static final Pattern SR_PATTERN = Pattern.compile("(.*)(SR)([0-9]+)");

	private final Template template;

	private final String releaseVersion;

	private final File blogOutput;

	private final Projects projects;

	private final NotesGenerator notesGenerator;

	BlogTemplateGenerator(Template template, String releaseVersion, File blogOutput,
			Projects projects, ProjectGitHubHandler handler) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.blogOutput = blogOutput;
		this.projects = projects;
		this.notesGenerator = new NotesGenerator(handler);
	}

	File blog() {
		try {
			// availability - General Availability (RELEASE) / Service Release 1 (SR1) /
			// Milestone 1 (M1)
			// releaseName - Dalston
			// releaseLink
			// - [Maven Central](https://repo1.maven.org/maven2/org/
			// springframework/cloud/spring-cloud-dependencies/Dalston.RELEASE/)
			// - [Spring Milestone](https://repo.spring.io/milestone/) repository
			// releaseVersion '- Dalston.RELEASE
			boolean release = this.releaseVersion.contains("RELEASE");
			boolean nonRelease = !(release
					|| SR_PATTERN.matcher(this.releaseVersion).matches());
			String availability = availability(release);
			String releaseName = parsedReleaseName(this.releaseVersion);
			String releaseLink = link(nonRelease);
			Map<String, Object> map = ImmutableMap.<String, Object>builder()
					.put("availability", availability).put("releaseName", releaseName)
					.put("releaseLink", releaseLink)
					.put("releaseVersion", this.releaseVersion)
					.put("projects", this.notesGenerator.fromProjects(this.projects))
					.put("nonRelease", nonRelease).build();
			String blog = this.template.apply(map);
			Files.write(this.blogOutput.toPath(), blog.getBytes());
			return this.blogOutput;
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Exception occurred while trying to create a blog entry", e);
		}
	}

	private String parsedReleaseName(String version) {
		return version.substring(0, version.indexOf("."));
	}

	private String availability(boolean release) {
		Matcher sr = SR_PATTERN.matcher(this.releaseVersion);
		Matcher rc = RC_PATTERN.matcher(this.releaseVersion);
		Matcher milestone = MILESTONE_PATTERN.matcher(this.releaseVersion);
		if (release) {
			return "General Availability (RELEASE)";
		}
		else if (sr.matches()) {
			return availabilityText(sr, "Service Release", "SR");
		}
		else if (rc.matches()) {
			return availabilityText(rc, "Release Candidate", "RC");
		}
		else if (milestone.matches()) {
			return milestone(milestone);
		}
		if (log.isWarnEnabled()) {
			log.warn("Unrecognized release [{}] . "
					+ "Hopefully, you know what you're doing. Will treat it as milestone",
					this.releaseVersion);
		}
		return milestone(milestone);
	}

	private String milestone(Matcher milestone) {
		return availabilityText(milestone, "Milestone", "M");
	}

	private String availabilityText(Matcher matcher, String text, String shortText) {
		String number = matcher.group(3);
		return text + " " + number + " (" + shortText + number + ")";
	}

	private String link(boolean nonRelease) {
		if (nonRelease) {
			return "[Spring Milestone](https://repo.spring.io/milestone/) repository";
		}
		return "[Maven Central](https://repo1.maven.org/maven2/"
				+ "org/springframework/cloud/spring-cloud-dependencies/"
				+ this.releaseVersion + "/)";
	}

}
