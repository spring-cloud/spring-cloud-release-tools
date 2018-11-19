package org.springframework.cloud.release.internal.docs;

import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class RowTests {

	@Test
	public void should_convert_projects_to_rows_for_last_ga() {
		Projects projects = new Projects(
				new ProjectVersion("foo", "1.0.0.BUILD-SNAPSHOT"),
				new ProjectVersion("sc-release", "2.0.1.RELEASE")
		);

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
				new ProjectVersion("sc-release", "2.0.1.RELEASE")
		);

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