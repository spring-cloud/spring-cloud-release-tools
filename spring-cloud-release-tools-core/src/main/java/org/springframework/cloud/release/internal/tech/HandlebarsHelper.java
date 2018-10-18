package org.springframework.cloud.release.internal.tech;

import java.io.IOException;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

/**
 * @author Marcin Grzejszczak
 */
public final class HandlebarsHelper {

	public static Template template(String templateSubFolder, String templateName) {
		try {
			Handlebars handlebars = new Handlebars(new ClassPathTemplateLoader("/templates/" +
					templateSubFolder));
			handlebars.registerHelper("replace", StringHelpers.replace);
			handlebars.registerHelper("capitalizeFirst", StringHelpers.capitalizeFirst);
			return handlebars.compile(templateName);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
