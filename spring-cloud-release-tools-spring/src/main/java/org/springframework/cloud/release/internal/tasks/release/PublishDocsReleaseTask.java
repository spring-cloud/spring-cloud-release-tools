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

package org.springframework.cloud.release.internal.tasks.release;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.tasks.ReleaseReleaserTask;

public class PublishDocsReleaseTask implements ReleaseReleaserTask {

	public static final int ORDER = 40;

	private final Releaser releaser;

	public PublishDocsReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "docs";
	}

	@Override
	public String shortName() {
		return "o";
	}

	@Override
	public String header() {
		return "PUBLISHING DOCS";
	}

	@Override
	public String description() {
		return "Publish the docs";
	}

	@Override
	public void accept(Arguments args) {
		this.releaser.publishDocs(args.originalVersion, args.versionFromBom);
	}

	@Override
	public int getOrder() {
		return PublishDocsReleaseTask.ORDER;
	}

}
