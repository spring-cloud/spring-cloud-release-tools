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

package releaser;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import releaser.internal.tasks.DryRunReleaseReleaserTask;
import releaser.internal.tasks.ReleaserTask;
import releaser.internal.tasks.release.PublishDocsReleaseTask;
import releaser.reactor.GenerateReleaseNotesTask;
import releaser.reactor.RestartSiteProjectPostReleaseTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { ReleaserApplication.class })
@ActiveProfiles("test")
class ReleaserApplicationTests {

	@Autowired
	ApplicationContext context;

	@Test
	void contextLoads() {

	}

	@Test
	void should_load_generate_release_notes_in_dry_run() {
		Map<String, DryRunReleaseReleaserTask> beans = context.getBeansOfType(DryRunReleaseReleaserTask.class);
		List<ReleaserTask> inOrder = new LinkedList<>(beans.values());
		inOrder.sort(AnnotationAwareOrderComparator.INSTANCE);

		assertThat(inOrder).anySatisfy(task -> assertThat(task).isInstanceOf(GenerateReleaseNotesTask.class));
	}

	@Test
	void should_load_restart_site() {
		Map<String, ReleaserTask> beans = context.getBeansOfType(ReleaserTask.class);
		List<ReleaserTask> inOrder = new LinkedList<>(beans.values());
		inOrder.sort(AnnotationAwareOrderComparator.INSTANCE);

		assertThat(inOrder).anySatisfy(task -> assertThat(task).isInstanceOf(RestartSiteProjectPostReleaseTask.class));
		assertThat(inOrder).noneSatisfy(task -> assertThat(task).isInstanceOf(PublishDocsReleaseTask.class)
				.isNotInstanceOf(RestartSiteProjectPostReleaseTask.class));

	}

}
