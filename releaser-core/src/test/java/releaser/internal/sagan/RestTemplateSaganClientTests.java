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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import releaser.internal.ReleaserProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@Disabled
@SpringBootTest(classes = RestTemplateSaganClientTests.Config.class)
@AutoConfigureStubRunner(ids = "io.spring.sagan:sagan-site")
public class RestTemplateSaganClientTests {

	@Value("${stubrunner.runningstubs.sagan-site.port}")
	Integer saganPort;

	@Autowired
	ObjectMapper objectMapper;

	SaganClient client;

	@BeforeEach
	public void setup() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken("foo");
		properties.getSagan().setBaseUrl("http://localhost:" + this.saganPort);
		this.client = saganClient(properties);
	}

	@Test
	@Disabled("TODO: The API has changed")
	public void should_get_a_project() {
		Project project = this.client.getProject("spring-boot");

		then(project.getSlug()).isEqualTo("spring-boot");
		then(project.getName()).isEqualTo("Spring Boot");
		then(project.getRepositoryUrl()).isEqualTo("https://github.com/spring-projects/spring-boot");
		then(project.getStatus()).isEqualTo("ACTIVE");
		then(project.getReleases()).isNotEmpty();
		Release release = project.getReleases().get(0);
		then(release.getStatus()).isEqualTo("PRERELEASE");
		then(release.getReferenceDocUrl())
				.isEqualTo("https://docs.spring.io/spring-boot/docs/2.4.0-M1/reference/html/");
		then(release.getApiDocUrl()).isEqualTo("https://docs.spring.io/spring-boot/docs/2.4.0-M1/api/");
		then(release.getVersion()).isEqualTo("2.4.0-M1");
	}

	@Test
	@Disabled("TODO: The API has changed")
	public void should_get_a_release() {
		Release release = this.client.getRelease("spring-boot", "2.3.0.RELEASE");

		then(release.getStatus()).isEqualTo("GENERAL_AVAILABILITY");
		then(release.getReferenceDocUrl()).isEqualTo("https://docs.spring.io/spring-boot/docs/current/reference/html/");
		then(release.getApiDocUrl()).isEqualTo("https://docs.spring.io/spring-boot/docs/current/api/");
		then(release.getVersion()).isEqualTo("2.3.0.RELEASE");
	}

	@Test
	@Disabled("TODO: The API has changed")
	public void should_delete_a_release() {
		boolean deleted = this.client.deleteRelease("spring-boot", "2.3.0.RELEASE");

		then(deleted).isTrue();
	}

	@Test
	@Disabled("TODO: The API has changed")
	public void should_update_a_release() {

		ReleaseInput releaseInput = new ReleaseInput();
		releaseInput.setVersion("2.2.0.RELEASE");
		releaseInput.setReferenceDocUrl("https://docs.spring.io/spring-boot/docs/{version}/reference/html/");
		releaseInput.setApiDocUrl("https://docs.spring.io/spring-boot/docs/{version}/api/");

		boolean added = this.client.addRelease("spring-boot", releaseInput);
		then(added).isTrue();
	}

	@Test
	@Disabled("no api yet https://github.com/spring-io/sagan/issues/1052")
	public void should_patch_a_project() throws IOException {
		String projectJson = "{\n  \"id\" : \"spring-framework\",\n  "
				+ "\"rawBootConfig\" : \"rawBootConfig\",\n  \"rawOverview\" : \"rawOverview\",\n  "
				+ "\"displayOrder\" : 2147483647,\n  \"projectReleases\" : [ ],"
				+ "\n  \"projectSamples\" : [ ],\n  \"mostCurrentRelease\" : {\n    \"present\" : false\n  },"
				+ "\n  \"nonMostCurrentReleases\" : [ ],\n  \"stackOverflowTagList\" : [ ],\n  \"topLevelProject\" : true\n}";
		Project project = this.objectMapper.readValue(projectJson, Project.class);

		Project patchedProject = this.client.patchProject(project);

		// then(patchedProject.id).isEqualTo("spring-framework");
		// then(patchedProject.name).isEqualTo("Spring Framework");
		// then(patchedProject.rawBootConfig).isEqualTo("rawBootConfig");
		// then(patchedProject.rawOverview).isEqualTo("rawOverview");
	}

	private Repository milestone() {
		Repository milestone = new Repository();
		milestone.id = "spring-milestones";
		milestone.name = "Spring Milestones";
		milestone.url = "https://repo.spring.io/libs-milestone";
		milestone.snapshotsEnabled = false;
		return milestone;
	}

	private Repository snapshots() {
		Repository snapshots = new Repository();
		snapshots.id = "spring-snapshots";
		snapshots.name = "Spring Snapshots";
		snapshots.url = "https://repo.spring.io/libs-snapshot";
		snapshots.snapshotsEnabled = true;
		return snapshots;
	}

	private SaganClient saganClient(ReleaserProperties properties) {
		RestTemplate restTemplate = restTemplate(properties);
		return new RestTemplateSaganClient(restTemplate, properties);
	}

	private RestTemplate restTemplate(ReleaserProperties properties) {
		return new RestTemplateBuilder().basicAuthentication(properties.getGit().getOauthToken(), "").build();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}

}
