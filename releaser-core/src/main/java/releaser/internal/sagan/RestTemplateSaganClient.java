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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;

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

	@Override
	public Project getProject(String projectName) {
		HttpHeaders headers = new HttpHeaders();
		headers.put("Accept", Collections.singletonList("application/hal+json"));
		Project project = this.restTemplate.exchange(this.baseUrl + "/projects/{projectName}", HttpMethod.GET,
				new HttpEntity<>(headers), Project.class, projectName).getBody();
		if (project == null) {
			return null;
		}

		EmbeddedProjectReleases body = this.restTemplate.exchange(this.baseUrl + "/projects/{projectName}/releases",
				HttpMethod.GET, new HttpEntity<>(headers), EmbeddedProjectReleases.class, projectName).getBody();
		project.setReleases(body._embedded.releases);
		return project;
	}

	@Override
	public Release getRelease(String projectName, String releaseVersion) {
		HttpHeaders headers = new HttpHeaders();
		headers.put("Accept", Collections.singletonList("application/hal+json"));
		return this.restTemplate.exchange(this.baseUrl + "/projects/{projectName}/releases/{releaseVersion}",
				HttpMethod.GET, new HttpEntity<>(headers), Release.class, projectName, releaseVersion).getBody();
	}

	@Override
	public boolean deleteRelease(String projectName, String releaseVersion) {
		ResponseEntity<Release> entity = this.restTemplate.exchange(
				this.baseUrl + "/projects/{projectName}/releases/{releaseVersion}", HttpMethod.DELETE,
				new HttpEntity<>(""), Release.class, projectName, releaseVersion);
		boolean deleted = entity.getStatusCode().is2xxSuccessful();
		log.info("Response from Sagan\n\n[{}] \n with status [{}]", entity, entity.getStatusCode());
		return deleted;
	}

	@Override
	public boolean addRelease(String projectName, ReleaseInput releaseInput) {
		RequestEntity<ReleaseInput> request = RequestEntity
				.post(URI.create(this.baseUrl + "/projects/" + projectName + "/releases"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE).body(releaseInput);
		log.info("Request to Sagans\n\n[{}]", request);
		ResponseEntity<Project> entity = this.restTemplate.exchange(request, Project.class);
		boolean added = entity.getStatusCode().is2xxSuccessful();
		log.info("Response from Sagan\n\n[{}]", entity);
		return added;
	}

	@Override
	public void patchProjectDetails(String projectName, ProjectDetails details) {
		RequestEntity<ProjectDetails> request = RequestEntity
				.patch(URI.create(this.baseUrl + "/projects/" + projectName + "/details"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE).body(details);
		ResponseEntity<Object> entity = this.restTemplate.exchange(request, Object.class);
		log.info("Response from Sagan\n\n[{}]", entity);
	}

	private static class EmbeddedProjectReleases {

		private ProjectReleases _embedded;

		public ProjectReleases get_embedded() {
			return this._embedded;
		}

		public void set_embedded(ProjectReleases _embedded) {
			this._embedded = _embedded;
		}

	}

	private static class ProjectReleases {

		private List<Release> releases;

		public List<Release> getReleases() {
			return this.releases;
		}

		public void setReleases(List<Release> releases) {
			this.releases = releases;
		}

	}

}
