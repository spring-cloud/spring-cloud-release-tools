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

package releaser.internal.tasks;

import java.util.function.Function;

import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.spring.Arguments;
import releaser.internal.tech.BuildUnstableException;
import releaser.internal.tech.ExecutionResult;

import org.springframework.core.Ordered;

/**
 * Describes a single release task.
 */
public interface ReleaserTask extends Ordered, Function<Arguments, ExecutionResult> {

	/**
	 * @return name of the release task (e.g. 'Build')
	 */
	String name();

	/**
	 * @return short name of the release task (e.g. 'b')
	 */
	String shortName();

	/**
	 * @return header presented in the logs when running the task
	 */
	String header();

	/**
	 * @return meaningful description of the release task.
	 */
	String description();

	/**
	 * If necessary mutates options and properties for the release task. This can be
	 * useful for composite tasks that impose certain configuration.
	 * @param options - options to mutate
	 * @param properties - properties to mutate
	 */
	default void setup(Options options, ReleaserProperties properties) {

	}

	/**
	 * Executes the task but catches exceptions and converts them into result. Knows how
	 * to differentiate between a failure and instability.
	 * @param args - arguments to run the task
	 * @return execution result
	 */
	default ExecutionResult apply(Arguments args) {
		try {
			return runTask(args);
		}
		catch (BuildUnstableException ex) {
			return ExecutionResult.unstable(ex);
		}
		catch (Exception ex) {
			return ExecutionResult.failure(ex);
		}
	}

	/**
	 * Main task execution logic.
	 * @param args - arguments for the job
	 * @return execution result of the job
	 * @throws BuildUnstableException - when the task failed but the release flow
	 * shouldn't be stopped (which means that the build is unstable)
	 * @throws RuntimeException - when the task failed for any other reason
	 */
	ExecutionResult runTask(Arguments args)
			throws BuildUnstableException, RuntimeException;

}
