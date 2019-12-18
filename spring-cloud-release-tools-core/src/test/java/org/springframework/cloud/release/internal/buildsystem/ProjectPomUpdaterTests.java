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

package org.springframework.cloud.release.internal.buildsystem;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.mockito.BDDMockito;

import org.springframework.cloud.release.SpringCloudReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.ProjectVersion;
import org.springframework.cloud.release.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectPomUpdaterTests {

	@Test
	public void should_convert_fixed_versions_to_updated_fixed_versions() {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		properties.getFixedVersions().put("spring-cloud-task", "2.0.0.RELEASE");
		properties.getFixedVersions().put("spring-cloud-openfeign",
				"2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-consul", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-zookeeper",
				"2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-stream", "Elmhurst.RELEASE");
		properties.getFixedVersions().put("spring-cloud-config", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-cloudfoundry",
				"2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-netflix", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-vault", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-security",
				"2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-commons", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-sleuth", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-aws", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-contract",
				"2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-release",
				"Finchley.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-build", "2.0.3.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-bus", "2.0.1.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-function",
				"1.0.0.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-cloud-starter-build",
				"Finchley.BUILD-SNAPSHOT");
		properties.getFixedVersions().put("spring-boot", "2.0.3.RELEASE");
		properties.getFixedVersions().put("spring-cloud-gateway", "2.0.1.BUILD-SNAPSHOT");
		ProjectPomUpdater updater = new ProjectPomUpdater(properties, Collections
				.singletonList(MavenBomParserAccessor.cloudMavenBomParser(properties)));

		Map<String, String> fixedVersions = updater.fixedVersions().stream()
				.collect(Collectors.toMap(projectVersion -> projectVersion.projectName,
						projectVersion -> projectVersion.version));

		BDDAssertions.then(fixedVersions).containsEntry("spring-boot", "2.0.3.RELEASE")
				.containsEntry("spring-boot-dependencies", "2.0.3.RELEASE")
				.containsEntry("spring-boot-starter", "2.0.3.RELEASE")
				.containsEntry("spring-cloud-build", "2.0.3.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-dependencies-parent", "2.0.3.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-dependencies", "Finchley.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud-release", "Finchley.BUILD-SNAPSHOT")
				.containsEntry("spring-cloud", "Finchley.BUILD-SNAPSHOT");
	}

	@Test
	public void should_skip_any_steps_if_there_is_no_pom_xml() {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		ProjectGitHandler handler = BDDMockito.mock(ProjectGitHandler.class);
		ProjectPomUpdater updater = new ProjectPomUpdater(properties, Collections
				.singletonList(MavenBomParserAccessor.cloudMavenBomParser(properties)));

		updater.updateProjectFromReleaseTrain(new File("target"), new Projects(),
				new ProjectVersion("foo", "1.0.0.RELEASE"), false);

		BDDMockito.then(handler).shouldHaveZeroInteractions();
	}

}
