package org.springframework.cloud.release.internal.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import java.io.File;
import java.io.IOException;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class TemplateGenerator {

	private static final String EMAIL_TEMPLATE = "email";
	private static final String BLOG_TEMPLATE = "blog";
	private static final String TWITTER_TEMPLATE = "tweet";
	private final File emailOutput;
	private final File blogOutput;
	private final File tweetOutput;
	private final ReleaserProperties props;

	public TemplateGenerator(ReleaserProperties props) {
		this.props = props;
		this.emailOutput = new File("target/email.txt");
		this.blogOutput = new File("target/blog.md");
		this.tweetOutput = new File("target/tweet.txt");
	}

	TemplateGenerator(ReleaserProperties props, File output) {
		this.props = props;
		this.emailOutput = output;
		this.blogOutput = output;
		this.tweetOutput = output;
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
			File parentFile = file.getParentFile();
			parentFile.mkdirs();
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

	public File tweet() {
		File output = file(this.tweetOutput);
		String releaseVersion = parsedVersion();
		Template template = template(TWITTER_TEMPLATE);
		return new TwitterTemplateGenerator(template, releaseVersion, output).tweet();
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
