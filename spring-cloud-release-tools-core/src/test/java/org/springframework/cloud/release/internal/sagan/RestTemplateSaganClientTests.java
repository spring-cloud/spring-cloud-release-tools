package org.springframework.cloud.release.internal.sagan;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestTemplateSaganClientTests.Config.class)
@AutoConfigureStubRunner(ids = "sagan:sagan-site")
public class RestTemplateSaganClientTests {

	@Value("${stubrunner.runningstubs.sagan-site.port}") Integer saganPort;
	SaganClient client;

	@Before
	public void setup() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getGit().setOauthToken("foo");
		properties.getSagan().setBaseUrl("http://localhost:" + saganPort);
		this.client = saganClient(properties);
	}

	@Test
	public void should_get_a_project() {
		Project project = this.client.getProject("spring-framework");

		then(project.id).isEqualTo("spring-framework");
		then(project.name).isEqualTo("Spring Framework");
		then(project.repoUrl).isEqualTo("http://github.com/spring-projects/spring-framework");
		then(project.siteUrl).isEqualTo("http://projects.spring.io/spring-framework");
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
	public void should_update_a_release() {
		ReleaseUpdate releaseUpdate = new ReleaseUpdate();
		releaseUpdate.groupId = "org.springframework";
		releaseUpdate.artifactId = "spring-context";
		releaseUpdate.version = "1.2.8.RELEASE";
		releaseUpdate.releaseStatus = "PRERELEASE";
		releaseUpdate.refDocUrl = "http://docs.spring.io/spring/docs/{version}/spring-framework-reference/";
		releaseUpdate.apiDocUrl = "http://docs.spring.io/spring/docs/{version}/javadoc-api/";


		\n  \"repository\" : {\n    \"id\" : \"spring-milestones\",\n    \"name\" : \"Spring Milestones\",\n    \"url\" : \"https://repo.spring.io/libs-milestone\",\n    \"snapshotsEnabled\" : false\n  }\n}, {\n  \"groupId\" : \"org.springframework\",\n  \"artifactId\" : \"spring-context\",\n  \"version\" : \"5.0.0.BUILD-SNAPSHOT\",\n  \"releaseStatus\" : \"SNAPSHOT\",\n  \"refDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/spring-framework-reference/\",\n  \"apiDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/javadoc-api/\",\n  \"repository\" : {\n    \"id\" : \"spring-snapshots\",\n    \"name\" : \"Spring Snapshots\",\n    \"url\" : \"https://repo.spring.io/libs-snapshot\",\n    \"snapshotsEnabled\" : true\n  }\n}, {\n  \"groupId\" : \"org.springframework\",\n  \"artifactId\" : \"spring-context\",\n  \"version\" : \"4.3.12.BUILD-SNAPSHOT\",\n  \"releaseStatus\" : \"SNAPSHOT\",\n  \"refDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/spring-framework-reference/htmlsingle/\",\n  \"apiDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/javadoc-api/\",\n  \"repository\" : {\n    \"id\" : \"spring-snapshots\",\n    \"name\" : \"Spring Snapshots\",\n    \"url\" : \"https://repo.spring.io/libs-snapshot\",\n    \"snapshotsEnabled\" : true\n  }\n}, {\n  \"groupId\" : \"org.springframework\",\n  \"artifactId\" : \"spring-context\",\n  \"version\" : \"4.3.11.RELEASE\",\n  \"releaseStatus\" : \"GENERAL_AVAILABILITY\",\n  \"current\" : true,\n  \"refDocUrl\" : \"http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/\",\n  \"apiDocUrl\" : \"http://docs.spring.io/spring/docs/current/javadoc-api/\"\n}, {\n  \"groupId\" : \"org.springframework\",\n  \"artifactId\" : \"spring-context\",\n  \"version\" : \"4.2.9.RELEASE\",\n  \"releaseStatus\" : \"GENERAL_AVAILABILITY\",\n  \"refDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/spring-framework-reference/htmlsingle/\",\n  \"apiDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/javadoc-api/\"\n}, {\n  \"groupId\" : \"org.springframework\",\n  \"artifactId\" : \"spring-context\",\n  \"version\" : \"3.2.18.RELEASE\",\n  \"releaseStatus\" : \"GENERAL_AVAILABILITY\",\n  \"refDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/spring-framework-reference/htmlsingle/\",\n  \"apiDocUrl\" : \"http://docs.spring.io/spring/docs/{version}/javadoc-api/\"\n} ]"


		Release release = this.client.createOrUpdateRelease("spring-framework", releaseUpdate);

		then(release.releaseStatus).isEqualTo("GENERAL_AVAILABILITY");
		then(release.refDocUrl).isEqualTo("http://docs.spring.io/spring/docs/1.2.3.RELEASE/spring-framework-reference/");
		then(release.apiDocUrl).isEqualTo("http://docs.spring.io/spring/docs/1.2.3.RELEASE/javadoc-api/");
		then(release.groupId).isEqualTo("org.springframework");
		then(release.artifactId).isEqualTo("spring-context");
		then(release.version).isEqualTo("1.2.3.RELEASE");
		then(release.current).isFalse();
		then(release.generalAvailability).isTrue();
		then(release.preRelease).isFalse();
		then(release.versionDisplayName).isEqualTo("1.2.3");
		then(release.snapshot).isFalse();
	}

	private SaganClient saganClient(ReleaserProperties properties) {
		RestTemplate restTemplate = restTemplate(properties);
		return new RestTemplateSaganClient(restTemplate, properties);
	}

	private RestTemplate restTemplate(ReleaserProperties properties) {
		return new RestTemplateBuilder()
				.basicAuthorization(properties.getGit().getOauthToken(), "")
				.build();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {

	}
}