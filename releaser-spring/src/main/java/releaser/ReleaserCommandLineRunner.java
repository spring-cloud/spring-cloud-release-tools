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

import releaser.internal.options.Options;
import releaser.internal.options.Parser;
import releaser.internal.spring.ExecutionResult;
import releaser.internal.spring.ExecutionResultHandler;
import releaser.internal.spring.SpringReleaser;

import org.springframework.boot.CommandLineRunner;

public class ReleaserCommandLineRunner implements CommandLineRunner {

	private final SpringReleaser releaser;

	private final ExecutionResultHandler executionResultHandler;

	private final Parser parser;

	public ReleaserCommandLineRunner(SpringReleaser releaser,
			ExecutionResultHandler executionResultHandler, Parser parser) {
		this.releaser = releaser;
		this.executionResultHandler = executionResultHandler;
		this.parser = parser;
	}

	@Override
	public void run(String... strings) {
		// TODO:
		// * Check out Spring Shell - maybe move interactive stuff out of it
		// * Spring Shell would spit out the options at the end
		// * Check why I can't run a composite job as a batch job (transaction not
		// committed exception)
		Options options = this.parser.parse(strings);
		ExecutionResult executionResult = this.releaser.release(options);
		this.executionResultHandler.accept(executionResult);
	}

}
