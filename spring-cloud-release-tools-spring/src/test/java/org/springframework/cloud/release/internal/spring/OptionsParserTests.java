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

	@Test public void should_filter_provided_task_names() {
		OptionsParser optionsParser = new OptionsParser();

		List<String> taskNames = optionsParser.filterProvidedTaskNames(
				providedTaskNames(), allTaskNames(), true
		);

		BDDAssertions.then(taskNames).isEqualTo(providedTaskNames());
	}

	private List<String> providedTaskNames() {
		return new ArrayList<>(
				Arrays.asList("spring-cloud-config", "spring-cloud-netflix",
						"spring-cloud-cloudfoundry", "spring-cloud-openfeign",
						"spring-cloud-gateway", "spring-cloud-security",
						"spring-cloud-sleuth", "spring-cloud-contract",
						"spring-cloud-vault", "spring-cloud-release"));
	}

	private List<String> allTaskNames() {
		return new ArrayList<>(
				Arrays.asList("spring-cloud-config", "spring-cloud-netflix",
						"spring-cloud-cloudfoundry", "spring-cloud-openfeign",
						"spring-cloud-gateway", "spring-cloud-security",
						"spring-cloud-sleuth", "spring-cloud-contract",
						"spring-cloud-vault", "spring-cloud-release",
						"spring-cloud-1", "spring-cloud-2", "spring-cloud-3"));
	}
}