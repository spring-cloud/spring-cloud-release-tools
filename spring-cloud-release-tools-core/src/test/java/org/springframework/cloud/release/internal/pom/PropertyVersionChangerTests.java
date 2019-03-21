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

package org.springframework.cloud.release.internal.pom;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.maven.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertyVersionChangerTests {

	@Mock
	PropertyStorer propertyStorer;

	@Test
	public void should_set_version_when_project_matches_property_name() throws Exception {
		PropertyVersionChanger changer = new PropertyVersionChanger(model(), versions(),
				null, null, this.propertyStorer);

		changer.apply(null);

		then(this.propertyStorer).should().setPropertyVersionIfApplicable(
				project("spring-cloud-sleuth", "1.2.0.BUILD-SNAPSHOT"));
	}

	@Test
	public void should_not_set_version_when_project_doesnt_match_property_name()
			throws Exception {
		PropertyVersionChanger changer = new PropertyVersionChanger(nonMatchingModel(),
				versions(), null, null, this.propertyStorer);

		changer.apply(null);

		then(this.propertyStorer).should(never())
				.setPropertyVersionIfApplicable(any(Project.class));
	}

	@Test
	public void should_not_set_version_when_project_matches_property_name_and_versions_are_the_same()
			throws Exception {
		PropertyVersionChanger changer = new PropertyVersionChanger(modelWithSameValues(),
				versions(), null, null, this.propertyStorer);

		changer.apply(null);

		then(this.propertyStorer).should(never())
				.setPropertyVersionIfApplicable(any(Project.class));
	}

	Versions versions() {
		return new Versions("", "", allProjects());
	}

	@SuppressWarnings("unchecked")
	private Set<Project> allProjects() {
		return new HashSet<>(Arrays.asList(
				new Project[] { project("spring-cloud-aws", "1.2.0.BUILD-SNAPSHOT"),
						project("spring-cloud-sleuth", "1.2.0.BUILD-SNAPSHOT") }));
	}

	Project project(String name, String value) {
		return new Project(name, value);
	}

	ModelWrapper model() {
		Model model = new Model();
		model.setProperties(properties());
		return new ModelWrapper(model);
	}

	Properties properties() {
		Properties properties = new Properties();
		properties.setProperty("spring-cloud-sleuth.version", "1.0.0.RELEASE");
		return properties;
	}

	ModelWrapper modelWithSameValues() {
		Model model = new Model();
		model.setProperties(propertiesWithSameValues());
		return new ModelWrapper(model);
	}

	Properties propertiesWithSameValues() {
		Properties properties = new Properties();
		properties.setProperty("spring-cloud-sleuth.version", "1.2.0.BUILD-SNAPSHOT");
		return properties;
	}

	ModelWrapper nonMatchingModel() {
		Model model = new Model();
		model.setProperties(nonMatchingProperties());
		return new ModelWrapper(model);
	}

	Properties nonMatchingProperties() {
		Properties properties = new Properties();
		properties.setProperty("spring-cloud-non-matching.version", "1.0.0.RELEASE");
		return properties;
	}

}
