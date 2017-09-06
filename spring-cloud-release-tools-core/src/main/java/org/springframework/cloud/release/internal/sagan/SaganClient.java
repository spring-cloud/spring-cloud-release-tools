package org.springframework.cloud.release.internal.sagan;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.web.client.RestTemplate;

/**
 * @author Marcin Grzejszczak
 */
class SaganClient {

	private final RestTemplate restTemplate;
	private final String baseUrl;

	SaganClient(RestTemplate restTemplate, ReleaserProperties properties) {
		this.restTemplate = restTemplate;
		this.baseUrl = properties.getSagan().getBaseUrl();
	}

	void getProject(String projectName) {
		this.restTemplate.getForObject(this.baseUrl + "/project_metadata/{projectName}", Object.class, projectName);
	}

	void getRelease(String projectName, String releaseVersion) {

	}

	// POST
	void createOrUpdateRelease(String projectName, Object release) {

	}
}
