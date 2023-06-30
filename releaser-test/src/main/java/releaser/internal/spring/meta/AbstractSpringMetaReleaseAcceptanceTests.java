/*
 * Copyright 2013-2022 the original author or authors.
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
import releaser.internal.ReleaserProperties;
import releaser.internal.github.ProjectGitHubHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.sagan.SaganUpdater;
import releaser.internal.spring.AbstractSpringAcceptanceTests;
import releaser.internal.spring.ArgsBuilder;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public abstract class AbstractSpringMetaReleaseAcceptanceTests extends AbstractSpringAcceptanceTests {

	public ArgsBuilder metaReleaseArgs(File project, File tempDirTestSamplesProject, File tempDirReleaseTrainDocs,
			File tempDirSpringCloud, File tempDirReleaseTrainWiki, File tempDirAllTestSample) throws Exception {
		return new ArgsBuilder(project, tempDirTestSamplesProject, tempDirReleaseTrainDocs, tempDirSpringCloud,
				tempDirReleaseTrainWiki, tempDirAllTestSample)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.projectsToSkip("spring-boot", "spring-cloud-build", "spring-cloud-commons",
								"spring-cloud-circuitbreaker", "spring-cloud-stream", "spring-cloud-task",
								"spring-cloud-function", "spring-cloud-aws", "spring-cloud-bus", "spring-cloud-config",
								"spring-cloud-netflix", "spring-cloud-kubernetes", "spring-cloud-cloudfoundry",
								"spring-cloud-gateway", "spring-cloud-security", "spring-cloud-openfeign",
								"spring-cloud-zookeeper", "spring-cloud-sleuth", "spring-cloud-contract",
								"spring-cloud-vault")
						.mavenBuildCommand("echo '{{profiles}}' > /tmp/executed_build")
						.mavenPublishCommand("echo '{{profiles}}' > /tmp/executed_docs")
						.mavenDeployCommand("echo '{{profiles}}' > /tmp/executed_deploy")
						.gitOrgUrl("file://" + this.temporaryFolder.getAbsolutePath())
						.releaseTrainBomUrl(file("/projects/spring-cloud-release/").toURI().toString());
	}

	public ArgsBuilder metaReleaseArgsForParallel(File project, File tempDirTestSamplesProject,
			File tempDirReleaseTrainDocs, File tempDirSpringCloud, File tempDirReleaseTrainWiki,
			File tempDirAllTestSample) throws Exception {
		return new ArgsBuilder(project, tempDirTestSamplesProject, tempDirReleaseTrainDocs, tempDirSpringCloud,
				tempDirReleaseTrainWiki, tempDirAllTestSample)
						.releaseTrainUrl("/projects/spring-cloud-release/")
						.projectsToSkip("spring-boot", "spring-cloud-build", "spring-cloud-commons",
								"spring-cloud-circuitbreaker", "spring-cloud-stream", "spring-cloud-task",
								"spring-cloud-function", "spring-cloud-aws", "spring-cloud-bus", "spring-cloud-config",
								"spring-cloud-netflix", "spring-cloud-kubernetes", "spring-cloud-cloudfoundry",
								"spring-cloud-gateway", "spring-cloud-security", "spring-cloud-openfeign",
								"spring-cloud-zookeeper", "spring-cloud-sleuth", "spring-cloud-contract",
								"spring-cloud-vault")
						.mavenBuildCommand("echo '{{profiles}}' > /tmp/executed_build")
						.mavenPublishCommand("echo '{{profiles}}' > /tmp/executed_docs")
						.mavenDeployCommand("echo '{{profiles}}' > /tmp/executed_deploy")
						.gitOrgUrl("file://" + this.temporaryFolder.getAbsolutePath())
						.releaseTrainBomUrl(file("/projects/spring-cloud-release/").toURI().toString());
	}

	public Map<String, String> v2022_0_4() {
		Map<String, String> versions = new LinkedHashMap<>();
		versions.put("spring-boot", "3.0.7");
		versions.put("spring-cloud-build", "4.0.3");
		versions.put("spring-cloud-function", "4.0.3");
		versions.put("spring-cloud-stream", "4.0.3");
		versions.put("spring-cloud-commons", "4.0.3");
		versions.put("spring-cloud-bus", "4.0.1");
		versions.put("spring-cloud-task", "3.0.3");
		versions.put("spring-cloud-config", "4.0.3");
		versions.put("spring-cloud-netflix", "4.0.2");
		versions.put("spring-cloud-openfeign", "4.0.3");
		versions.put("spring-cloud-consul", "4.0.2");
		versions.put("spring-cloud-circuitbreaker", "3.0.2");
		versions.put("spring-cloud-gateway", "4.0.6");
		versions.put("spring-cloud-zookeeper", "4.0.0");
		versions.put("spring-cloud-contract", "4.0.3");
		versions.put("spring-cloud-kubernetes", "3.0.3");
		versions.put("spring-cloud-vault", "4.0.1");
		versions.put("spring-cloud-release", "2022.0.4");
		return versions;
	}

	public void thenAllStepsWereExecutedForEachProject(
			NonAssertingTestProjectGitHandler nonAssertingTestProjectGitHandler) {
		nonAssertingTestProjectGitHandler.clonedProjects.stream()
				.filter(f -> !f.getName().contains("angel") && !f.getName().equals("spring-cloud")).forEach(project -> {
					then(Arrays.asList("spring-cloud-starter-build", "spring-cloud-consul"))
							.contains(pom(project).getArtifactId());
					then(new File("/tmp/executed_build")).exists();
					then(new File("/tmp/executed_deploy")).exists();
					then(new File("/tmp/executed_docs")).exists();
				});
	}

	public void thenSaganWasCalled(SaganUpdater saganUpdater) {
		BDDMockito.then(saganUpdater).should(BDDMockito.atLeastOnce()).updateSagan(BDDMockito.any(File.class),
				BDDMockito.anyString(), BDDMockito.any(ProjectVersion.class), BDDMockito.any(ProjectVersion.class),
				BDDMockito.any(Projects.class));
	}

	public void thenSaganWasNotCalled(SaganUpdater saganUpdater) {
		BDDMockito.then(saganUpdater).should(BDDMockito.never()).updateSagan(BDDMockito.any(File.class),
				BDDMockito.anyString(), BDDMockito.any(ProjectVersion.class), BDDMockito.any(ProjectVersion.class),
				BDDMockito.any(Projects.class));
	}

	public static class NonAssertingTestProjectGitHubHandler extends ProjectGitHubHandler {

		boolean closedMilestones = false;

		boolean issueCreatedInSpringGuides = false;

		boolean issueCreatedInStartSpringIo = false;

		public NonAssertingTestProjectGitHubHandler(ReleaserProperties properties) {
			super(properties, Collections.emptyList());
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
		public void createIssueInStartSpringIo(Projects projects, ProjectVersion version) {
			this.issueCreatedInStartSpringIo = true;
		}

		@Override
		public String milestoneUrl(ProjectVersion releaseVersion) {
			return "https://foo.bar.com/" + releaseVersion.toString();
		}

	}

}
