/*
 * Copyright 2013-2022 the original author or authors.
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

package releaser.internal.sagan;

import java.io.IOException;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import releaser.internal.ReleaserProperties;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@WireMockTest(httpPort = 23456)
class RestTemplateSaganClientTests {

	RestTemplateSaganClient client;

	@BeforeEach
	void setup() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken("foo");
		properties.getSagan().setBaseUrl("http://localhost:23456");
		this.client = saganClient(properties);
	}

	@Test
	void should_get_a_project() {
		Project project = this.client.getProject("spring-boot");

		then(project.getSlug()).isEqualTo("spring-boot");
		then(project.getName()).isEqualTo("Spring Boot");
		then(project.getRepositoryUrl()).isEqualTo("https://github.com/spring-projects/spring-boot");
		then(project.getStatus()).isEqualTo("ACTIVE");
		then(project.getReleases()).isNotEmpty();
		Release release = project.getReleases().get(0);
		then(release.getStatus()).isEqualTo("GENERAL_AVAILABILITY");
		then(release.getReferenceDocUrl())
				.isEqualTo("https://docs.spring.io/spring-boot/docs/{version}/reference/html/");
		then(release.getApiDocUrl()).isEqualTo("https://docs.spring.io/spring-boot/docs/{version}/api/");
		then(release.getVersion()).isEqualTo("2.5.14");
	}

	@Test
	void should_get_a_release() {
		Release release = this.client.getRelease("spring-boot", "2.5.14");

		then(release.getStatus()).isEqualTo("GENERAL_AVAILABILITY");
		then(release.getReferenceDocUrl())
				.isEqualTo("https://docs.spring.io/spring-boot/docs/{version}/reference/html/");
		then(release.getApiDocUrl()).isEqualTo("https://docs.spring.io/spring-boot/docs/{version}/api/");
		then(release.getVersion()).isEqualTo("2.5.14");
	}

	@Test
	void should_delete_a_release() {
		boolean deleted = this.client.deleteRelease("spring-cloud-contract", "4.0.3-SNAPSHOT");

		then(deleted).isTrue();
	}

	@Test
	void should_update_a_release() {
		ReleaseInput releaseInput = new ReleaseInput();
		releaseInput.setVersion("4.0.3-SNAPSHOT");
		releaseInput.setReferenceDocUrl("https://docs.spring.io/spring-cloud-contract/docs/{version}/reference/html/");
		releaseInput.setApiDocUrl("https://docs.spring.io/spring-cloud-contract/docs/{version}/api/");

		boolean added = this.client.addRelease("spring-cloud-contract", releaseInput);

		then(added).isTrue();
	}

	@Test
	void should_patch_a_project() throws IOException {
		ProjectDetails projectDetails = new ProjectDetails();
		projectDetails.setBody("new body");
		projectDetails.setBootConfig("new sbc");

		BDDAssertions.thenNoException()
				.isThrownBy(() -> this.client.patchProjectDetails("spring-cloud-contract", projectDetails));
	}

	private RestTemplateSaganClient saganClient(ReleaserProperties properties) {
		RestTemplate restTemplate = restTemplate(properties);
		return new RestTemplateSaganClient(restTemplate, properties);
	}

	private RestTemplate restTemplate(ReleaserProperties properties) {
		return new RestTemplateBuilder().basicAuthentication(properties.getGit().getOauthToken(), "").build();
	}

}
