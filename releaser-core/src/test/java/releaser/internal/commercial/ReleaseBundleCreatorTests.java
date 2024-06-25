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

import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
class ReleaseBundleCreatorTests {

	@RegisterExtension
	static WireMockExtension wiremockArtifactory = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort()).build();

	private static ReleaserProperties properties = new ReleaserProperties();

	@BeforeAll
	static void beforeAll() {

		properties.getBundles().setRepoUrl("http://localhost:" + wiremockArtifactory.getRuntimeInfo().getHttpPort());
		properties.getBundles().setRepoUsername("admin");
		properties.getBundles().setRepoAccessToken("password");
		properties.getBundles()
				.setRepos(Collections.singletonMap("spring-cloud-build",
						List.of("org/springframework/cloud/spring-cloud-build*",
								"org/springframework/cloud/spring-cloud-starter-build*",
								"org/springframework/cloud/spring-cloud-dependencies-parent*")));
	}

	@Test
	void testDistributeProjectReleaseBundle() throws Exception {
		ReleaseBundleCreator releaseBundleCreator = new ReleaseBundleCreator(properties);
		assertThat(releaseBundleCreator.distributeProjectReleaseBundle("TNZ-spring-cloud-contract-commercial", "4.0.7"))
				.isTrue();
	}

	@Test
	void testDistributeReleaseTrainBundle() throws Exception {
		ReleaseBundleCreator releaseBundleCreator = new ReleaseBundleCreator(properties);
		assertThat(releaseBundleCreator.distributeReleaseTrainSourceBundle("2022.0.7")).isTrue();
	}

	@Test
	void testCreateReleaseTrainBundle() throws Exception {
		ReleaseBundleCreator releaseBundleCreator = new ReleaseBundleCreator(properties);

		List<ProjectVersion> repos = List.of(new ProjectVersion("spring-cloud-build", "4.0.8"),
				new ProjectVersion("spring-cloud-starter", "2022.0.7"),
				new ProjectVersion("spring-cloud-config", "4.0.7"), new ProjectVersion("spring-cloud-vault", "4.0.7"));
		assertThat(releaseBundleCreator.createReleaseTrainSourceBundle(repos, "2022.0.7")).isTrue();
	}

	@Test
	void testCreateProjectReleaseBundle() throws Exception {
		ReleaseBundleCreator releaseBundleCreator = new ReleaseBundleCreator(properties);
		assertThat(
				releaseBundleCreator.createReleaseBundle(properties.getBundles().getRepos().get("spring-cloud-build"),
						"4.0.7", "TNZ-spring-cloud-build-commercial")).isTrue();
	}

}
