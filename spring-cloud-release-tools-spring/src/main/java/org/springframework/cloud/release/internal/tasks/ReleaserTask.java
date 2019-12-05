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

package org.springframework.cloud.release.internal.tasks;

import java.util.function.Function;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.spring.ExecutionResult;
import org.springframework.cloud.release.internal.tech.MakeBuildUnstableException;
import org.springframework.core.Ordered;

public interface ReleaserTask extends Ordered, Function<Arguments, ExecutionResult> {

	String name();

	String shortName();

	String header();

	String description();

	default void setup(Options options, ReleaserProperties properties) {

	}

	default ExecutionResult apply(Arguments args) {
		try {
			return runTask(args);
		}
		catch (MakeBuildUnstableException ex) {
			return ExecutionResult.unstable(ex);
		}
		catch (Exception ex) {
			return ExecutionResult.failure(ex);
		}
	}

	ExecutionResult runTask(Arguments args);

}
