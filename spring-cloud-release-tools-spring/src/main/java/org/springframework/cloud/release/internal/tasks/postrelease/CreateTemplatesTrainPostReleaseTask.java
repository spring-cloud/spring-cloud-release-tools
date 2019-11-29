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

package org.springframework.cloud.release.internal.tasks.postrelease;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.tasks.TrainPostReleaseReleaserTask;

public class CreateTemplatesTrainPostReleaseTask implements TrainPostReleaseReleaserTask {

	public static final int ORDER = 70;

	private final Releaser releaser;

	public CreateTemplatesTrainPostReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "createTemplates";
	}

	@Override
	public String shortName() {
		return "t";
	}

	@Override
	public String header() {
		return "CREATING TEMPLATES";
	}

	@Override
	public String description() {
		return "Create email / blog / tweet etc. templates";
	}

	@Override
	public void accept(Arguments args) {
		this.releaser.createEmail(args.versionFromBom, args.projects);
		this.releaser.createBlog(args.versionFromBom, args.projects);
		this.releaser.createTweet(args.versionFromBom, args.projects);
		this.releaser.createReleaseNotes(args.versionFromBom, args.projects);
	}

	@Override
	public int getOrder() {
		return CreateTemplatesTrainPostReleaseTask.ORDER;
	}

}
