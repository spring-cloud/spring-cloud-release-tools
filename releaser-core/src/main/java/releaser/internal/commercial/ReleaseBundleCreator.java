/*
 * Copyright 2013-2024 the original author or authors.
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

package releaser.internal.commercial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.aql.AqlItem;
import org.jfrog.artifactory.client.aql.AqlQueryBuilder;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

/**
 * @author Ryan Baxter
 */
public class ReleaseBundleCreator {

	private static final Logger log = LoggerFactory.getLogger(ReleaseBundleCreator.class);

	public static final String RELEASE_TRAIN_BUNDLE_NAME = "TNZ-spring-cloud-commercial-release";

	private final Artifactory artifactory;

	private final ObjectMapper objectMapper;

	public ReleaseBundleCreator(ReleaserProperties properties) {
		log.info("Creating Artifactory client with URL [{}]", properties.getBundles().getRepoUrl());
		log.info("Creating Artifactory client with username [{}]", properties.getBundles().getRepoUsername());
		log.info("Creating Artifactory client with access token [{}]", properties.getBundles().getRepoAccessToken());
		this.artifactory = ArtifactoryClientBuilder.create().setUrl(properties.getBundles().getRepoUrl())
				.setUsername(properties.getBundles().getRepoUsername())
				.setAccessToken(properties.getBundles().getRepoAccessToken()).build();
		this.objectMapper = new ObjectMapper();
	}

	public boolean createReleaseBundle(List<String> repos, String version, String releaseBundleName)
			throws IOException {
		Map<String, Object> json = new HashMap<>();
		json.put("release_bundle_name", releaseBundleName);
		json.put("release_bundle_version", version);
		json.put("source_type", "aql");
		json.put("source", createAqlMap(repos, version));
		return createReleaseBundle(objectMapper.writeValueAsString(json));
	}

	private Map<String, Object> createAqlMap(List<String> repos, String version) {
		String aql = createAql(repos, version);
		Map<String, Object> aqlObject = new HashMap<>();
		aqlObject.put("aql", aql);
		return aqlObject;
	}

	private String createAql(List<String> repos, String version) {
		AqlQueryBuilder builder = new AqlQueryBuilder()
				.item(AqlItem.aqlItem("repo", AqlItem.aqlItem("$eq", "spring-enterprise-maven-prod-local")));

		AqlItem[] items = new AqlItem[repos.size()];
		for (String repo : repos) {
			items[repos.indexOf(repo)] = AqlItem.match("path", repo + "/" + version);
		}
		return builder.or(items).asc("path", "name").build();
	}

	public boolean createReleaseBundle(String json) throws IOException {
		log.info("Creating release bundle with JSON [{}]", json);

		ArtifactoryRequest aqlRequest = new ArtifactoryRequestImpl().method(ArtifactoryRequest.Method.POST)
				.apiUrl("lifecycle/api/v2/release_bundle").addQueryParam("project", "spring").addQueryParam("async", "false")
				.addHeader("X-JFrog-Signing-Key-Name", "packagesKey").requestType(ArtifactoryRequest.ContentType.JSON)
				.responseType(ArtifactoryRequest.ContentType.JSON).requestBody(json);
		return makeArtifactoryRequest(aqlRequest);
	}

	public boolean createReleaseTrainSourceBundle(List<ProjectVersion> projectsReleased, String version)
			throws IOException {
		log.info("Creating release train source bundle for projects {}", projectsReleased);
		Map<String, Object> json = new HashMap<>();
		json.put("release_bundle_name", RELEASE_TRAIN_BUNDLE_NAME);
		json.put("release_bundle_version", version);
		json.put("skip_docker_manifest_resolution", false);
		json.put("source_type", "release_bundles");
		json.put("source", createReleaseBundlesJson(projectsReleased));
		return createReleaseBundle(objectMapper.writeValueAsString(json));
	}

	private Map<String, Object> createReleaseBundlesJson(List<ProjectVersion> projectsReleased) {
		Map<String, Object> json = new HashMap<>();
		List<Map<String, Object>> releaseBundles = new ArrayList<>();
		projectsReleased.forEach(project -> releaseBundles.add(createReleaseBundleJson(project)));
		json.put("release_bundles", releaseBundles);
		return json;
	}

	private Map<String, Object> createReleaseBundleJson(ProjectVersion project) {
		Map<String, Object> json = new HashMap<>();
		json.put("project_key", "spring");
		json.put("repository_key", "spring-release-bundles-v2");
		json.put("release_bundle_name", createReleaseBundleName(project.projectName));
		json.put("release_bundle_version", project.version);
		return json;
	}

	public static String createReleaseBundleName(String projectName) {
		return "TNZ-" + projectName + "-commercial";
	}

	public boolean distributeReleaseTrainSourceBundle(String version) throws IOException {
		return distributeReleaseBundle(RELEASE_TRAIN_BUNDLE_NAME, version,
				objectMapper.writeValueAsString(createDistributionJson()));
	}

	public boolean getDistributedReleaseBundleStatus(String releaseBundleName, String version) throws IOException {
		ArtifactoryRequest request = new ArtifactoryRequestImpl().method(ArtifactoryRequest.Method.GET)
				.apiUrl("lifecycle/api/v2/distribution/export/status/" + releaseBundleName + "/" + version)
				.addQueryParam("project", "spring").responseType(ArtifactoryRequest.ContentType.JSON);
		return makeArtifactoryRequest(request);
	}

	public boolean distributeReleaseBundle(String releaseBundleName, String version, String json) throws IOException {
		log.info("Distributing release bundle with name [{}] and version[{}] and JSON data [{}]", releaseBundleName,
				version, json);
		if (getDistributedReleaseBundleStatus(releaseBundleName, version)) {
			log.info("Release bundle with name [{}] and version[{}] already distributed, deleting distributed release bundle", releaseBundleName, version);
//			if (deleteDistributedReleaseBundle(releaseBundleName, version)) {
//				log.info("Deleted distributed release bundle with name [{}] and version[{}]", releaseBundleName, version);
//			}
//			else {
//				log.error("Error deleting distributed release bundle with name [{}] and version[{}]", releaseBundleName, version);
//				return false;
//			}
		}
		ArtifactoryRequest request = new ArtifactoryRequestImpl().method(ArtifactoryRequest.Method.POST)
				.apiUrl("lifecycle/api/v2/distribution/distribute/" + releaseBundleName + "/" + version)
				.addQueryParam("project", "spring").requestType(ArtifactoryRequest.ContentType.JSON)
				.responseType(ArtifactoryRequest.ContentType.JSON).requestBody(json);
		return makeArtifactoryRequest(request);
	}

	public boolean deleteDistributedReleaseBundle(String releaseBundleName, String version) {
		log.info("Deleting distributed release bundle with name [{}] and version[{}]", releaseBundleName, version);
		ArtifactoryRequest request = new ArtifactoryRequestImpl().method(ArtifactoryRequest.Method.POST)
				.apiUrl("lifecycle/api/v2/distribution/remote_delete/" + releaseBundleName + "/" + version)
				.addQueryParam("project", "spring").requestType(ArtifactoryRequest.ContentType.JSON)
				.responseType(ArtifactoryRequest.ContentType.JSON).requestBody(createDeleteDistributionJson());
		try {
			return makeArtifactoryRequest(request);
		}
		catch (IOException e) {
			log.error("Error deleting distributed release bundle with name [{}] and version[{}]", releaseBundleName, version, e);
			return false;
		}
	}

	private boolean makeArtifactoryRequest(ArtifactoryRequest request) throws IOException {
		ArtifactoryResponse response = artifactory.restCall(request);
		if (!response.isSuccessResponse()) {
			log.warn("Artifactory request {} failed {}", request, response.getRawBody());
		}
		else {
			log.info("Artifactory request succeeded {}", response.getRawBody());
		}
		return response.isSuccessResponse();
	}

	private Map<String, Object> createDistributionJson() {
		Map<String, Object> json = new HashMap<>();
		json.put("auto_create_missing_repositories", "false");
		json.put("distribution_rules", List.of(Collections.singletonMap("site_name", "JP-SaaS")));
		json.put("modifications", createMappings());
		return json;
	}

	private Map<String, Object> createDeleteDistributionJson() {
		Map<String, Object> json = new HashMap<>();
		json.put("distribution_rules", List.of(Collections.singletonMap("site_name", "JP-SaaS")));
		return json;
	}

	private Map<String, Object> createMappings() {
		Map<String, Object> mappings = new HashMap<>();
		mappings.put("input", "spring-enterprise-maven-prod-local/(.*)");
		mappings.put("output", "spring-enterprise/$1");
		List<Map<String, Object>> mappingsArray = List.of(mappings);
		return Collections.singletonMap("mappings", mappingsArray);
	}

	public boolean distributeProjectReleaseBundle(String projectName, String version) throws IOException {
		return distributeReleaseBundle(projectName, version, objectMapper.writeValueAsString(createDistributionJson()));
	}

}
