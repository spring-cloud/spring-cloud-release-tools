package org.springframework.cloud.release.internal.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.cloud.release.internal.ReleaserProperties;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;

/**
 * @author Marcin Grzejszczak
 */
public class TemplateGenerator {

	private final String emailTemplate = "/templates/email";
	private final File emailOutput = new File("target/email.txt");
	private final ReleaserProperties props;

	public TemplateGenerator(ReleaserProperties props) {
		this.props = props;
	}

	public File email() {
		try {
			Template template = uncheckedCompileTemplate();
			String releaseVersion = parsedVersion();
			String email = template.apply(releaseVersion);
			if (emailOutput.exists()) {
				emailOutput.delete();
			}
			if (!emailOutput.createNewFile()) {
				throw new IllegalStateException("Couldn't create a file with email template");
			}
			Files.write(emailOutput.toPath(), email.getBytes());
			return emailOutput;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String parsedVersion() {
		String version = this.props.getPom().getBranch();
		if (version.startsWith("v")) {
			return version.substring(1);
		}
		return version;
	}

	private Template uncheckedCompileTemplate() {
		try {
			Handlebars handlebars = new Handlebars();
			handlebars.registerHelper("replace", StringHelpers.replace);
			handlebars.registerHelper("capitalizeFirst", StringHelpers.capitalizeFirst);
			return handlebars.compile(this.emailTemplate);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
