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

package org.springframework.cloud.release.internal.tasks.composite;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.spring.ExecutionResult;
import org.springframework.cloud.release.internal.spring.FlowRunner;
import org.springframework.cloud.release.internal.spring.TasksToRun;
import org.springframework.cloud.release.internal.tasks.CompositeReleaserTask;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.cloud.release.internal.tasks.TrainPostReleaseReleaserTask;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Marked by {@link Options#metaRelease} and
 * {@link ReleaserProperties#isPostReleaseTasksOnly()}.
 */
public class TrainPostReleaseCompositeTask implements CompositeReleaserTask {

	private static final Logger log = LoggerFactory
			.getLogger(TrainPostReleaseCompositeTask.class);

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = -50;

	private final ApplicationContext context;

	private FlowRunner flowRunner;

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
	public ExecutionResult runTask(Arguments args) {
		Map<String, TrainPostReleaseReleaserTask> trainPostReleaseReleaserTasks = this.context
				.getBeansOfType(TrainPostReleaseReleaserTask.class);
		List<ReleaserTask> values = new LinkedList<>(
				trainPostReleaseReleaserTasks.values());
		values.sort(AnnotationAwareOrderComparator.INSTANCE);
		log.info("Found the following post release tasks {}", values);
		return flowRunner().runPostReleaseTasks(args.options, args.properties,
				this.name(), new TasksToRun(values));
	}

	@Override
	public void setup(Options options, ReleaserProperties properties) {
		options.metaRelease = true;
		properties.setPostReleaseTasksOnly(true);
	}

	@Override
	public int getOrder() {
		return TrainPostReleaseCompositeTask.ORDER;
	}

	private FlowRunner flowRunner() {
		if (this.flowRunner == null) {
			this.flowRunner = this.context.getBean(FlowRunner.class);
		}
		return this.flowRunner;
	}

}
