/*
 * Copyright 2013-2022 the original author or authors.
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

package releaser.internal.buildsystem;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.versions.rewriting.ModifiedPomXMLEventReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import releaser.internal.project.Project;

import static org.mockito.BDDMockito.then;

/**
 * @author Marcin Grzejszczak
 */
@ExtendWith(MockitoExtension.class)
public class PropertyStorerTests {

	@Mock
	Log log;

	@Mock
	ModifiedPomXMLEventReader pom;

	@InjectMocks
	PropertyStorer propertyStorer;

	@Test
	public void should_not_set_a_version_when_its_empty() {
		this.propertyStorer.setPropertyVersionIfApplicable(new Project("foo", ""));

		then(this.log).should().warn(containsWarnMsgAboutEmptyVersion());
	}

	private String containsWarnMsgAboutEmptyVersion() {
		return BDDMockito.argThat(argument -> argument.contains("is empty. Will not set it"));
	}

}
