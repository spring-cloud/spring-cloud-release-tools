package org.springframework.cloud.release.internal.sagan;

import java.net.URI;

import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
class RestTemplateSaganClient implements SaganClient {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(RestTemplateSaganClient.class);

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

	@Override
	public Release createOrUpdateRelease(String projectName, ReleaseUpdate releaseUpdate) {
		RequestEntity<ReleaseUpdate> request = RequestEntity
				.put(URI.create(this.baseUrl +"/project_metadata/" + projectName + "/releases"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.body(releaseUpdate);
		ResponseEntity<Release> entity = this.restTemplate
				.exchange(request, Release.class);
		Release release = entity.getBody();
		log.info("Response from Sagan\n\n[{}] \n with body [{}]", entity, release);
		return release;
	}
}
