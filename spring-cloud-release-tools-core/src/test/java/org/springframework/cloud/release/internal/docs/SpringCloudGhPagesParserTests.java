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

package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.net.URISyntaxException;

import org.assertj.core.api.BDDAssertions;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

/**
 * @author Marcin Grzejszczak
 */
public class SpringCloudGhPagesParserTests {

	@Rule
	public OutputCapture capture = new OutputCapture();

	File rawHtml = new File(SpringCloudGhPagesParserTests.class
			.getResource("/raw/spring-cloud-gh-pages-index.html").toURI());

	File wrongHtml = new File(SpringCloudGhPagesParserTests.class
			.getResource("/raw/some-file.html").toURI());

	public SpringCloudGhPagesParserTests() throws URISyntaxException {
	}

	@Test
	public void should_parse_the_components_table() {
		ReleaseTrainContents contents = new ReleaseTrainContentsParser()
				.parseProjectPage(this.rawHtml);

		BDDAssertions.then(contents).isNotNull();
		BDDAssertions.then(contents.title).isEqualTo(
				new Title("Edgware.SR5", "Finchley.SR1", "Finchley.BUILD-SNAPSHOT"));
		BDDAssertions.then(contents.rows).contains(
				new Row("spring-cloud-aws", "1.2.3.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-cloud-bus", "1.3.3.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-cloud-cli", "1.4.1.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-cloud-commons", "1.3.5.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-contract", "1.2.6.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-config", "1.4.5.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-netflix", "1.4.6.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-security", "1.2.3.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-cloud-cloudfoundry", "1.1.2.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-cloud-consul", "1.3.5.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-sleuth", "1.3.5.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-stream", "Ditmars.SR4", "Elmhurst.SR1",
						"Elmhurst.BUILD-SNAPSHOT"),
				new Row("spring-cloud-zookeeper", "1.2.2.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-boot", "1.5.16.RELEASE", "2.0.4.RELEASE",
						"2.0.4.BUILD-SNAPSHOT"),
				new Row("spring-cloud-task", "1.2.3.RELEASE", "2.0.0.RELEASE",
						"2.0.1.BUILD-SNAPSHOT"),
				new Row("spring-cloud-vault", "1.1.2.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-gateway", "1.0.2.RELEASE", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-openfeign", "", "2.0.1.RELEASE",
						"2.0.2.BUILD-SNAPSHOT"),
				new Row("spring-cloud-function", "1.0.1.RELEASE", "1.0.0.RELEASE",
						"1.0.1.BUILD-SNAPSHOT"));
	}

	@Test
	public void should_not_parse_the_components_table_when_markers_are_not_found() {
		ReleaseTrainContents contents = new ReleaseTrainContentsParser()
				.parseProjectPage(this.wrongHtml);

		BDDAssertions.then(contents).isNull();
		BDDAssertions.then(this.capture.toString())
				.contains("The page is missing the components table markers");
	}

}
