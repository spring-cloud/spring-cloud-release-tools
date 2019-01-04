/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import org.springframework.context.ApplicationEvent;

class TaskCompleted extends ApplicationEvent {

	final String projectName;
	final TaskAndException taskAndException;

	/**
	 * Create a new ApplicationEvent.
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	TaskCompleted(Object source, String projectName, TaskAndException taskAndException) {
		super(source);
		this.taskAndException = taskAndException;
		this.projectName = projectName;
	}

	@Override
	public String toString() {
		return "TaskCompleted{" +
				"projectName='" + this.projectName + '\'' +
				", taskName=" + this.taskAndException.task.name +
				'}';
	}
}
