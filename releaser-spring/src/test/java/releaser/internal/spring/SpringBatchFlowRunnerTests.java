/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package releaser.internal.spring;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import releaser.internal.ReleaserProperties;

class SpringBatchFlowRunnerTests {

	@Test
	void releaseGroup() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.getMetaRelease().setReleaseGroups(Arrays.asList("c,d", "e,f,g"));
		ProjectsToRun projectsToRun = projectsToRun();

		List<ReleaseGroup> groups = new ProjectsToReleaseGroups(properties)
				.toReleaseGroup(projectsToRun);

		BDDAssertions.then(groups).hasSize(5);
		BDDAssertions.then(projectNames(0, groups)).containsExactly("a");
		BDDAssertions.then(projectNames(1, groups)).containsExactly("b");
		BDDAssertions.then(projectNames(2, groups)).containsExactly("d", "c");
		BDDAssertions.then(projectNames(3, groups)).containsExactly("e", "f", "g");
		BDDAssertions.then(projectNames(4, groups)).containsExactly("h");
	}

	private List<String> projectNames(int index, List<ReleaseGroup> groups) {
		return groups.get(index).projectsToRuns.stream()
				.map(ProjectToRun.ProjectToRunSupplier::projectName)
				.collect(Collectors.toCollection(LinkedList::new));
	}

	private ProjectsToRun projectsToRun() {
		ProjectsToRun projectsToRun = new ProjectsToRun();
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("a", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("b", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("d", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("c", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("e", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("f", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("g", () -> null));
		projectsToRun.add(new ProjectToRun.ProjectToRunSupplier("h", () -> null));
		return projectsToRun;
	}

}