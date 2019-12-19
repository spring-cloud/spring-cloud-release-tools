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

package releaser.internal.spring.meta;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mockito.BDDMockito;
import releaser.cloud.github.SpringCloudGithubIssuesAccessor;
import releaser.internal.ReleaserProperties;
import releaser.internal.docs.DocumentationUpdater;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.sagan.SaganUpdater;
import releaser.internal.spring.AbstractSpringAcceptanceTests;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
abstract class AbstractSpringMetaReleaseAcceptanceTests
		extends AbstractSpringAcceptanceTests {

	void thenAllDryRunStepsWereExecutedForEachProject(
			NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler) {
		nonAssertingTestProjectGitHandler.clonedProjects.stream()
				.filter(f -> !f.getName().contains("angel")
						&& !f.getName().equals("spring-cloud"))
				.forEach(project -> {
					then(Arrays.asList("spring-cloud-starter-build",
							"spring-cloud-consul"))
									.contains(pom(project).getArtifactId());
					then(new File("/tmp/executed_build")).exists();
					then(new File("/tmp/executed_deploy")).doesNotExist();
					then(new File("/tmp/executed_docs")).doesNotExist();
				});
	}

	ArgsBuilder metaReleaseArgs(File project) throws Exception {
		return new ArgsBuilder(project, this.tmp)
				.releaseTrainUrl("/projects/spring-cloud-release/")
				.projectsToSkip("spring-boot", "spring-cloud-build",
						"spring-cloud-commons", "spring-cloud-stream",
						"spring-cloud-task", "spring-cloud-function", "spring-cloud-aws",
						"spring-cloud-bus", "spring-cloud-config", "spring-cloud-netflix",
						"spring-cloud-cloudfoundry", "spring-cloud-gateway",
						"spring-cloud-security", "spring-cloud-zookeeper",
						"spring-cloud-sleuth", "spring-cloud-contract",
						"spring-cloud-vault")
				.mavenBuildCommand("echo '{{profiles}}' > /tmp/executed_build")
				.mavenPublishCommand("echo '{{profiles}}' > /tmp/executed_docs")
				.mavenDeployCommand("echo '{{profiles}}' > /tmp/executed_deploy")
				.gitOrgUrl("file://" + this.temporaryFolder.getAbsolutePath())
				.releaseTrainBomUrl(
						file("/projects/spring-cloud-release/").toURI().toString());
	}

	Map<String, String> edgwareSr10() {
		Map<String, String> versions = new LinkedHashMap<>();
		versions.put("spring-boot", "5.5.16.RELEASE");
		versions.put("spring-cloud-build", "5.3.11.RELEASE");
		versions.put("spring-cloud-commons", "5.3.5.RELEASE");
		versions.put("spring-cloud-stream", "Xitmars.SR4");
		versions.put("spring-cloud-task", "5.2.3.RELEASE");
		versions.put("spring-cloud-function", "5.0.1.RELEASE");
		versions.put("spring-cloud-aws", "5.2.3.RELEASE");
		versions.put("spring-cloud-bus", "5.3.4.RELEASE");
		versions.put("spring-cloud-config", "5.4.5.RELEASE");
		versions.put("spring-cloud-netflix", "5.4.6.RELEASE");
		versions.put("spring-cloud-cloudfoundry", "5.1.2.RELEASE");
		versions.put("spring-cloud-gateway", "5.0.2.RELEASE");
		versions.put("spring-cloud-security", "5.2.3.RELEASE");
		versions.put("spring-cloud-consul", "5.3.5.RELEASE");
		versions.put("spring-cloud-zookeeper", "5.2.2.RELEASE");
		versions.put("spring-cloud-sleuth", "5.3.5.RELEASE");
		versions.put("spring-cloud-contract", "5.2.6.RELEASE");
		versions.put("spring-cloud-vault", "5.1.2.RELEASE");
		versions.put("spring-cloud-release", "Edgware.SR10");
		return versions;
	}

	void thenAllStepsWereExecutedForEachProject(
			NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler) {
		nonAssertingTestProjectGitHandler.clonedProjects.stream()
				.filter(f -> !f.getName().contains("angel")
						&& !f.getName().equals("spring-cloud"))
				.forEach(project -> {
					then(Arrays.asList("spring-cloud-starter-build",
							"spring-cloud-consul"))
									.contains(pom(project).getArtifactId());
					then(new File("/tmp/executed_build")).exists();
					then(new File("/tmp/executed_deploy")).exists();
					then(new File("/tmp/executed_docs")).exists();
				});
	}

	void thenSaganWasCalled(SaganUpdater saganUpdater) {
		BDDMockito.then(saganUpdater).should(BDDMockito.atLeastOnce()).updateSagan(
				BDDMockito.any(File.class), BDDMockito.anyString(),
				BDDMockito.any(ProjectVersion.class),
				BDDMockito.any(ProjectVersion.class), BDDMockito.any(Projects.class));
	}

	void thenSaganWasNotCalled(SaganUpdater saganUpdater) {
		BDDMockito.then(saganUpdater).should(BDDMockito.never()).updateSagan(
				BDDMockito.any(File.class), BDDMockito.anyString(),
				BDDMockito.any(ProjectVersion.class),
				BDDMockito.any(ProjectVersion.class), BDDMockito.any(Projects.class));
	}

	void thenDocumentationWasUpdated(DocumentationUpdater documentationUpdater) {
		BDDMockito.then(documentationUpdater).should().updateDocsRepo(
				BDDMockito.any(Projects.class), BDDMockito.any(ProjectVersion.class),
				BDDMockito.anyString());
	}

	void thenDocumentationWasNotUpdated(DocumentationUpdater documentationUpdater) {
		BDDMockito.then(documentationUpdater).should(BDDMockito.never()).updateDocsRepo(
				BDDMockito.any(Projects.class), BDDMockito.any(ProjectVersion.class),
				BDDMockito.anyString());
	}

	void assertThatClonedConsulProjectIsInSnapshots(File origin) {
		pomVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
		consulPomParentVersionIsEqualTo(origin, "1.2.0.BUILD-SNAPSHOT");
	}

	static class NonAssertingTestProjectGitHubHandler extends ProjectGitHubHandler {

		boolean closedMilestones = false;

		boolean issueCreatedInSpringGuides = false;

		boolean issueCreatedInStartSpringIo = false;

		NonAssertingTestProjectGitHubHandler(ReleaserProperties properties) {
			super(properties, Collections.singletonList(
					SpringCloudGithubIssuesAccessor.springCloud(properties)));
		}

		@Override
		public void closeMilestone(ProjectVersion releaseVersion) {
			this.closedMilestones = true;
		}

		@Override
		public void createIssueInSpringGuides(Projects projects, ProjectVersion version) {
			this.issueCreatedInSpringGuides = true;
		}

		@Override
		public void createIssueInStartSpringIo(Projects projects,
				ProjectVersion version) {
			this.issueCreatedInStartSpringIo = true;
		}

		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "https://foo.bar.com/" + releaseVersion.toString();
		}

	}

}
