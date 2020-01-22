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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import releaser.internal.options.Parser;
import releaser.internal.spring.BuildReportHandler;
import releaser.internal.spring.ExecutionResultHandler;
import releaser.internal.spring.SpringReleaser;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
		classes = { ReleaserApplicationTests.Config.class, ReleaserApplication.class },
		properties = { "releaser.sagan.update-sagan=false" })
class ReleaserApplicationTests {

	@Test
	void contextLoads() {

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
		BuildReportHandler mockBuildReportHandler() {
			return Mockito.mock(BuildReportHandler.class);
		}

		@Bean
		Parser mockParser() {
			return Mockito.mock(Parser.class);
		}

	}

}
