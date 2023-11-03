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

import releaser.internal.options.Parser;
import releaser.internal.spring.ExecutionResultHandler;
import releaser.internal.spring.SpringReleaser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReleaserApplication extends ReleaserCommandLineRunner {

	public ReleaserApplication(SpringReleaser releaser, ExecutionResultHandler executionResultHandler, Parser parser) {
		super(releaser, executionResultHandler, parser);
	}

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ReleaserApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}

}
