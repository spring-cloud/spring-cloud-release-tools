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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.tasks.CompositeReleaserTask;
import releaser.internal.tasks.ReleaseReleaserTask;
import releaser.internal.tasks.ReleaserTask;
import releaser.internal.tech.BuildUnstableException;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class SpringBatchFlowRunner implements FlowRunner {

	private static final Logger log = LoggerFactory
			.getLogger(SpringBatchFlowRunner.class);

	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";

	private final ConsoleInputStepSkipper stepSkipper = new ConsoleInputStepSkipper();

	private final StepBuilderFactory stepBuilderFactory;

	private final JobBuilderFactory jobBuilderFactory;

	private final ProjectsToRunFactory projectsToRunFactory;

	private final JobLauncher jobLauncher;

	SpringBatchFlowRunner(StepBuilderFactory stepBuilderFactory,
			JobBuilderFactory jobBuilderFactory,
			ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher) {
		this.stepBuilderFactory = stepBuilderFactory;
		this.jobBuilderFactory = jobBuilderFactory;
		this.projectsToRunFactory = projectsToRunFactory;
		this.jobLauncher = jobLauncher;
	}

	@Override
	public Decision beforeTask(Options options, ReleaserProperties properties,
			ReleaserTask releaserTask) {
		return decide(options, releaserTask);
	}

	private Step createStep(ReleaserTask releaserTask, Arguments args) {
		return this.stepBuilderFactory.get(releaserTask.name())
				.tasklet((contribution, chunkContext) -> {
					FlowRunner.Decision decision = beforeTask(args.options,
							args.properties, releaserTask);
					if (decision == FlowRunner.Decision.CONTINUE) {
						ExecutionResult result = runTask(releaserTask, args);
						contribution.getStepExecution().getExecutionContext()
								.put("result", result);
						List<Throwable> errors = (List<Throwable>) contribution
								.getStepExecution().getExecutionContext().get("errors");
						RuntimeException exception = result.foundExceptions();
						errors = addExceptionToErrors(errors, exception);
						String status = result.isUnstable() ? "UNSTABLE"
								: result.isFailure() ? "FAILURE" : "SUCCESS";
						ExecutionResultReport entity = buildEntity(releaserTask, args,
								status, errors);
						contribution.getStepExecution().getExecutionContext()
								.put("entity", entity);
						if (result.isFailureOrUnstable()) {
							contribution.getStepExecution().getExecutionContext()
									.put("errors", errors);
						}
					}
					else {
						ExecutionResultReport entity = buildEntity(releaserTask, args,
								"SKIPPED", Collections.emptyList());
						contribution.getStepExecution().getExecutionContext()
								.put("entity", entity);
						log.info("Skipping step [{}]", releaserTask.name());
					}
					return RepeatStatus.FINISHED;
				}).listener(releaserListener(args, releaserTask)).build();
	}

	private List<Throwable> addExceptionToErrors(List<Throwable> errors,
			RuntimeException exception) {
		if (errors == null) {
			errors = new LinkedList<>();
		}
		if (exception != null) {
			errors.add(exception);
		}
		return errors;
	}

	private ExecutionResultReport buildEntity(ReleaserTask releaserTask, Arguments args,
			String state, List<Throwable> errors) {
		String projectName = args.project.getName();
		String shortName = releaserTask.getClass().getSimpleName();
		String description = releaserTask.description();
		Class<? extends ReleaserTask> releaseType = releaserTask.getClass();
		return new ExecutionResultReport(projectName, shortName, description, releaseType,
				state, errors);
	}

	private ExecutionResult runTask(ReleaserTask releaserTask, Arguments args) {
		ExecutionResult executionResult = releaserTask.apply(args);
		if (executionResult.isFailure() && releaserTask instanceof ReleaseReleaserTask) {
			throw executionResult.foundExceptions();
		}
		return executionResult;
	}

	private StepExecutionListener releaserListener(Arguments args,
			ReleaserTask releaserTask) {
		return new StepExecutionListenerSupport() {
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				FlowRunner.Decision decision = afterTask(args.options, args.properties,
						releaserTask);
				if (decision == FlowRunner.Decision.ABORT) {
					return ExitStatus.FAILED;
				}
				ExecutionResult result = (ExecutionResult) stepExecution
						.getExecutionContext().get("result");
				if (result == null || result.isSuccess()) {
					return stepExecution.getExitStatus();
				}
				else if (result.isUnstable()) {
					return new ExitStatus(BuildUnstableException.EXIT_CODE,
							BuildUnstableException.DESCRIPTION);
				}
				else if (result.isFailure()) {
					return ExitStatus.FAILED;
				}
				return ExitStatus.COMPLETED;
			}
		};
	}

	@Override
	public ExecutionResult runReleaseTasks(Options options, ReleaserProperties properties,
			ProjectsToRun projectsToRun, TasksToRun tasksToRun) {
		return projectsToRun.stream().map(projectToRun -> {
			ProjectToRun startedProject = projectToRun.get();
			String name = startedProject.name() + "_" + System.currentTimeMillis();
			ExecutionResult result = executeReleaseTasks(tasksToRun, name,
					Arguments.forProject(startedProject));
			if (result.isFailure()) {
				throw result.foundExceptions();
			}
			return result;
		}).reduce(new ExecutionResult(), ExecutionResult::merge);
	}

	@Override
	public ExecutionResult runPostReleaseTrainTasks(Options options,
			ReleaserProperties properties, String taskName, TasksToRun tasksToRun) {
		ProjectsToRun projectsToRun = postReleaseTrainProjects(
				new OptionsAndProperties(properties, options));
		Flow flow = postReleaseFlow(tasksToRun, properties, projectsToRun);
		String name = taskName + "_" + System.currentTimeMillis();
		if (flow == null) {
			log.info("No post release tasks to run, will do nothing");
			return ExecutionResult.success();
		}
		Job job = this.jobBuilderFactory.get(name).start(flow).build().build();
		return runJob(job);
	}

	private ProjectsToRun postReleaseTrainProjects(OptionsAndProperties options) {
		return this.projectsToRunFactory.postReleaseTrain(options);
	}

	private ExecutionResult executeReleaseTasks(TasksToRun tasksToRun, String name,
			Arguments args) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		if (!iterator.hasNext()) {
			return ExecutionResult.success();
		}
		ReleaserTask task = iterator.next();
		// If composite, just run it instead of using Batch (otherwise a job will call
		// another job)
		if (task instanceof CompositeReleaserTask) {
			log.info(
					"Task [{}] is composite, will run it manually instead of calling it via Batch",
					task);
			return task.apply(args);
		}
		return runBatchJob(name, args, iterator, task);
	}

	private ExecutionResult runBatchJob(String name, Arguments args,
			Iterator<? extends ReleaserTask> iterator, ReleaserTask task) {
		if (!iterator.hasNext()) {
			log.info("No jobs to run, will do nothing");
			return ExecutionResult.success();
		}
		SimpleJobBuilder startedBuilder = this.jobBuilderFactory.get(name)
				.start(createStep(task, args));
		while (iterator.hasNext()) {
			startedBuilder.next(createStep(iterator.next(), args));
		}
		Job job = startedBuilder.preventRestart().build();
		return runJob(job);
	}

	private ExecutionResult runJob(Job job) {
		try {
			JobExecution execution = this.jobLauncher.run(job, new JobParameters());
			if (!ExitStatus.COMPLETED.equals(execution.getExitStatus())) {
				return ExecutionResult.failure(new IllegalStateException(
						"Job failed to get executed successfully. Failed with exit code ["
								+ execution.getExitStatus().getExitCode()
								+ "] and description ["
								+ execution.getExitStatus().getExitDescription() + "]"));
			}
			List<Exception> thrownExceptions = exceptionsThrownBySteps(execution);
			if (thrownExceptions.isEmpty()) {
				return ExecutionResult.success();
			}
			return new ExecutionResult(thrownExceptions);
		}
		catch (JobExecutionException ex) {
			return ExecutionResult.failure(ex);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Exception> exceptionsThrownBySteps(JobExecution execution) {
		return execution.getStepExecutions().stream()
				.map(e -> e.getExecutionContext().get("errors") != null
						? (List) e.getExecutionContext().get("errors") : new LinkedList())
				.reduce(new LinkedList<Exception>(), (o, o2) -> {
					o.addAll(o2);
					return o;
				});
	}

	private Flow postReleaseFlow(TasksToRun tasksToRun, ReleaserProperties properties,
			ProjectsToRun projectsToRun) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		ReleaserTask task = iterator.next();
		Flow flow = flow(properties, projectsToRun, task);
		FlowBuilder<Flow> flowBuilder = new FlowBuilder<Flow>(
				"parallelPostRelease_" + System.currentTimeMillis()).start(flow);
		if (!iterator.hasNext()) {
			return flowBuilder.build();
		}
		// TODO: Add an option to configure the task executor
		FlowBuilder.SplitBuilder<Flow> builder = flowBuilder.split(taskExecutor());
		List<Flow> flows = new LinkedList<>();
		while (iterator.hasNext()) {
			flows.add(flow(properties, projectsToRun, iterator.next()));
		}
		Flow[] objects = flows.toArray(new Flow[0]);
		return builder.add(objects).build();
	}

	private TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.initialize();
		return executor;
	}

	private Flow flow(ReleaserProperties properties, ProjectsToRun projectsToRun,
			ReleaserTask task) {
		return new FlowBuilder<Flow>(task.name() + "Flow").start(
				createStep(task, Arguments.forPostRelease(properties, projectsToRun)))
				.build();
	}

	Decision decide(Options options, ReleaserTask task) {
		boolean interactive = options.interactive;
		printLog(interactive, task);
		if (interactive) {
			boolean skipStep = this.stepSkipper.skipStep();
			return skipStep ? Decision.SKIP : Decision.CONTINUE;
		}
		return Decision.CONTINUE;
	}

	private void printLog(boolean interactive, ReleaserTask task) {
		log.info("\n\n\n=== {} ===\n\n{} {}\n\n", task.header(), task.description(),
				interactive ? MSG : "");
	}

}

class ConsoleInputStepSkipper {

	public boolean skipStep() {
		String input = chosenOption();
		switch (input.toLowerCase()) {
		case "s":
			return true;
		case "q":
			System.exit(0);
			return true;
		default:
			return false;
		}
	}

	String chosenOption() {
		return System.console().readLine();
	}

}
