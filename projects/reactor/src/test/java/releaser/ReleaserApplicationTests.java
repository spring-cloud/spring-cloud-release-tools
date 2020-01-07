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

package releaser;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import releaser.internal.options.Parser;
import releaser.internal.spring.ExecutionResultHandler;
import releaser.internal.spring.SpringReleaser;
import releaser.internal.tasks.DryRunReleaseReleaserTask;
import releaser.internal.tasks.ReleaserTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
		classes = { ReleaserApplicationTests.Config.class, ReleaserApplication.class },
		properties = { "releaser.sagan.update-sagan=false" })
@ActiveProfiles("test")
class ReleaserApplicationTests {

	@Autowired
	ApplicationContext context;

	@Test
	void contextLoads() {

	}

	@Test
	void should_load_reactor_version_of_publish_docs() {
		Map<String, DryRunReleaseReleaserTask> beans = context
				.getBeansOfType(DryRunReleaseReleaserTask.class);
		List<ReleaserTask> inOrder = new LinkedList<>(beans.values());
		inOrder.sort(AnnotationAwareOrderComparator.INSTANCE);
		System.out.println(inOrder);
	}

	@Configuration
	static class Config {

		@Bean
		SpringReleaser mockReleaser() {
			return Mockito.mock(SpringReleaser.class);
		}

		@Bean
		ExecutionResultHandler mockExecutionResultHandler() {
			return Mockito.mock(ExecutionResultHandler.class);
		}

		@Bean
		Parser mockParser() {
			return Mockito.mock(Parser.class);
		}

	}

}
