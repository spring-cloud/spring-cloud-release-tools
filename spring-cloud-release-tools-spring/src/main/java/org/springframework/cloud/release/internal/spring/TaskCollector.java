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

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

import com.jakewharton.fliptables.FlipTableConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.StringUtils;

class TaskCollector implements ApplicationListener<ReleaserTask> {

	private static final Logger log = LoggerFactory.getLogger(TaskCollector.class);

	private final Queue<TaskCompleted> completedTasks = new ArrayBlockingQueue<>(300);

	private void handleTaskCompleted(TaskCompleted taskCompleted) {
		this.completedTasks.add(taskCompleted);
		log.info("Completed task: " + taskCompleted);
	}

	private void handleBuildCompleted(BuildCompleted buildCompleted) {
		log.info("Build has finished. Will summarize the results");
		List<Table> table = this.completedTasks.stream()
				.map(task -> new Table(task.projectName, task.taskAndException))
				.collect(Collectors.toList());
		String string = "\n\n***** BUILD REPORT *****\n\n"
				+ FlipTableConverters.fromIterable(table, Table.class)
				+ "\n\n***** BUILD REPORT *****\n\n";
		List<Table> brokenTasks = table.stream()
				.filter(table1 -> StringUtils.hasText(table1.thrownException))
				.collect(Collectors.toList());
		if (!brokenTasks.isEmpty()) {
			String brokenBuilds = "\n\n[BUILD UNSTABLE] The following release tasks are failing!\n\n" +
					brokenTasks.stream()
							.map(table1 ->
									String.format("***** Project / Task : <%s/%s> ***** \nTask Description <%s>\nException Stacktrace \n\n%s",
											table1.projectName, table1.taskCaption,
											table1.taskDescription, table1.exception + "\n" + Arrays
													.stream(table1.exception.getStackTrace())
													.map(StackTraceElement::toString)
													.collect(Collectors.joining("\n"))))
							.collect(Collectors.joining("\n\n"));
			log.warn(string + brokenBuilds);
			this.completedTasks.clear();
			throw new IllegalStateException(brokenBuilds);
		} else {
			log.info(string);
			this.completedTasks.clear();
		}
	}

	@Override
	public void onApplicationEvent(ReleaserTask event) {
		if (event instanceof TaskCompleted) {
			handleTaskCompleted((TaskCompleted) event);
		} else if (event instanceof BuildCompleted) {
			handleBuildCompleted((BuildCompleted) event);
		}
	}
}

class Table {
	final String projectName;
	final String taskCaption;
	final String taskDescription;
	final String taskState;
	final String thrownException;
	Exception exception;

	Table(String projectName, TaskAndException tae) {
		this.projectName = StringUtils.hasText(projectName) ? projectName : "Post Release";
		this.taskCaption = tae.task.name;
		this.taskDescription = tae.task.description;
		this.taskState = tae.taskState.name().toLowerCase();
		this.thrownException = tae.exception == null ? "" :
				NestedExceptionUtils.getMostSpecificCause(tae.exception).toString();
		this.exception = tae.exception;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public String getTaskCaption() {
		return this.taskCaption;
	}

	public String getTaskDescription() {
		return this.taskDescription;
	}

	public String getTaskState() {
		return this.taskState;
	}

	public String getThrownException() {
		return this.thrownException;
	}
}
