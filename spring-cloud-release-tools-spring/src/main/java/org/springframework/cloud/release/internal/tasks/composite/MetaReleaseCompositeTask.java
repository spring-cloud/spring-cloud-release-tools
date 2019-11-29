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
 * Marked by {@link Options#metaRelease}
 */
public class MetaReleaseCompositeTask implements CompositeReleaserTask {

	private static final Logger log = LoggerFactory.getLogger(MetaReleaseCompositeTask.class);

	public static final int ORDER = -80;

	private final ApplicationContext context;

	public MetaReleaseCompositeTask(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public String name() {
		return "metaRelease";
	}

	@Override
	public String shortName() {
		return "x";
	}

	@Override
	public String header() {
		return "META RELEASE";
	}

	@Override
	public String description() {
		return "Perform a meta release of projects";
	}

	@Override
	public void accept(Arguments args) {
		//TODO: Use Batch here already
		args.properties.getMetaRelease().setEnabled(true);
		// TODO: Let's run all the tasks
		/*
			static Task META_RELEASE = Tasks.task("metaRelease", "x", "META RELEASE",
			"Perform a meta release of projects",
			args -> new CompositeConsumer(DEFAULT_TASKS_PER_PROJECT,
					(args1 -> args.properties.getMetaRelease().setEnabled(true)))
							.accept(args));
		 */
	}

	@Override
	public int getOrder() {
		return MetaReleaseCompositeTask.ORDER;
	}
}
