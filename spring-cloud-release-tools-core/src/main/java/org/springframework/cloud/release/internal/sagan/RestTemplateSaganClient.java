package org.springframework.cloud.release.internal.sagan;

import java.net.URI;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
class RestTemplateSaganClient implements SaganClient {

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
				.post(URI.create(this.baseUrl +"/project_metadata/" + projectName + "/releases"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.body(releaseUpdate);
		return this.restTemplate.exchange(request, Release.class).getBody();
	}
}
