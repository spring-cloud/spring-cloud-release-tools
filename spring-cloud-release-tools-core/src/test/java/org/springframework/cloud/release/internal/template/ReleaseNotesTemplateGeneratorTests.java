package org.springframework.cloud.release.internal.template;

import java.io.File;

import com.github.jknack.handlebars.Template;
import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.mockito.BDDMockito;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseNotesTemplateGeneratorTests {

	@Test
	public void should_grab_notes_from_cache_if_present() {
		Template template = BDDMockito.mock(Template.class);
		ReleaseNotesTemplateGenerator generator =
				new ReleaseNotesTemplateGenerator(template, "Foo.RELEASE", null, null, null);
		ReleaseNotesTemplateGenerator.CACHE.put("Foo.RELEASE", new File("pom.xml"));

		BDDAssertions.then(generator.releaseNotes()).isNotNull();

		BDDMockito.then(template).shouldHaveZeroInteractions();
	}
}