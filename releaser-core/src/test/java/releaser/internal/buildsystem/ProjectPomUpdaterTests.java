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

package releaser.internal.buildsystem;

import java.io.File;
import java.util.Collections;

import org.junit.Test;
import org.mockito.BDDMockito;
import releaser.SpringCloudReleaserProperties;
import releaser.internal.ReleaserProperties;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectPomUpdaterTests {

	@Test
	public void should_skip_any_steps_if_there_is_no_pom_xml() {
		ReleaserProperties properties = SpringCloudReleaserProperties.get();
		ProjectGitHandler handler = BDDMockito.mock(ProjectGitHandler.class);
		ProjectPomUpdater updater = new ProjectPomUpdater(properties,
				Collections.emptyList());

		updater.updateProjectFromReleaseTrain(new File("target"), new Projects(),
				new ProjectVersion("foo", "1.0.0.RELEASE"), false);

		BDDMockito.then(handler).shouldHaveZeroInteractions();
	}

}
