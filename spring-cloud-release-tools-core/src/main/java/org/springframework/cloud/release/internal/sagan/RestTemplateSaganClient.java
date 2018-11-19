package org.springframework.cloud.release.internal.sagan;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
class RestTemplateSaganClient implements SaganClient {

	private static final Logger log = LoggerFactory.getLogger(RestTemplateSaganClient.class);

	private final RestTemplate restTemplate;
	private final String baseUrl;

	RestTemplateSaganClient(RestTemplate restTemplate, ReleaserProperties properties) {
		this.restTemplate = restTemplate;
		this.baseUrl = properties.getSagan().getBaseUrl();
	}

	@Override public Project getProject(String projectName) {
		return this.restTemplate.getForObject(this.baseUrl + "/project_metadata/{projectName}", Project.class, projectName);
	}

	@Override public Release getRelease(String projectName, String releaseVersion) {
		return this.restTemplate.getForObject(this.baseUrl + "/project_metadata/{projectName}/releases/{releaseVersion}", Release.class, projectName, releaseVersion);
	}

	@Override public Release deleteRelease(String projectName, String releaseVersion) {
		ResponseEntity<Release> entity = this.restTemplate.exchange(this.baseUrl + "/project_metadata/{projectName}/releases/{releaseVersion}",
				HttpMethod.DELETE, new HttpEntity<>(""), Release.class, projectName, releaseVersion);
		Release release = entity.getBody();
		log.info("Response from Sagan\n\n[{}] \n with body [{}]", entity, release);
		return release;
	}

	@Override
	public Project updateRelease(String projectName, List<ReleaseUpdate> releaseUpdates) {
		RequestEntity<List<ReleaseUpdate>> request = RequestEntity
				.put(URI.create(this.baseUrl +"/project_metadata/" + projectName + "/releases"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.body(releaseUpdates);
		ResponseEntity<Project> entity = this.restTemplate
				.exchange(request, Project.class);
		Project project = entity.getBody();
		log.info("Response from Sagan\n\n[{}] \n with body [{}]", entity, project);
		return project;
	}
}
