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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaserPropertiesTests {

	@Test
	public void should_return_provided_working_dir_when_it_was_set() {
		String workingDir = "foo";
		ReleaserProperties properties = new ReleaserProperties();

		properties.setWorkingDir(workingDir);

		then(properties.getWorkingDir()).isEqualTo(workingDir);
	}

	@Test
	public void should_return_current_working_dir_when_it_was_not_previously_set() {
		ReleaserProperties properties = new ReleaserProperties();

		then(properties.getWorkingDir()).isNotEmpty();
	}

	@Test
	public void should_return_a_copy_of_properties() {
		ReleaserProperties properties = new ReleaserProperties();
		properties.setWorkingDir("foo");
		properties.setFixedVersions(map());
		properties.getMaven().setBuildCommand("foo2");
		properties.getGradle().setIgnoredGradleRegex(Arrays.asList("foo3", "foo4"));
		properties.getMetaRelease().setProjectsToSkip(Arrays.asList("foo5", "foo6"));
		properties.getGit().setPassword("foo7");
		properties.getPom().setIgnoredPomRegex(Arrays.asList("foo8", "foo9"));
		properties.getSagan().setBaseUrl("foo10");

		ReleaserProperties copy = properties.copy();
		copy.setWorkingDir("bar");
		copy.setFixedVersions(map2());
		copy.getMaven().setBuildCommand("bar2");
		copy.getGradle().setIgnoredGradleRegex(Arrays.asList("bar3", "bar4"));
		copy.getMetaRelease().setProjectsToSkip(Arrays.asList("bar5", "bar6"));
		copy.getGit().setPassword("bar7");
		copy.getPom().setIgnoredPomRegex(Arrays.asList("bar8", "bar9"));
		copy.getSagan().setBaseUrl("bar10");

		BDDAssertions.then(properties.getWorkingDir()).isEqualTo("foo");
		BDDAssertions.then(properties.getFixedVersions()).isEqualTo(map());
		BDDAssertions.then(properties.getMaven().getBuildCommand()).isEqualTo("foo2");
		BDDAssertions.then(properties.getGradle().getIgnoredGradleRegex())
				.isEqualTo(Arrays.asList("foo3", "foo4"));
		BDDAssertions.then(properties.getMetaRelease().getProjectsToSkip())
				.isEqualTo(Arrays.asList("foo5", "foo6"));
		BDDAssertions.then(properties.getGit().getPassword()).isEqualTo("foo7");
		BDDAssertions.then(properties.getPom().getIgnoredPomRegex())
				.isEqualTo(Arrays.asList("foo8", "foo9"));
		BDDAssertions.then(properties.getSagan().getBaseUrl()).isEqualTo("foo10");
	}

	private Map<String, String> map() {
		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		return map;
	}

	private Map<String, String> map2() {
		Map<String, String> map = new HashMap<>();
		map.put("bar", "foo");
		return map;
	}

}
