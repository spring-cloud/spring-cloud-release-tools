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

	private static final Logger log = LoggerFactory
			.getLogger(RestTemplateSaganClient.class);

	private final RestTemplate restTemplate;

	private final String baseUrl;

	RestTemplateSaganClient(RestTemplate restTemplate, ReleaserProperties properties) {
		this.restTemplate = restTemplate;
		this.baseUrl = properties.getSagan().getBaseUrl();
	}

	@Override
	public Project getProject(String projectName) {
		return this.restTemplate.getForObject(
				this.baseUrl + "/project_metadata/{projectName}", Project.class,
				projectName);
	}

	@Override
	public Release getRelease(String projectName, String releaseVersion) {
		return this.restTemplate.getForObject(
				this.baseUrl
						+ "/project_metadata/{projectName}/releases/{releaseVersion}",
				Release.class, projectName, releaseVersion);
	}

	@Override
	public Release deleteRelease(String projectName, String releaseVersion) {
		ResponseEntity<Release> entity = this.restTemplate.exchange(
				this.baseUrl
						+ "/project_metadata/{projectName}/releases/{releaseVersion}",
				HttpMethod.DELETE, new HttpEntity<>(""), Release.class, projectName,
				releaseVersion);
		Release release = entity.getBody();
		log.info("Response from Sagan\n\n[{}] \n with body [{}]", entity, release);
		return release;
	}

	@Override
	public Project updateRelease(String projectName, List<ReleaseUpdate> releaseUpdates) {
		RequestEntity<List<ReleaseUpdate>> request = RequestEntity
				.put(URI.create(
						this.baseUrl + "/project_metadata/" + projectName + "/releases"))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.body(releaseUpdates);
		ResponseEntity<Project> entity = this.restTemplate.exchange(request,
				Project.class);
		Project project = entity.getBody();
		log.info("Response from Sagan\n\n[{}] \n with body [{}]", entity, project);
		return project;
	}

	@Override
	public Project patchProject(Project project) {
		RequestEntity<Project> request = RequestEntity
				.patch(URI.create(this.baseUrl + "/project_metadata/" + project.id))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
				.body(project);
		ResponseEntity<Project> entity = this.restTemplate.exchange(request,
				Project.class);
		Project updatedProject = entity.getBody();
		log.info("Response from Sagan\n\n[{}] \n with body [{}]", entity, updatedProject);
		return updatedProject;
	}

}
