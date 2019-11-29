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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.tasks.CompositeReleaserTask;
import org.springframework.context.ApplicationContext;

/**
 * Marked by {@link Options#metaRelease} and {@link Options#dryRun}
 */
public class MetaReleaseDryRunCompositeTask implements CompositeReleaserTask {

	private static final Logger log = LoggerFactory.getLogger(MetaReleaseDryRunCompositeTask.class);

	public static final int ORDER = -70;

	private final ApplicationContext context;

	public MetaReleaseDryRunCompositeTask(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public String name() {
		return "metaReleaseDryRun";
	}

	@Override
	public String shortName() {
		return "xdr";
	}

	@Override
	public String header() {
		return "META RELEASE DRY RUN";
	}

	@Override
	public String description() {
		return "Perform a meta release dry run of projects";
	}

	@Override
	public void accept(Arguments args) {
		//TODO: Use Batch here already
		args.properties.getMetaRelease().setEnabled(true);
		// TODO: Let's run all the tasks
		/*
				static Task META_RELEASE_DRY_RUN = Tasks.task("metaReleaseDryRun", "xdr",
			"META RELEASE DRY RUN", "Perform a meta release dry run of projects",
			args -> new CompositeConsumer(DEFAULT_DRY_RUN_TASKS_PER_PROJECT,
					(args1 -> args.properties.getMetaRelease().setEnabled(true)))
							.accept(args));
		 */
	}

	@Override
	public int getOrder() {
		return MetaReleaseDryRunCompositeTask.ORDER;
	}
}
