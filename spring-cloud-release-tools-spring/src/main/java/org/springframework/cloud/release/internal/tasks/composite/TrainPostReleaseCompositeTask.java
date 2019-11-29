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

package org.springframework.cloud.release.internal.tasks.composite;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.tasks.CompositeReleaserTask;
import org.springframework.cloud.release.internal.tasks.TrainPostReleaseReleaserTask;
import org.springframework.context.ApplicationContext;

/**
 * Marked by {@link Options#metaRelease} and {@link ReleaserProperties#isPostReleaseTasksOnly()}
 */
public class TrainPostReleaseCompositeTask implements CompositeReleaserTask {

	private static final Logger log = LoggerFactory.getLogger(TrainPostReleaseCompositeTask.class);

	public static final int ORDER = -50;

	private final ApplicationContext context;

	public TrainPostReleaseCompositeTask(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public String name() {
		return "postRelease";
	}

	@Override
	public String shortName() {
		return "pr";
	}

	@Override
	public String header() {
		return "POST RELEASE TASKS";
	}

	@Override
	public String description() {
		return "Perform post release tasks for this release without interruptions";
	}

	@Override
	public void accept(Arguments args) {
		//TODO: Use Batch here already
		Map<String, TrainPostReleaseReleaserTask> trainPostReleaseReleaserTasks = this.context.getBeansOfType(TrainPostReleaseReleaserTask.class);
		log.info("Found the following train post release tasks {}", trainPostReleaseReleaserTasks);
		trainPostReleaseReleaserTasks.values().forEach(t -> t.accept(args));
	}

	@Override
	public int getOrder() {
		return TrainPostReleaseCompositeTask.ORDER;
	}
}
