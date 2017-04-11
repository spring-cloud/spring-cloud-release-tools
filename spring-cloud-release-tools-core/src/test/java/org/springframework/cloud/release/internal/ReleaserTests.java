package org.springframework.cloud.release.internal;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.git.ProjectGitUpdater;
import org.springframework.cloud.release.internal.pom.ProjectPomUpdater;
import org.springframework.cloud.release.internal.project.ProjectBuilder;

import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class ReleaserTests {

	@Mock ProjectPomUpdater projectPomUpdater;
	@Mock ProjectBuilder projectBuilder;
	@Mock ProjectGitUpdater projectGitUpdater;
	@InjectMocks Releaser releaser;
	File pom;

	@Before
	public void setup() throws URISyntaxException {
		URI pomUri = ReleaserTests.class.getResource("/projects/project/pom.xml").toURI();
		this.pom = new File(pomUri);
	}

	@Test
	public void rollbackReleaseVersion() throws Exception {
		this.releaser.rollbackReleaseVersion(this.pom, null, null);

		verifyZeroInteractions(this.projectPomUpdater);
		verifyZeroInteractions(this.projectBuilder);
		verifyZeroInteractions(this.projectGitUpdater);
	}

}