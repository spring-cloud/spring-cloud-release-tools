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

package org.springframework.cloud.release.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.release.internal.docs.DocumentationUpdater;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.gradle.GradleUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.cloud.release.internal.post.PostReleaseActions;
import org.springframework.cloud.release.internal.project.ProjectBuilder;
import org.springframework.cloud.release.internal.sagan.SaganUpdater;
import org.springframework.cloud.release.internal.template.TemplateGenerator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ReleaserTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Mock
	ProjectPomUpdater projectPomUpdater;

	@Mock
	ProjectBuilder projectBuilder;

	@Mock
	ProjectGitHandler projectGitHandler;

	@Mock
	TemplateGenerator templateGenerator;

	@Mock
	GradleUpdater gradleUpdater;

	@Mock
	SaganUpdater saganUpdater;

	@Mock
	DocumentationUpdater documentationUpdater;

	@Mock
	PostReleaseActions postReleaseActions;

	File pom;

	@Before
	public void setup() throws URISyntaxException {
		URI pomUri = ReleaserTests.class.getResource("/projects/project/pom.xml").toURI();
		this.pom = new File(pomUri);
	}

	Releaser releaser(Supplier<ProjectVersion> originalVersionSupplier) {
		return new Releaser(new ReleaserProperties(), this.projectPomUpdater,
				this.projectBuilder, this.projectGitHandler, this.templateGenerator,
				this.gradleUpdater, this.saganUpdater, this.documentationUpdater,
				this.postReleaseActions) {
			@Override
			ProjectVersion originalVersion(File project) {
				return originalVersionSupplier.get();
			}
		};
	}

	Releaser releaser() {
		return new Releaser(new ReleaserProperties(), this.projectPomUpdater,
				this.projectBuilder, this.projectGitHandler, this.templateGenerator,
				this.gradleUpdater, this.saganUpdater, this.documentationUpdater,
				this.postReleaseActions);
	}

	@Test
	public void should_not_bump_versions_for_original_release_project() {
		releaser(() -> new ProjectVersion("original", "1.0.0.RELEASE"))
				.rollbackReleaseVersion(this.pom,
						new Projects(new ProjectVersion("changed", "1.0.0.RELEASE")),
						new ProjectVersion("changed", "1.0.0.RELEASE"));

		BDDAssertions.then(this.outputCapture.toString()).contains(
				"Successfully reverted the commit and came back to snapshot versions");
		then(this.projectGitHandler).should(never())
				.commitAfterBumpingVersions(any(File.class), any(ProjectVersion.class));
	}

	@Test
	public void should_not_bump_versions_for_original_snapshot_project_and_current_snapshot() {
		releaser(() -> new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"))
				.rollbackReleaseVersion(this.pom,
						new Projects(
								new ProjectVersion("changed", "1.0.0.BUILD-SNAPSHOT")),
						new ProjectVersion("changed", "1.0.0.BUILD-SNAPSHOT"));

		BDDAssertions.then(this.outputCapture.toString())
				.contains("Won't rollback a snapshot version");
		then(this.projectGitHandler).should(never())
				.commitAfterBumpingVersions(any(File.class), any(ProjectVersion.class));
	}

	@Test
	public void should_bump_versions_for_original_snapshot_project() {
		ProjectVersion scReleaseVersion = new ProjectVersion("changed", "1.0.0.RELEASE");
		releaser(() -> new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"))
				.rollbackReleaseVersion(this.pom, new Projects(
						new ProjectVersion("changed", "1.0.0.RELEASE"),
						new ProjectVersion("spring-cloud-build", "2.0.0.RELEASE"),
						new ProjectVersion("spring-boot-starter", "3.0.0.RELEASE")),
						scReleaseVersion);

		BDDAssertions.then(this.outputCapture.toString())
				.contains("Project was successfully updated")
				.contains("Successfully reverted the commit and bumped snapshot versions")
				.contains("spring-boot-starter=>3.0.0.RELEASE")
				.contains("spring-cloud-build=>2.0.1.BUILD-SNAPSHOT")
				.contains("changed=>1.0.1.BUILD-SNAPSHOT");
	}

	@Test
	public void should_not_generate_email_for_snapshot_version() {
		releaser().createEmail(new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"),
				projects());

		then(this.templateGenerator).should(never()).email(any(Projects.class));
	}

	@Test
	public void should_generate_email_for_release_version() {
		BDDMockito.given(this.templateGenerator.email(projects()))
				.willReturn(new File("."));
		releaser().createEmail(new ProjectVersion("original", "1.0.0.RELEASE"),
				projects());

		then(this.templateGenerator).should().email(any(Projects.class));
	}

	@Test
	public void should_not_close_milestone_for_snapshots() {
		releaser().closeMilestone(new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"));

		then(this.projectGitHandler).should(never())
				.closeMilestone(any(ProjectVersion.class));
	}

	@Test
	public void should_not_rollback_for_snapshots() {
		releaser(() -> new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"))
				.rollbackReleaseVersion(null,
						new Projects(
								new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT")),
						new ProjectVersion("original", "1.0.0.BUILD-SNAPSHOT"));

		then(this.projectGitHandler).should(never())
				.revertChangesIfApplicable(any(File.class), any(ProjectVersion.class));
	}

	Projects projects() {
		return new Projects(new ProjectVersion("foo", "1.0.0.RELEASE"));
	}

}
