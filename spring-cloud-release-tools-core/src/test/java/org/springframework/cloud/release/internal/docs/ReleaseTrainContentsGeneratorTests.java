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

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaseTrainContentsGeneratorTests {

	@Test
	public void should_return_empty_contents_if_there_versions_are_lower_than_current_ones() {
		ReleaseTrainContentsGenerator generator = new ReleaseTrainContentsGenerator(
				new ReleaserProperties());

		String contents = generator.releaseTrainContents(contents(), olderReleaseTrain());

		BDDAssertions.then(contents).isEmpty();
	}

	@Test
	public void should_return_empty_contents_if_there_are_no_changes_in_the_versions() {
		ReleaseTrainContentsGenerator generator = new ReleaseTrainContentsGenerator(
				new ReleaserProperties());

		String contents = generator.releaseTrainContents(contents(),
				sameProjectsForCurrentGa());

		BDDAssertions.then(contents).isEmpty();
	}

	@Test
	public void should_return_contents_with_updated_current_release_train() {
		ReleaseTrainContentsGenerator generator = new ReleaseTrainContentsGenerator(
				new ReleaserProperties());

		String contents = generator.releaseTrainContents(contents(),
				newerProjectsForNewGa());

		BDDAssertions.then(contents)
				.contains("|Edgware.SR5|Finchley.SR2|Finchley.BUILD-SNAPSHOT|");
		BDDAssertions.then(contents).contains(
				" |spring-cloud-cli|1.4.1.RELEASE|3.0.0.RELEASE|2.0.1.BUILD-SNAPSHOT|");
	}

	@Test
	public void should_return_contents_with_updated_previous_release_train() {
		ReleaseTrainContentsGenerator generator = new ReleaseTrainContentsGenerator(
				new ReleaserProperties());

		String contents = generator.releaseTrainContents(contents(),
				newerProjectsForOldGa());

		BDDAssertions.then(contents)
				.contains("|Edgware.SR6|Finchley.SR1|Finchley.BUILD-SNAPSHOT|");
		BDDAssertions.then(contents).contains(
				" |spring-cloud-cli|3.0.0.RELEASE|2.0.0.RELEASE|2.0.1.BUILD-SNAPSHOT|");
	}

	@Test
	public void should_return_contents_with_updated_current_snapshot_train() {
		ReleaseTrainContentsGenerator generator = new ReleaseTrainContentsGenerator(
				new ReleaserProperties());

		String contents = generator.releaseTrainContents(contents(),
				newerProjectsForCurrentSnapshot());

		BDDAssertions.then(contents)
				.contains("|Edgware.SR5|Finchley.SR1|Finchley.BUILD-SNAPSHOT|");
		BDDAssertions.then(contents).contains(
				" |spring-cloud-cli|1.4.1.RELEASE|2.0.0.RELEASE|3.0.0.BUILD-SNAPSHOT|");
	}

	ReleaseTrainContents contents() {
		Title title = new Title("Edgware.SR5", "Finchley.SR1", "Finchley.BUILD-SNAPSHOT");
		List<Row> rows = Arrays.asList(
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
		return new ReleaseTrainContents(title, rows);
	}

	Projects olderReleaseTrain() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "2.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "2.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-release", "Dalston.SR1"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects sameProjectsForCurrentGa() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "2.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "2.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-release", "Finchley.SR1"),
				new ProjectVersion("spring-cloud-vault", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "2.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects newerProjectsForNewGa() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "3.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "3.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-release", "Finchley.SR2"),
				new ProjectVersion("spring-cloud-vault", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects newerProjectsForOldGa() {
		return new Projects(new ProjectVersion("spring-cloud-aws", "2.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-bus", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cli", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-commons", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-contract", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-config", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-netflix", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-security", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-cloudfoundry", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-consul", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-sleuth", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "3.0.0.RELEASE"),
				new ProjectVersion("spring-boot", "3.0.4.RELEASE"),
				new ProjectVersion("spring-cloud-task", "3.0.0.RELEASE"),
				new ProjectVersion("spring-cloud-release", "Edgware.SR6"),
				new ProjectVersion("spring-cloud-vault", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-gateway", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-openfeign", "3.0.1.RELEASE"),
				new ProjectVersion("spring-cloud-function", "1.0.0.RELEASE"));
	}

	Projects newerProjectsForCurrentSnapshot() {
		return new Projects(
				new ProjectVersion("spring-cloud-aws", "2.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-bus", "3.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-cli", "3.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-commons", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-contract", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-config", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-netflix", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-security", "3.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-cloudfoundry", "3.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-consul", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-sleuth", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-stream", "Elmhurst.SR1"),
				new ProjectVersion("spring-cloud-zookeeper", "3.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-boot", "3.0.4.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-task", "3.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-release", "Finchley.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-vault", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-gateway", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-openfeign", "3.0.1.BUILD-SNAPSHOT"),
				new ProjectVersion("spring-cloud-function", "1.0.0.BUILD-SNAPSHOT"));
	}

}
