package org.springframework.cloud.release.internal;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.BDDAssertions;
import org.codehaus.mojo.versions.change.VersionChange;

/**
 * @author Marcin Grzejszczak
 */
class VersionChangeAssertions extends BDDAssertions {

	public static VersionChangeAssert then(ListOfChanges actual) {
		return assertThat(actual);
	}

	public static VersionChangeAssert assertThat(ListOfChanges actual) {
		return new VersionChangeAssert(actual);
	}

}

class ListOfChanges {

	final List<VersionChange> changes;

	ListOfChanges(ModelWrapper model) {
		this.changes = new ArrayList<>(model.sourceChanges);
	}
}

class VersionChangeAssert extends
		AbstractAssert<VersionChangeAssert, ListOfChanges> {

	public VersionChangeAssert(ListOfChanges actual) {
		super(actual, VersionChangeAssert.class);
	}

	VersionChangeAssert newParentVersionIsEqualTo(String groupId, String artifactId, String newVersion) {
		boolean matches = false;
		for (VersionChange change : actual.changes) {
			if (newVersion.equals(change.getNewVersion())
					&& groupId.equals(change.getGroupId())
					&& artifactId.equals(change.getArtifactId())) {
				matches = true;
				break;
			}
		}
		if (matches) {
			return this;
		}
		failWithMessage("There is no change with that parent coordinates");
		return this;
	}
}

