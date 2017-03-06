package org.springframework.cloud.release;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents versions taken out from Spring Cloud Release pom
 *
 * @author Marcin Grzejszczak
 */
class Versions {

	String boot;
	String build;
	Map<String, String> projects = new HashMap<>();
}