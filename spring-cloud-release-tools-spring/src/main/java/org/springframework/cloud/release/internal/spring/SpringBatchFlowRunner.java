/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.tasks.CompositeReleaserTask;
import org.springframework.cloud.release.internal.tasks.ReleaseReleaserTask;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.cloud.release.internal.tech.MakeBuildUnstableException;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class SpringBatchFlowRunner implements FlowRunner {

	private static final Logger log = LoggerFactory.getLogger(SpringBatchFlowRunner.class);

	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";

	private final ConsoleInputStepSkipper stepSkipper = new ConsoleInputStepSkipper();

	private final StepBuilderFactory stepBuilderFactory;

	private final JobBuilderFactory jobBuilderFactory;

	private final ProjectsToRunFactory projectsToRunFactory;

	private final JobLauncher jobLauncher;

	SpringBatchFlowRunner(StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher) {
		this.stepBuilderFactory = stepBuilderFactory;
		this.jobBuilderFactory = jobBuilderFactory;
		this.projectsToRunFactory = projectsToRunFactory;
		this.jobLauncher = jobLauncher;
	}

	@Override
	public Decision beforeTask(Options options, ReleaserProperties properties, ReleaserTask releaserTask) {
		return decide(options, releaserTask);
	}

	private Step createStep(ReleaserTask releaserTask, Arguments args) {
		return this.stepBuilderFactory.get(releaserTask.name())
				.tasklet((contribution, chunkContext) -> {
					FlowRunner.Decision decision = beforeTask(args.options, args.properties, releaserTask);
					if (decision == FlowRunner.Decision.CONTINUE) {
						runTask(releaserTask, args);
					} else {
						log.info("Skipping step [{}]", releaserTask.name());
					}
					return RepeatStatus.FINISHED;
				})
				.listener(releaserListener(args, releaserTask))
				.build();
	}

	private void runTask(ReleaserTask releaserTask, Arguments args) {
		try {
			releaserTask.accept(args);
		} catch (Exception ex) {
			if (releaserTask instanceof ReleaseReleaserTask) {
				throw ex;
			}
			throw new MakeBuildUnstableException(ex);
		}
	}

	private StepExecutionListener releaserListener(Arguments args, ReleaserTask releaserTask) {
		return new StepExecutionListener() {
			@Override
			public void beforeStep(StepExecution stepExecution) {

			}

			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				FlowRunner.Decision decision = afterTask(args.options, args.properties, releaserTask);
				if (decision == FlowRunner.Decision.ABORT) {
					return ExitStatus.FAILED;
				}
				boolean makeUnstable = stepExecution.getFailureExceptions().stream().anyMatch(t -> t instanceof MakeBuildUnstableException);
				if (makeUnstable) {
					return new ExitStatus(MakeBuildUnstableException.EXIT_CODE, MakeBuildUnstableException.DESCRIPTION);
				}
				return stepExecution.getExitStatus();
			}
		};
	}

	@Override
	public void runReleaseTasks(Options options, ReleaserProperties properties, ProjectsToRun projectsToRun, TasksToRun tasksToRun) {
		projectsToRun.forEach(projectToRun -> {
			ProjectToRun startedProject = projectToRun.get();
			executeReleaseTasks(tasksToRun, startedProject.name(), Arguments.forProject(startedProject));
		});
	}

	@Override
	public void runPostReleaseTasks(Options options, ReleaserProperties properties, String taskName, TasksToRun tasksToRun) {
		ProjectsToRun projectsToRun = postReleaseProjects(new OptionsAndProperties(properties, options));
		Flow flow = postReleaseFlow(tasksToRun, properties, projectsToRun);
		if (flow == null) {
			log.info("No post release tasks to run, will do nothing");
			return;
		}
		Job job = this.jobBuilderFactory.get(taskName)
			.start(flow)
			.build()
		.build();
		runJob(job);
	}

	private ProjectsToRun postReleaseProjects(OptionsAndProperties options) {
		return this.projectsToRunFactory.postRelease(options);
	}

	private void executeReleaseTasks(TasksToRun tasksToRun, String name, Arguments args) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		if (!iterator.hasNext()) {
			return;
		}
		ReleaserTask task = iterator.next();
		// If composite, just run it instead of using Batch (otherwise a job will call another job)
		if (task instanceof CompositeReleaserTask) {
			log.info("Task [{}] is composite, will run it manually instead of calling it via Batch", task);
			task.accept(args);
			return;
		}
		runBatchJob(name, args, iterator, task);
	}

	private void runBatchJob(String name, Arguments args, Iterator<? extends ReleaserTask> iterator, ReleaserTask task) {
		JobBuilder builder = this.jobBuilderFactory.get(name);
		SimpleJobBuilder startedBuilder = builder.start(createStep(task, args));
		while (iterator.hasNext()) {
			startedBuilder.next(createStep(iterator.next(), args));
		}
		Job job = startedBuilder.build();
		runJob(job);
	}

	private void runJob(Job job) {
		try {
			JobExecution execution = this.jobLauncher.run(job, new JobParameters());
			if (MakeBuildUnstableException.EXIT_CODE.equals(execution.getExitStatus().getExitCode())) {
				throw new MakeBuildUnstableException(execution.getJobConfigurationName() + " failed!");
			} else if (!ExitStatus.COMPLETED.equals(execution.getExitStatus())) {
				throw new IllegalStateException("Job failed to get executed successfully. Failed with exit code [" + execution.getExitStatus().getExitCode() + "] and description [" + execution.getExitStatus().getExitDescription() + "]");
			}
		}
		catch (JobExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Flow postReleaseFlow(TasksToRun tasksToRun, ReleaserProperties properties, ProjectsToRun projectsToRun) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		ReleaserTask task = iterator.next();
		Flow flow = flow(properties, projectsToRun, task);
		FlowBuilder<Flow> flowBuilder = new FlowBuilder<Flow>("parallelPostRelease")
				.start(flow);
		if (!iterator.hasNext()) {
			return flowBuilder.build();
		}
		FlowBuilder.SplitBuilder<Flow> builder = flowBuilder
				.split(new SimpleAsyncTaskExecutor());
		List<Flow> flows = new LinkedList<>();
		while (iterator.hasNext()) {
			flows.add(flow(properties, projectsToRun, task));
		}
		Flow[] objects = flows.toArray(new Flow[0]);
		return builder
				.add(objects)
				.build();
	}

	private Flow flow(ReleaserProperties properties, ProjectsToRun projectsToRun, ReleaserTask task) {
		return new FlowBuilder<Flow>(task.name() + "Flow")
				.start(createStep(task, Arguments.forPostRelease(properties, projectsToRun)))
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