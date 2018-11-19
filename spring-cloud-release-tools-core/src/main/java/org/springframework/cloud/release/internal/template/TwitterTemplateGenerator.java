package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Template;

/**
 * @author Marcin Grzejszczak
 */
class TwitterTemplateGenerator {
	private static final Logger log = LoggerFactory.getLogger(TwitterTemplateGenerator.class);

	private final Template template;
	private final String releaseVersion;
	private final File output;

	TwitterTemplateGenerator(Template template, String releaseVersion, File output) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.output = output;
	}

	File tweet() {
		try {
			String tweet = this.template.apply(this.releaseVersion);
			Files.write(this.output.toPath(), tweet.getBytes());
			return this.output;
		}
		catch (Exception e) {
			throw new IllegalStateException("Exception occurred while trying to generate a twitter template", e);
		}
	}
}
