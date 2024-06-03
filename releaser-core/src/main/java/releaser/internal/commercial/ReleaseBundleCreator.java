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
import java.util.List;

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

/**
 * @author Ryan Baxter
 */
public class ReleaseBundleCreator {

	private static final Logger log = LoggerFactory.getLogger(ReleaseBundleCreator.class);

	private ReleaserProperties properties;

	private Artifactory artifactory;

	public ReleaseBundleCreator(ReleaserProperties properties) {
		this.properties = properties;
		this.artifactory = ArtifactoryClientBuilder.create().setUrl(properties.getBundles().getRepoUrl())
				.setUsername(properties.getBundles().getRepoUsername())
				.setPassword(properties.getBundles().getRepoPassword()).build();
	}

	public boolean createReleaseBundle(List<String> repos, String version) throws IOException {
		AqlQueryBuilder builder = new AqlQueryBuilder()
				.item(AqlItem.aqlItem("repo", AqlItem.aqlItem("$eq", "spring-enterprise-maven-prod-local")));

		AqlItem[] items = new AqlItem[repos.size()];
		for (String repo : repos) {
			items[repos.indexOf(repo)] = AqlItem.match("path", repo + "/" + version);
		}
		String aql = builder.or(items).asc("path", "name").build();
		return createReleaseBundle(aql);
	}

	public boolean createReleaseBundle(String aql) throws IOException {
		log.info("Creating release bundle with AQL [{}]", aql);

		ArtifactoryRequest aqlRequest = new ArtifactoryRequestImpl().method(ArtifactoryRequest.Method.POST)
				.apiUrl("lifecycle/api/v2/release_bundle").addQueryParam("project", "spring")
				.addHeader("X-JFrog-Signing-Key-Name", "packagesKey").requestType(ArtifactoryRequest.ContentType.JSON)
				.responseType(ArtifactoryRequest.ContentType.JSON).requestBody(aql);
		ArtifactoryResponse response = artifactory.restCall(aqlRequest);
		if (!response.isSuccessResponse()) {
			log.warn("Failed to create release bundle {}", response.getRawBody());
			return false;
		}
		log.info("Created release bundle {}", response.getRawBody());
		return true;

	}

}
