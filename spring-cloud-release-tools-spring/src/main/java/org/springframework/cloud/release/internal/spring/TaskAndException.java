/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

/**
 * @author Marcin Grzejszczak
 */
final class TaskAndException {

	final Task task;

	final TaskState taskState;

	final Exception exception;

	private TaskAndException(Task task, TaskState taskState) {
		this.task = task;
		this.taskState = taskState;
		this.exception = null;
	}

	private TaskAndException(Task task, TaskState taskState, Exception exception) {
		this.task = task;
		this.taskState = taskState;
		this.exception = exception;
	}

	static TaskAndException skipped(Task task) {
		return new TaskAndException(task, TaskState.SKIPPED);
	}

	static TaskAndException success(Task task) {
		return new TaskAndException(task, TaskState.SUCCESS);
	}

	static TaskAndException failure(Task task, Exception exception) {
		return new TaskAndException(task, TaskState.FAILURE, exception);
	}

	enum TaskState {

		SKIPPED, SUCCESS, FAILURE

	}

}
