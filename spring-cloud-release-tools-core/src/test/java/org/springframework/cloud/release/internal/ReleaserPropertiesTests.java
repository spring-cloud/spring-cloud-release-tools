package org.springframework.cloud.release.internal;

import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class ReleaserPropertiesTests {
	@Test
	public void should_return_provided_working_dir_when_it_was_set() throws Exception {
		String workingDir = "foo";
		ReleaserProperties properties = new ReleaserProperties();

		properties.setWorkingDir(workingDir);

		then(properties.getWorkingDir()).isEqualTo(workingDir);
	}

	@Test
	public void should_return_current_working_dir_when_it_was_not_previously_set() throws Exception {
		ReleaserProperties properties = new ReleaserProperties();

		then(properties.getWorkingDir()).isNotEmpty();
	}

}