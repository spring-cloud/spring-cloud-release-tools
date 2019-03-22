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

package org.springframework.cloud.release.internal.options;

import java.util.ArrayList;
import java.util.List;

public class OptionsBuilder {

	private Boolean metaRelease = false;

	private Boolean fullRelease = false;

	private Boolean interactive = true;

	private List<String> taskNames = new ArrayList<>();

	private String startFrom = "";

	private String range = "";

	public OptionsBuilder metaRelease(Boolean metaRelease) {
		this.metaRelease = metaRelease;
		return this;
	}

	public OptionsBuilder fullRelease(Boolean fullRelease) {
		this.fullRelease = fullRelease;
		return this;
	}

	public OptionsBuilder interactive(Boolean interactive) {
		this.interactive = interactive;
		return this;
	}

	public OptionsBuilder taskNames(List<String> taskNames) {
		this.taskNames = taskNames;
		return this;
	}

	public OptionsBuilder startFrom(String startFrom) {
		this.startFrom = startFrom;
		return this;
	}

	public OptionsBuilder range(String range) {
		this.range = range;
		return this;
	}

	public Options options() {
		return new Options(this.metaRelease, this.fullRelease, this.interactive,
				this.taskNames, this.startFrom, this.range);
	}

}
