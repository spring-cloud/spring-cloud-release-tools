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

package releaser.internal.sagan;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
@SpringBootTest(classes = RestTemplateSaganClientTests.Config.class)
@AutoConfigureStubRunner(ids = "sagan:sagan-site")
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
	@Disabled("2 stubs point to the same endpoint")
	public void should_get_a_project() {
		Project project = this.client.getProject("spring-framework");

		then(project.id).isEqualTo("spring-framework");
		then(project.name).isEqualTo("Spring Framework");
		then(project.repoUrl).isEqualTo("https://github.com/spring-projects/spring-framework");
		then(project.siteUrl).isEqualTo("https://projects.spring.io/spring-framework");
		then(project.category).isEqualTo("active");
		then(project.stackOverflowTags).isNotBlank();
		then(project.stackOverflowTagList).isNotEmpty();
		then(project.aggregator).isFalse();
		then(project.projectReleases).isNotEmpty();
		Release release = project.projectReleases.get(0);
		then(release.releaseStatus).isEqualTo("PRERELEASE");
		then(release.refDocUrl).isEqualTo("http://docs.spring.io/spring/docs/5.0.0.RC4/spring-framework-reference/");
		then(release.apiDocUrl).isEqualTo("http://docs.spring.io/spring/docs/5.0.0.RC4/javadoc-api/");
		then(release.groupId).isEqualTo("org.springframework");
		then(release.artifactId).isEqualTo("spring-context");
		then(release.repository.id).isEqualTo("spring-milestones");
		then(release.repository.name).isEqualTo("Spring Milestones");
		then(release.repository.url).isEqualTo("https://repo.spring.io/libs-milestone");
		then(release.repository.snapshotsEnabled).isFalse();
		then(release.version).isEqualTo("5.0.0.RC4");
		then(release.current).isFalse();
		then(release.generalAvailability).isFalse();
		then(release.preRelease).isTrue();
		then(release.versionDisplayName).isEqualTo("5.0.0 RC4");
		then(release.snapshot).isFalse();
	}

	@Test
	public void should_get_a_release() {
		Release release = this.client.getRelease("spring-framework", "5.0.0.RC4");

		then(release.releaseStatus).isEqualTo("PRERELEASE");
		then(release.refDocUrl).isEqualTo("http://docs.spring.io/spring/docs/{version}/spring-framework-reference/");
		then(release.apiDocUrl).isEqualTo("http://docs.spring.io/spring/docs/{version}/javadoc-api/");
		then(release.groupId).isEqualTo("org.springframework");
		then(release.artifactId).isEqualTo("spring-context");
		then(release.repository.id).isEqualTo("spring-milestones");
		then(release.repository.name).isEqualTo("Spring Milestones");
		then(release.repository.url).isEqualTo("https://repo.spring.io/libs-milestone");
		then(release.repository.snapshotsEnabled).isFalse();
		then(release.version).isEqualTo("5.0.0.RC4");
		then(release.current).isFalse();
		then(release.generalAvailability).isFalse();
		then(release.preRelease).isTrue();
		then(release.versionDisplayName).isEqualTo("5.0.0 RC4");
		then(release.snapshot).isFalse();
	}

	@Test
	public void should_delete_a_release() {
		Release release = this.client.deleteRelease("spring-framework", "5.0.0.RC4");

		then(release).isNotNull();
		then(release.releaseStatus).isEqualTo("PRERELEASE");
	}

	@Test
	public void should_update_a_release() {
		Repository snapshots = snapshots();
		Repository milestone = milestone();

		ReleaseUpdate firstRelease = new ReleaseUpdate();
		firstRelease.groupId = "org.springframework";
		firstRelease.artifactId = "spring-context";
		firstRelease.version = "1.2.8.RELEASE";
		firstRelease.releaseStatus = "PRERELEASE";
		firstRelease.refDocUrl = "http://docs.spring.io/spring/docs/{version}/spring-framework-reference/";
		firstRelease.apiDocUrl = "http://docs.spring.io/spring/docs/{version}/javadoc-api/";
		firstRelease.repository = milestone;

		ReleaseUpdate secondRelease = new ReleaseUpdate();
		secondRelease.groupId = "org.springframework";
		secondRelease.artifactId = "spring-context";
		secondRelease.version = "5.0.0.BUILD-SNAPSHOT";
		secondRelease.releaseStatus = "SNAPSHOT";
		secondRelease.refDocUrl = "http://docs.spring.io/spring/docs/{version}/spring-framework-reference/";
		secondRelease.apiDocUrl = "http://docs.spring.io/spring/docs/{version}/javadoc-api/";
		secondRelease.repository = snapshots;

		ReleaseUpdate thirdRelease = new ReleaseUpdate();
		thirdRelease.groupId = "org.springframework";
		thirdRelease.artifactId = "spring-context";
		thirdRelease.version = "4.3.12.BUILD-SNAPSHOT";
		thirdRelease.releaseStatus = "SNAPSHOT";
		thirdRelease.refDocUrl = "http://docs.spring.io/spring/docs/{version}/spring-framework-reference/htmlsingle/";
		thirdRelease.apiDocUrl = "http://docs.spring.io/spring/docs/{version}/javadoc-api/";
		thirdRelease.repository = snapshots;

		ReleaseUpdate fourthRelease = new ReleaseUpdate();
		fourthRelease.groupId = "org.springframework";
		fourthRelease.artifactId = "spring-context";
		fourthRelease.version = "4.3.11.RELEASE";
		fourthRelease.releaseStatus = "GENERAL_AVAILABILITY";
		fourthRelease.current = true;
		fourthRelease.refDocUrl = "http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/";
		fourthRelease.apiDocUrl = "http://docs.spring.io/spring/docs/current/javadoc-api/";

		ReleaseUpdate fithRelease = new ReleaseUpdate();
		fithRelease.groupId = "org.springframework";
		fithRelease.artifactId = "spring-context";
		fithRelease.version = "4.2.9.RELEASE";
		fithRelease.releaseStatus = "GENERAL_AVAILABILITY";
		fithRelease.refDocUrl = "http://docs.spring.io/spring/docs/{version}/spring-framework-reference/htmlsingle/";
		fithRelease.apiDocUrl = "http://docs.spring.io/spring/docs/{version}/javadoc-api/";

		ReleaseUpdate sithRelease = new ReleaseUpdate();
		sithRelease.groupId = "org.springframework";
		sithRelease.artifactId = "spring-context";
		sithRelease.version = "3.2.18.RELEASE";
		sithRelease.releaseStatus = "GENERAL_AVAILABILITY";
		sithRelease.refDocUrl = "http://docs.spring.io/spring/docs/{version}/spring-framework-reference/htmlsingle/";
		sithRelease.apiDocUrl = "http://docs.spring.io/spring/docs/{version}/javadoc-api/";

		List<ReleaseUpdate> updates = Arrays.asList(firstRelease, secondRelease, thirdRelease, fourthRelease,
				fithRelease, sithRelease);

		Project project = this.client.updateRelease("spring-framework", updates);

		then(project.id).isEqualTo("spring-framework");
		then(project.name).isEqualTo("Spring Framework");
		then(project.projectReleases).hasSize(7);
	}

	@Test
	public void should_patch_a_project() throws IOException {
		String projectJson = "{\n  \"id\" : \"spring-framework\",\n  "
				+ "\"rawBootConfig\" : \"rawBootConfig\",\n  \"rawOverview\" : \"rawOverview\",\n  "
				+ "\"displayOrder\" : 2147483647,\n  \"projectReleases\" : [ ],"
				+ "\n  \"projectSamples\" : [ ],\n  \"mostCurrentRelease\" : {\n    \"present\" : false\n  },"
				+ "\n  \"nonMostCurrentReleases\" : [ ],\n  \"stackOverflowTagList\" : [ ],\n  \"topLevelProject\" : true\n}";
		Project project = this.objectMapper.readValue(projectJson, Project.class);

		Project patchedProject = this.client.patchProject(project);

		then(patchedProject.id).isEqualTo("spring-framework");
		then(patchedProject.name).isEqualTo("Spring Framework");
		then(patchedProject.rawBootConfig).isEqualTo("rawBootConfig");
		then(patchedProject.rawOverview).isEqualTo("rawOverview");
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
