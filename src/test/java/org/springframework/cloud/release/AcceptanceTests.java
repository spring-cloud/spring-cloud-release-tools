package org.springframework.cloud.release;

import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 */
public class AcceptanceTests {

	/**

	 - should clone spring cloud release (x)
	 - should check out a branch / tag (x)
	 - should parse spring-cloud-starter-parent/pom.xml and resolve:
	 	- Boot version (x)
	 - should parse spring-cloud-dependencies/pom.xml and resolve:
	 	- Project versions from properties (x)
	 	- Spring Cloud Build version from parent (x)
	 - should update the existing poms with the taken versions
	 */
	@Test
	public void should_update_all_versions_for_a_release_train() {
	}
}
