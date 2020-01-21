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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
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
import releaser.internal.tech.ExecutionResult;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.StringUtils;

class SpringBatchExecutionResultHandler
		implements ExecutionResultHandler, BuildReportHandler {

	private static final Logger log = LoggerFactory
			.getLogger(SpringBatchExecutionResultHandler.class);

	private final JobExplorer jobExplorer;

	private final ConfigurableApplicationContext context;

	SpringBatchExecutionResultHandler(JobExplorer jobExplorer,
			ConfigurableApplicationContext context) {
		this.jobExplorer = jobExplorer;
		this.context = context;
	}

	@Override
	public void accept(ExecutionResult executionResult) {
		reportBuildSummary();
		if (executionResult.isFailure()) {
			log.error("At least one failure occurred while running the release process",
					executionResult.foundExceptions());
			handleFailedBuild();
			exitWithException();
			return;
		}
		else if (executionResult.isUnstable()) {
			log.warn(
					"The release went fine, however at least one unstable post release task was found",
					executionResult.foundExceptions());
			handleUnstableException();
		}
		handleStableBuild();
		exitSuccessfully();
	}

	void exitSuccessfully() {
		System.exit(SpringApplication.exit(this.context, () -> 0));
	}

	void exitWithException() {
		System.exit(SpringApplication.exit(this.context, () -> 1));
	}

	@Override
	public void reportBuildSummary() {
		List<String> jobNames = this.jobExplorer.getJobNames();
		List<JobExecution> sortedJobExecutions = jobNames.stream()
				.flatMap(name -> this.jobExplorer.findJobInstancesByJobName(name, 0, 100)
						.stream())
				.flatMap(instance -> this.jobExplorer.getJobExecutions(instance).stream())
				.filter(j -> !j.isRunning())
				.sorted(Comparator.comparing(JobExecution::getCreateTime))
				.collect(Collectors.toList());
		List<StepExecution> stepContexts = sortedJobExecutions.stream()
				.flatMap(j -> j.getStepExecutions().stream())
				.collect(Collectors.toCollection(LinkedList::new));
		printTable(buildTable(stepContexts));
	}

	private List<Table> buildTable(List<StepExecution> stepContexts) {
		return stepContexts.stream().map(step -> {
			String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
					.format(step.getStartTime());
			long millis = ChronoUnit.MILLIS.between(step.getStartTime().toInstant(),
					step.getEndTime().toInstant());
			ExecutionContext context = step.getExecutionContext();
			ExecutionResultReport entity = (ExecutionResultReport) context.get("entity");
			if (entity == null) {
				return null;
			}
			String projectName = TrainPostReleaseReleaserTask.class
					.isAssignableFrom(entity.getReleaserTaskType()) ? "postRelease"
							: entity.getProjectName();
			return new Table(date, time(millis), projectName, entity.getShortName(),
					entity.getDescription(), entity.getState(), entity.getExceptions());
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
		String string = "\n\n***** BUILD REPORT *****\n\n"
				+ FlipTableConverters.fromIterable(table, Table.class)
				+ "\n\n***** BUILD REPORT *****\n\n";
		List<Table> brokenTasks = table.stream()
				.filter(table1 -> StringUtils.hasText(table1.thrownException))
				.collect(Collectors.toList());
		if (!brokenTasks.isEmpty()) {
			String brokenBuilds = "\n\n[BUILD UNSTABLE] The following release tasks are failing!\n\n"
					+ brokenTasks.stream().map(table1 -> String.format(
							"***** Project / Task : <%s/%s> ***** \nTask Description <%s>\nException Stacktrace \n\n%s",
							table1.projectName, table1.taskCaption,
							table1.taskDescription,
							table1.exceptions + "\n" + table1.exceptions.stream()
									.map(Throwable::getStackTrace).flatMap(Arrays::stream)
									.map(StackTraceElement::toString)
									.collect(Collectors.joining("\n"))))
							.collect(Collectors.joining("\n\n"));
			log.warn(string + brokenBuilds);
		}
		else {
			log.info(string);
		}
	}

	// File creation required by Jenkins
	private void handleUnstableException() {
		File buildStatus = new File("build_status");
		try {
			buildStatus.createNewFile();
			String text = "[BUILD UNSTABLE] The release happened successfully, but there were post release issues";
			Files.write(buildStatus.toPath(), text.getBytes());
			log.info(
					"\n\n\n  ___ _   _ ___ _    ___    _   _ _  _ ___ _____ _   ___ _    ___ \n"
							+ " | _ ) | | |_ _| |  |   \\  | | | | \\| / __|_   _/_\\ | _ ) |  | __|\n"
							+ " | _ \\ |_| || || |__| |) | | |_| | .` \\__ \\ | |/ _ \\| _ \\ |__| _| \n"
							+ " |___/\\___/|___|____|___/   \\___/|_|\\_|___/ |_/_/ \\_\\___/____|___|\n"
							+ "                                                                  ");
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"[BUILD UNSTABLE] Couldn't create a file to show that the build is unstable");
		}
	}

	// File creation required by Jenkins
	private void handleStableBuild() {
		File buildStatus = new File("build_status");
		if (buildStatus.exists()) {
			log.info("Build status file has already been created!");
			return;
		}
		try {
			buildStatus.createNewFile();
			String text = "[BUILD STABLE] All the release steps have been successfully executed!";
			Files.write(buildStatus.toPath(), text.getBytes());
			log.info(
					"\n\n\n  ___ _   _ ___ _    ___    ___ _   _  ___ ___ ___ ___ ___ ___ _   _ _    \n"
							+ " | _ ) | | |_ _| |  |   \\  / __| | | |/ __/ __| __/ __/ __| __| | | | |   \n"
							+ " | _ \\ |_| || || |__| |) | \\__ \\ |_| | (_| (__| _|\\__ \\__ \\ _|| |_| | |__ \n"
							+ " |___/\\___/|___|____|___/  |___/\\___/ \\___\\___|___|___/___/_|  \\___/|____|\n"
							+ "                                                                          ");
		}
		catch (IOException e) {
			log.info("Failed to store the file but the build was stable");
		}
	}

	// File creation required by Jenkins
	private void handleFailedBuild() {
		File buildStatus = new File("build_status");
		if (buildStatus.exists()) {
			log.info("Build status file has already been created!");
			return;
		}
		try {
			buildStatus.createNewFile();
			String text = "[BUILD FAILED] There were exceptions while doing the release!";
			Files.write(buildStatus.toPath(), text.getBytes());
			log.info("\n\n\n  ___ _   _ ___ _    ___    ___ _   ___ _    ___ ___  \n"
					+ " | _ ) | | |_ _| |  |   \\  | __/_\\ |_ _| |  | __|   \\ \n"
					+ " | _ \\ |_| || || |__| |) | | _/ _ \\ | || |__| _|| |) |\n"
					+ " |___/\\___/|___|____|___/  |_/_/ \\_\\___|____|___|___/ \n"
					+ "                                                      ");
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"[BUILD FAILED] Couldn't create a file to show that the build is unstable");
		}
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

	Table(String creationTime, String executionTime, String projectName,
			String taskCaption, String taskDescription, String taskState,
			List<Throwable> exceptions) {
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
