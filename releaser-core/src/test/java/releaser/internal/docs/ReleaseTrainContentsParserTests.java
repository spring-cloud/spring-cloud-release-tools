/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package releaser.internal.docs;

import java.io.File;
import java.net.URISyntaxException;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseTrainContentsParserTests {

	File finchley = new File(ReleaseTrainContentsParserTests.class
			.getResource("/projects/spring-cloud-wiki/Spring-Cloud-Finchley-Release-Notes.md").toURI());

	public ReleaseTrainContentsParserTests() throws URISyntaxException {
	}

	@Test
	public void should_retrieve_latest_release_version_name() {
		String releaseTrain = new ReleaseTrainContentsParser().latestReleaseTrainFromWiki(this.finchley);

		BDDAssertions.then(releaseTrain).isEqualTo("Finchley.SR2");
	}

}
