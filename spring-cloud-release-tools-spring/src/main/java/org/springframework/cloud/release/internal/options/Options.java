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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marcin Grzejszczak
 */
public class Options implements Serializable {

	/**
	 * Is meta release set.
	 */
	public Boolean metaRelease;

	/**
	 * Is full release set.
	 */
	public Boolean fullRelease;

	/**
	 * Is interactive mode set.
	 */
	public Boolean interactive;

	/**
	 * Is dry run set.
	 */
	public Boolean dryRun;

	/**
	 * List of task names to release.
	 */
	public List<String> taskNames;

	/**
	 * Name of the task / projects to start from.
	 */
	public String startFrom = "";

	/**
	 * Range of task / projects to release.
	 */
	public String range = "";

	Options(Boolean metaRelease, Boolean fullRelease, Boolean interactive, Boolean dryRun,
			List<String> taskNames, String startFrom, String range) {
		this.metaRelease = metaRelease;
		this.fullRelease = fullRelease;
		this.interactive = interactive;
		this.dryRun = dryRun;
		this.taskNames = taskNames.stream().map(this::removeQuotingChars)
				.collect(Collectors.toList());
		this.startFrom = removeQuotingChars(startFrom);
		this.range = removeQuotingChars(range);
	}

	private String removeQuotingChars(String string) {
		if (string != null && string.startsWith("'") && string.endsWith("'")) {
			return string.substring(1, string.length() - 1);
		}
		return string;
	}

	@Override
	public String toString() {
		return "Options{" + "metaRelease=" + this.metaRelease + ", fullRelease="
				+ this.fullRelease + ", interactive=" + this.interactive + ", dryRun="
				+ this.dryRun + ", taskNames=" + this.taskNames + ", startFrom='"
				+ this.startFrom + '\'' + ", range='" + this.range + '\'' + '}';
	}

}
