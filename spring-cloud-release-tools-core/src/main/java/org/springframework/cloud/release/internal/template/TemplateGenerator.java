package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.Projects;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

/**
 * @author Marcin Grzejszczak
 */
public class TemplateGenerator {

	private static final String EMAIL_TEMPLATE = "email";
	private static final String BLOG_TEMPLATE = "blog";
	private final File emailOutput = new File("target/email.txt");
	private final File blogOutput = new File("target/blog.md");
	private final ReleaserProperties props;

	public TemplateGenerator(ReleaserProperties props) {
		this.props = props;
	}

	public File email() {
		File emailOutput = file(this.emailOutput);
		String releaseVersion = parsedVersion();
		Template template = template(EMAIL_TEMPLATE);
		return new EmailTemplateGenerator(template, releaseVersion, emailOutput).email();
	}

	private File file(File file) {
		try {
			if (file.exists()) {
				file.delete();
			}
			if (!file.createNewFile()) {
				throw new IllegalStateException("Couldn't create a file [" + file + "]");
			}
			return file;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public File blog(Projects projects) {
		File blogOutput = file(this.blogOutput);
		String releaseVersion = parsedVersion();
		Template template = template(BLOG_TEMPLATE);
		return new BlogTemplateGenerator(template, releaseVersion, blogOutput, projects).blog();
	}

	private String parsedVersion() {
		String version = this.props.getPom().getBranch();
		if (version.startsWith("v")) {
			return version.substring(1);
		}
		return version;
	}

	private Template template(String template) {
		try {
			Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates"));
			handlebars.registerHelper("replace", StringHelpers.replace);
			handlebars.registerHelper("capitalizeFirst", StringHelpers.capitalizeFirst);
			return handlebars.compile(template);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
