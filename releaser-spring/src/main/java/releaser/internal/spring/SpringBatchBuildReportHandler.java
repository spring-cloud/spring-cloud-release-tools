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

package releaser.internal.spring;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.jakewharton.fliptables.FlipTableConverters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.tasks.TrainPostReleaseReleaserTask;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.StringUtils;

class SpringBatchBuildReportHandler implements BuildReportHandler {

	private static final Logger log = LoggerFactory.getLogger(SpringBatchExecutionResultHandler.class);

	private final JobExplorer jobExplorer;

	SpringBatchBuildReportHandler(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	@Override
	public void reportBuildSummary() {
		List<String> jobNames = this.jobExplorer.getJobNames();
		List<JobExecution> sortedJobExecutions = jobNames.stream()
				.flatMap(name -> this.jobExplorer.findJobInstancesByJobName(name, 0, 100).stream())
				.flatMap(instance -> this.jobExplorer.getJobExecutions(instance).stream()).filter(j -> !j.isRunning())
				.sorted(Comparator.comparing(JobExecution::getCreateTime)).collect(Collectors.toList());
		List<StepExecution> stepContexts = sortedJobExecutions.stream().flatMap(j -> j.getStepExecutions().stream())
				.collect(Collectors.toCollection(LinkedList::new));
		printTable(buildTable(stepContexts));
	}

	private List<Table> buildTable(List<StepExecution> stepContexts) {
		return stepContexts.stream().map(step -> {
			String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(step.getStartTime());
			long millis = ChronoUnit.MILLIS.between(step.getStartTime().toInstant(ZoneOffset.UTC),
					step.getEndTime().toInstant(ZoneOffset.UTC));
			ExecutionContext context = step.getExecutionContext();
			ExecutionResultReport entity = (ExecutionResultReport) context.get("entity");
			if (entity == null) {
				return null;
			}
			String projectName = TrainPostReleaseReleaserTask.class.isAssignableFrom(entity.getReleaserTaskType())
					? "postRelease" : entity.getProjectName();
			return new Table(date, time(millis), projectName, entity.getShortName(), entity.getDescription(),
					entity.getState(), entity.getExceptions());
		}).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedList::new));
	}

	private String time(long millis) {
		long minutes = (millis / 1000) / 60;
		long seconds = (millis / 1000) % 60;
		if (minutes == 0 && seconds == 0) {
			return millis + " ms";
		}
		else if (minutes == 0) {
			return seconds + " s";
		}
		return minutes + " min " + seconds + " s";
	}

	private void printTable(List<Table> table) {
		String string = "\n\n***** BUILD REPORT *****\n\n" + FlipTableConverters.fromIterable(table, Table.class)
				+ "\n\n***** BUILD REPORT *****\n\n";
		List<Table> brokenTasks = table.stream().filter(table1 -> StringUtils.hasText(table1.thrownException))
				.collect(Collectors.toList());
		if (!brokenTasks.isEmpty()) {
			String brokenBuilds = "\n\n[BUILD UNSTABLE] The following release tasks are failing!\n\n" + brokenTasks
					.stream()
					.map(table1 -> String.format(
							"***** Project / Task : <%s/%s> ***** \nTask Description <%s>\nException Stacktrace \n\n%s",
							table1.projectName, table1.taskCaption, table1.taskDescription,
							table1.exceptions + "\n"
									+ table1.exceptions.stream().map(Throwable::getStackTrace).flatMap(Arrays::stream)
											.map(StackTraceElement::toString).collect(Collectors.joining("\n"))))
					.collect(Collectors.joining("\n\n"));
			log.warn(string + brokenBuilds);
		}
		else {
			log.info(string);
		}
	}

	class Table {

		final String creationTime;

		final String executionTime;

		final String projectName;

		final String taskCaption;

		final String taskDescription;

		final String taskState;

		final String thrownException;

		List<Throwable> exceptions;

		Table(String creationTime, String executionTime, String projectName, String taskCaption, String taskDescription,
				String taskState, List<Throwable> exceptions) {
			this.creationTime = creationTime;
			this.executionTime = executionTime;
			this.projectName = projectName;
			this.taskCaption = taskCaption;
			this.taskDescription = taskDescription;
			this.taskState = taskState;
			this.thrownException = exceptions == null ? "" : exceptions.stream()
					// TODO: Last but most specific
					.map(t -> NestedExceptionUtils.getMostSpecificCause(t).toString())
					.collect(Collectors.joining("\n"));
			this.exceptions = exceptions;
		}

		public String getExecutionTime() {
			return this.executionTime;
		}

		public String getCreationTime() {
			return this.creationTime;
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

}
