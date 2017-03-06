package org.springframework.cloud.release;

import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 */
public class AcceptanceTests {

	/**

	 - should clone spring cloud release
	 - should check out a branch / tag
	 - should parse spring-cloud-dependencies/pom.xml and resolve:
	 	- Boot version
	 	- Project versions from properties
	 	- Spring Cloud Build version from parent
	 - should update the existing poms with the taken versions
	 */
	@Test
	public void should_update_all_versions_for_a_release_train() {
	}
}
