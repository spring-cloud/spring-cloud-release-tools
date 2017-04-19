package org.springframework.cloud.release.internal.template;

import java.io.File;

import org.junit.Test;
import org.springframework.cloud.release.internal.ReleaserProperties;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class TemplateGeneratorTests {

	@Test
	public void should_generate_email_from_template_for_tag_with_v_prefix() {
		ReleaserProperties props = new ReleaserProperties();
		props.getPom().setBranch("vDalston.RELEASE");
		File generatedMail = new TemplateGenerator(props).email();

		then(generatedMail).hasContent(expectedEmail());
	}

	@Test
	public void should_generate_email_from_template_for_tag_without_v_prefix() {
		ReleaserProperties props = new ReleaserProperties();
		props.getPom().setBranch("Dalston.RELEASE");
		File generatedMail = new TemplateGenerator(props).email();

		then(generatedMail).hasContent(expectedEmail());
	}

	private String expectedEmail() {
		return "Title:\n"
				+ "Spring Cloud Dalston.RELEASE available\n\n"
				+ "Content:\n"
				+ "All,\n" + "\n"
				+ "On behalf of the team and the community, I'm excited to announce Spring Cloud Dalston RELEASE Train release.\n"
				+ "\n"
				+ "link to blog post\n"
				+ "link to twitter\n\n"
				+ "Cheers,\n";
	}
}
