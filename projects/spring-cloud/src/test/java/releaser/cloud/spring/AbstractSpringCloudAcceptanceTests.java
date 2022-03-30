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

package releaser.cloud.spring;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import releaser.internal.buildsystem.TestUtils;
import releaser.internal.spring.AbstractSpringAcceptanceTests;

import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.BDDAssertions.then;

public class AbstractSpringCloudAcceptanceTests extends AbstractSpringAcceptanceTests {

	public File springCloudConsulProject;

	public File springCloudBuildProject;

	@BeforeEach
	public void setupCloud() throws Exception {
		this.temporaryFolder = this.tmp.newFolder();
		this.springCloudConsulProject = new File(
				AbstractSpringAcceptanceTests.class.getResource("/projects/spring-cloud-consul").toURI());
		this.springCloudBuildProject = new File(
				AbstractSpringAcceptanceTests.class.getResource("/projects/spring-cloud-build").toURI());
		TestUtils.prepareLocalRepo();
		FileSystemUtils.copyRecursively(file("/projects/"), this.temporaryFolder);
	}

	public void consulPomParentVersionIsEqualTo(File project, String expected) {
		pomParentVersionIsEqualTo(project, "spring-cloud-starter-consul", expected);
	}

	public void thenAllDryRunStepsWereExecutedForEachProject(
			NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler) {
		nonAssertingTestProjectGitHandler.clonedProjects.stream()
				.filter(f -> !f.getName().contains("angel") && !f.getName().equals("spring-cloud")).forEach(project -> {
					then(Arrays.asList("spring-cloud-starter-build", "spring-cloud-consul"))
							.contains(pom(project).getArtifactId());
					then(new File("/tmp/executed_build")).exists();
					then(new File("/tmp/executed_deploy")).doesNotExist();
					then(new File("/tmp/executed_docs")).doesNotExist();
				});
	}

	public void assertThatClonedConsulProjectIsInSnapshots(File origin) {
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
	}

}
