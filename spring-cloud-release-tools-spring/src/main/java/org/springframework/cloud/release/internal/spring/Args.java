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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Marcin Grzejszczak
 */
class Args {

	private static final Logger log = LoggerFactory.getLogger(Args.class);

	final Releaser releaser;

	final File project;

	final Projects projects;

	final ProjectVersion originalVersion;

	final ProjectVersion versionFromScRelease;

	final ReleaserProperties properties;

	final boolean interactive;

	final TaskType taskType;

	final ApplicationEventPublisher applicationEventPublisher;

	Args(Releaser releaser, File project, Projects projects,
			ProjectVersion originalVersion, ProjectVersion versionFromScRelease,
			ReleaserProperties properties, boolean interactive, TaskType taskType,
			ApplicationEventPublisher applicationEventPublisher) {
		this.releaser = releaser;
		this.project = project;
		this.projects = projects;
		this.originalVersion = originalVersion;
		this.versionFromScRelease = versionFromScRelease;
		this.properties = properties;
		this.interactive = interactive;
		this.taskType = taskType;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	// Used by meta-release task
	Args(Releaser releaser, Projects projects, ProjectVersion versionFromScRelease,
			ReleaserProperties properties, boolean interactive,
			ApplicationEventPublisher applicationEventPublisher) {
		this.releaser = releaser;
		this.project = null;
		this.projects = projects;
		this.originalVersion = null;
		this.versionFromScRelease = versionFromScRelease;
		this.properties = properties;
		this.interactive = interactive;
		this.taskType = TaskType.POST_RELEASE;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	// Used for tests
	Args(TaskType taskType) {
		this.releaser = null;
		this.project = null;
		this.projects = null;
		this.originalVersion = null;
		this.versionFromScRelease = null;
		this.properties = null;
		this.interactive = false;
		this.taskType = taskType;
		this.applicationEventPublisher = null;
	}

	String projectName() {
		return this.project != null ? this.project.getName() : "";
	}

	void publishEvent(ApplicationEvent applicationEvent) {
		if (this.applicationEventPublisher == null) {
			log.warn("Application Event Publisher not present");
			return;
		}
		this.applicationEventPublisher.publishEvent(applicationEvent);
	}

	@Override
	public String toString() {
		return "Args{" + "releaser=" + this.releaser + ", project=" + this.project
				+ ", projects=" + this.projects + ", originalVersion="
				+ this.originalVersion + ", versionFromScRelease="
				+ this.versionFromScRelease + ", properties=" + this.properties
				+ ", interactive=" + this.interactive + ", taskType=" + this.taskType
				+ '}';
	}

}
