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

package releaser.internal.docs;

import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class RowTests {

	@Test
	public void should_convert_projects_to_rows_for_last_ga() {
		Projects projects = new Projects(
				new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("sc-release", "2.0.1.RELEASE"));

		List<Row> rows = Row.fromProjects(projects, true);

		Row release = rows.get(0);
		BDDAssertions.then(release.componentName).isEqualTo("sc-release");
		BDDAssertions.then(release.currentGaVersion).isEmpty();
		BDDAssertions.then(release.currentSnapshotVersion).isEmpty();
		BDDAssertions.then(release.lastGaVersion).isEqualTo("2.0.1.RELEASE");
		Row foo = rows.get(1);
		BDDAssertions.then(foo.componentName).isEqualTo("foo");
		BDDAssertions.then(foo.currentSnapshotVersion).isEqualTo("1.0.0.BUILD-SNAPSHOT");
		BDDAssertions.then(foo.currentGaVersion).isEmpty();
		BDDAssertions.then(foo.lastGaVersion).isEmpty();
	}

	@Test
	public void should_convert_projects_to_rows_for_current_ga() {
		Projects projects = new Projects(
				new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("sc-release", "2.0.1.RELEASE"));

		List<Row> rows = Row.fromProjects(projects, false);

		Row release = rows.get(0);
		BDDAssertions.then(release.componentName).isEqualTo("sc-release");
		BDDAssertions.then(release.lastGaVersion).isEmpty();
		BDDAssertions.then(release.currentSnapshotVersion).isEmpty();
		BDDAssertions.then(release.currentGaVersion).isEqualTo("2.0.1.RELEASE");
		Row foo = rows.get(1);
		BDDAssertions.then(foo.componentName).isEqualTo("foo");
		BDDAssertions.then(foo.lastGaVersion).isEmpty();
		BDDAssertions.then(foo.currentGaVersion).isEmpty();
		BDDAssertions.then(foo.currentSnapshotVersion).isEqualTo("1.0.0.BUILD-SNAPSHOT");
	}

}
