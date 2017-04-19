package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.github.jknack.handlebars.Template;

/**
 * @author Marcin Grzejszczak
 */
class EmailTemplateGenerator {

	private final Template template;
	private final String releaseVersion;
	private final File emailOutput;

	EmailTemplateGenerator(Template template, String releaseVersion, File emailOutput) {
		this.template = template;
		this.releaseVersion = releaseVersion;
		this.emailOutput = emailOutput;
	}

	File email() {
		try {
			String email = this.template.apply(this.releaseVersion);
			Files.write(this.emailOutput.toPath(), email.getBytes());
			return this.emailOutput;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
