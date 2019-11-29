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

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.spring.Arguments;
import org.springframework.cloud.release.internal.tasks.DryRunReleaseReleaserTask;
import org.springframework.cloud.release.internal.tasks.ReleaseReleaserTask;

public class UpdatingPomsReleaseTask implements ReleaseReleaserTask, DryRunReleaseReleaserTask {

	public static final int ORDER = 10;

	private final Releaser releaser;

	public UpdatingPomsReleaseTask(Releaser releaser) {
		this.releaser = releaser;
	}

	@Override
	public String name() {
		return "updatePoms";
	}

	@Override
	public String shortName() {
		return "u";
	}

	@Override
	public String header() {
		return "UPDATING VERSIONS";
	}

	@Override
	public String description() {
		return "Update versions from the BOM";
	}

	@Override
	public void accept(Arguments args) {
		this.releaser.updateProjectFromBom(args.project, args.projects, args.versionFromBom);
	}

	@Override
	public int getOrder() {
		return UpdatingPomsReleaseTask.ORDER;
	}

	@Override
	public List<TaskType> taskTypes() {
		return Arrays.asList(TaskType.RELEASE, TaskType.DRY_RUN);
	}
}
