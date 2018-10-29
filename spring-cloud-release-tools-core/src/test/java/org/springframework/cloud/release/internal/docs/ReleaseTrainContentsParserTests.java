package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.net.URISyntaxException;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseTrainContentsParserTests {

	File finchley = new File(
			ReleaseTrainContentsParserTests.class
					.getResource("/projects/spring-cloud-wiki/Spring-Cloud-Finchley-Release-Notes.md")
					.toURI());

	public ReleaseTrainContentsParserTests() throws URISyntaxException {
	}

	@Test
	public void should_retrieve_latest_release_version_name() {
		String releaseTrain = new ReleaseTrainContentsParser()
				.latestReleaseTrainFromWiki(this.finchley);

		BDDAssertions.then(releaseTrain).isEqualTo("Finchley.SR2");
	}
}