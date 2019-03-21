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

package org.springframework.cloud.release.internal.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 * @since
 */
public class OptionsParserTests {

	@Test
	public void should_filter_provided_task_names() {
		OptionsParser optionsParser = new OptionsParser();

		List<String> taskNames = optionsParser
				.filterProvidedTaskNames(providedTaskNames(), allTaskNames(), true);

		BDDAssertions.then(taskNames).isEqualTo(providedTaskNames());
	}

	private List<String> providedTaskNames() {
		return new ArrayList<>(Arrays.asList("spring-cloud-config",
				"spring-cloud-netflix", "spring-cloud-cloudfoundry",
				"spring-cloud-openfeign", "spring-cloud-gateway", "spring-cloud-security",
				"spring-cloud-sleuth", "spring-cloud-contract", "spring-cloud-vault",
				"spring-cloud-release"));
	}

	private List<String> allTaskNames() {
		return new ArrayList<>(Arrays.asList("spring-cloud-config",
				"spring-cloud-netflix", "spring-cloud-cloudfoundry",
				"spring-cloud-openfeign", "spring-cloud-gateway", "spring-cloud-security",
				"spring-cloud-sleuth", "spring-cloud-contract", "spring-cloud-vault",
				"spring-cloud-release", "spring-cloud-1", "spring-cloud-2",
				"spring-cloud-3"));
	}

}
