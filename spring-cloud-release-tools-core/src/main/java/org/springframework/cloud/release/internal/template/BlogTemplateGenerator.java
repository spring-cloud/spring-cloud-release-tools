package org.springframework.cloud.release.internal.template;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class BlogTemplateGenerator {

	private static final Pattern RC_PATTERN = Pattern.compile("(.*)(RC)([0-9]+)");
	private static final Pattern MILESTONE_PATTERN = Pattern.compile("(.*)(M)([0-9]+)");
	private static final Pattern SR_PATTERN = Pattern.compile("(.*)(SR)([0-9]+)");

	private final Template template;
	private final String releaseVersion;
	private final File blogOutput;
	private final Projects projects;

	BlogTemplateGenerator(Template template, String releaseVersion, File blogOutput,
			Projects projects) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.blogOutput = blogOutput;
		this.projects = projects;
	}

	File blog() {
		try {
			// availability - General Availability (RELEASE) / Service Release 1 (SR1) / Milestone 1 (M1)
			// releaseName - Dalston
			// releaseLink
			// - [Maven Central](http://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dependencies/Dalston.RELEASE/)
			// - [Spring Milestone](https://repo.spring.io/milestone/) repository
			// releaseVersion '- Dalston.RELEASE
			boolean release = this.releaseVersion.contains("RELEASE");
			boolean nonRelease = !(release || SR_PATTERN.matcher(this.releaseVersion).matches());
			String availability = availability(release);
			String releaseName = parsedReleaseName(this.releaseVersion);
			String releaseLink = link(nonRelease);
			Map<String, Object> map = ImmutableMap.<String, Object>builder()
					.put("availability", availability)
					.put("releaseName", releaseName)
					.put("releaseLink", releaseLink)
					.put("releaseVersion", this.releaseVersion)
					.put("projects", fromProjects())
					.put("nonRelease", nonRelease)
					.build();
			String blog = this.template.apply(map);
			Files.write(this.blogOutput.toPath(), blog.getBytes());
			return this.blogOutput;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private Set<BlogTuple> fromProjects() {
		return this.projects.stream().map(projectVersion -> {
			String name = projectVersion.projectName;
			String version = projectVersion.version;
			String convertedName = Arrays.stream(name.split("-")).map(
					StringUtils::capitalize).collect(Collectors.joining(" "));
			return new BlogTuple(convertedName, version);
		}).collect(Collectors.toSet());
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
		} else if (sr.matches()) {
			return availabilityText(sr, "Service Release", "SR");
		} else if (rc.matches()) {
			return availabilityText(rc, "Release Candidate", "RC");
		} else if (milestone.matches()) {
			return availabilityText(milestone, "Milestone", "M");
		}
		throw new IllegalStateException("Wrong version [" + this.releaseVersion + "] for a blog post");
	}

	private String availabilityText(Matcher matcher, String text, String shortText) {
		String number = matcher.group(3);
		return text + " " + number + " (" + shortText + number + ")";
	}

	private String link(boolean nonRelease) {
		if (nonRelease) {
			return "[Spring Milestone](https://repo.spring.io/milestone/) repository";
		}
		return "[Maven Central](http://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dependencies/" + this.releaseVersion + "/)";
	}
}

class BlogTuple {
	private final String name;
	private final String version;

	BlogTuple(String name, String version) {
		this.name = name;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BlogTuple blogTuple = (BlogTuple) o;
		if (name != null ? !name.equals(blogTuple.name) : blogTuple.name != null)
			return false;
		return version != null ? version.equals(blogTuple.version) : blogTuple.version == null;
	}

	@Override public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}
}
