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
import org.springframework.cloud.release.internal.spring.ProjectToRun;
import org.springframework.cloud.release.internal.spring.ProjectsToRun;
import org.springframework.cloud.release.internal.spring.TasksToRun;
import org.springframework.cloud.release.internal.tasks.CompositeReleaserTask;
import org.springframework.cloud.release.internal.tasks.DryRunReleaseReleaserTask;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Marked by {@link Options#dryRun}.
 */
public class DryRunCompositeTask implements CompositeReleaserTask {

	private static final Logger log = LoggerFactory.getLogger(DryRunCompositeTask.class);

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = -60;

	private final ApplicationContext context;

	private FlowRunner flowRunner;

	public DryRunCompositeTask(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public String name() {
		return "dryRun";
	}

	@Override
	public String shortName() {
		return "dr";
	}

	@Override
	public String header() {
		return "DRY RUN";
	}

	@Override
	public String description() {
		return "Perform a dry run release of a single project - bumps versions and installs them locally";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		Map<String, DryRunReleaseReleaserTask> dryRunTasks = this.context
				.getBeansOfType(DryRunReleaseReleaserTask.class);
		List<DryRunReleaseReleaserTask> values = new LinkedList<>(dryRunTasks.values());
		values.sort(AnnotationAwareOrderComparator.INSTANCE);
		log.info("Found the following dry run tasks {}", values);
		return flowRunner().runReleaseTasks(args.options, args.properties,
				new ProjectsToRun(new ProjectToRun.ProjectToRunSupplier(
						args.originalVersion.projectName, () -> args.projectToRun)),
				new TasksToRun(values));
	}

	@Override
	public void setup(Options options, ReleaserProperties properties) {
		options.dryRun = true;
	}

	@Override
	public int getOrder() {
		return DryRunCompositeTask.ORDER;
	}

	private FlowRunner flowRunner() {
		if (this.flowRunner == null) {
			this.flowRunner = this.context.getBean(FlowRunner.class);
		}
		return this.flowRunner;
	}

}
