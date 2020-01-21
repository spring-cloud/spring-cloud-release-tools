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

package releaser.internal.tasks.composite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.spring.Arguments;
import releaser.internal.tasks.CompositeReleaserTask;
import releaser.internal.tech.ExecutionResult;

import org.springframework.context.ApplicationContext;

public class ReleaseVerboseCompositeTask implements CompositeReleaserTask {

	/**
	 * Order of this task. The higher value, the lower order.
	 */
	public static final int ORDER = -90;

	private static final Logger log = LoggerFactory
			.getLogger(ReleaseVerboseCompositeTask.class);

	private final ApplicationContext context;

	private ReleaseCompositeTask releaserCompositeTask;

	public ReleaseVerboseCompositeTask(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public String name() {
		return "releaseVerbose";
	}

	@Override
	public String shortName() {
		return "r";
	}

	@Override
	public String header() {
		return "FULL VERBOSE RELEASE";
	}

	@Override
	public String description() {
		return "Perform a full release of this project in interactive mode (you'll be asked about skipping steps)";
	}

	@Override
	public ExecutionResult runTask(Arguments args) {
		return releaserCompositeTask().apply(args);
	}

	@Override
	public void setup(Options options, ReleaserProperties properties) {
		options.fullRelease = true;
		options.interactive = true;
	}

	@Override
	public int getOrder() {
		return ReleaseVerboseCompositeTask.ORDER;
	}

	private ReleaseCompositeTask releaserCompositeTask() {
		if (this.releaserCompositeTask == null) {
			this.releaserCompositeTask = this.context.getBean(ReleaseCompositeTask.class);
		}
		return this.releaserCompositeTask;
	}

}
