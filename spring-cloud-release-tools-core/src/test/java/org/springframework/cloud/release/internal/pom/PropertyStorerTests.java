package org.springframework.cloud.release.internal.pom;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.then;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertyStorerTests {

	@Mock Log log;
	@Mock ModifiedPomXMLEventReader pom;
	@InjectMocks PropertyStorer propertyStorer;

	@Test public void should_not_set_a_version_when_its_empty() throws Exception {
		this.propertyStorer.setPropertyVersionIfApplicable(new Project("foo", ""));

		then(this.log).should().warn(containsWarnMsgAboutEmptyVersion());
	}

	private String containsWarnMsgAboutEmptyVersion() {
		return BDDMockito.argThat(
				argument -> argument.contains("is empty. Will not set it"));
	}

}