/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.tasks;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.core.Ordered;

public interface ReleaserTask extends Ordered, Consumer<Arguments> {

	String name();

	String shortName();

	String header();

	String description();

	List<TaskType> taskTypes();

	default void setup(Options options, ReleaserProperties properties) {

	}

	enum TaskType {
		/**
		 * Release task for a project that should break the build if it fails.
		 */
		RELEASE,

		/**
		 * A post release task for a particular project. Will not fail the build if fails.
		 */
		PROJECT_POST_RELEASE,

		/**
		 * A post release task for a release train. Will not fail the build if fails.
		 */
		TRAIN_POST_RELEASE,

		/**
		 * A task that consists of other tasks.
		 */
		COMPOSITE,

		/**
		 * A task that should be executed when dry run is called.
		 */
		DRY_RUN
	}

}
