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

import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.tasks.CompositeReleaserTask;
import org.springframework.context.ApplicationContext;

public class ReleaseVerboseCompositeTask implements CompositeReleaserTask {

	private static final Logger log = LoggerFactory.getLogger(ReleaseVerboseCompositeTask.class);

	public static final int ORDER = -90;

	private final ApplicationContext context;

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
	public void accept(Arguments args) {
		//TODO: Use Batch here already
		// TODO: How to mark the interactive mode aka the listener
	}

	@Override
	public int getOrder() {
		return ReleaseVerboseCompositeTask.ORDER;
	}
}
