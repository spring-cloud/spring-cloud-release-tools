package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.github.jknack.handlebars.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marcin Grzejszczak
 */
class EmailTemplateGenerator {

	private static final Logger log = LoggerFactory.getLogger(EmailTemplateGenerator.class);

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
			log.warn("Exception occurred while trying to generate an email template", e);
			return null;
		}
	}
}
