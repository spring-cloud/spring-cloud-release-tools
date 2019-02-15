/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.pom;

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

class VersionChangeAssert extends AbstractAssert<VersionChangeAssert, ListOfChanges> {

	VersionChangeAssert(ListOfChanges actual) {
		super(actual, VersionChangeAssert.class);
	}

	VersionChangeAssert newParentVersionIsEqualTo(String groupId, String artifactId,
			String newVersion) {
		boolean matches = false;
		for (VersionChange change : this.actual.changes) {
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
