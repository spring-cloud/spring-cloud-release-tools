package org.springframework.cloud.release.internal.template;

import com.github.jknack.handlebars.Template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Marcin Grzejszczak
 */
class TwitterTemplateGenerator {
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
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
