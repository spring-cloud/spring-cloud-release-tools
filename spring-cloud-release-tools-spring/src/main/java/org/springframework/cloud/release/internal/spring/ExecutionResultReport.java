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

package org.springframework.cloud.release.internal.spring;

import java.util.List;

import org.springframework.cloud.release.internal.tasks.ReleaserTask;

/**
 * A report from running a task. Can be mapped to a row in a table for a single execution
 * of a release task.
 */
public class ExecutionResultReport {

	private String projectName;

	private String shortName;

	private String description;

	private Class<? extends ReleaserTask> releaserTaskType;

	private String state;

	private List<Throwable> exceptions;

	ExecutionResultReport() {
	}

	public ExecutionResultReport(String projectName, String shortName, String description,
			Class<? extends ReleaserTask> releaserTaskType, String state,
			List<Throwable> exceptions) {
		this.projectName = projectName;
		this.shortName = shortName;
		this.description = description;
		this.releaserTaskType = releaserTaskType;
		this.state = state;
		this.exceptions = exceptions;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getShortName() {
		return this.shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Class<? extends ReleaserTask> getReleaserTaskType() {
		return this.releaserTaskType;
	}

	public void setReleaserTaskType(Class<? extends ReleaserTask> releaserTaskType) {
		this.releaserTaskType = releaserTaskType;
	}

	public String getState() {
		return this.state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public List<Throwable> getExceptions() {
		return this.exceptions;
	}

	public void setExceptions(List<Throwable> exceptions) {
		this.exceptions = exceptions;
	}

}
