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
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.tasks.ReleaserTask;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

class SpringBatchFlowRunner implements FlowRunner {

	private static final Logger log = LoggerFactory.getLogger(SpringBatchFlowRunner.class);

	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";

	static StepSkipper stepSkipper = new ConsoleInputStepSkipper();

	private final StepBuilderFactory stepBuilderFactory;

	private final JobBuilderFactory jobBuilderFactory;

	SpringBatchFlowRunner(StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory) {
		this.stepBuilderFactory = stepBuilderFactory;
		this.jobBuilderFactory = jobBuilderFactory;
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
						releaserTask.accept(args);
					} else {
						log.info("Skipping step [{}]", releaserTask.name());
					}
					return RepeatStatus.FINISHED;
				})
				.listener(releaserListener(args, releaserTask))
				.build();
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
	public void runPostReleaseTasks(Options options, ReleaserProperties properties, String taskName, TasksToRun tasksToRun, ProjectsToRun projectsToRun) {
		postReleaseFlow(tasksToRun, properties, options, projectsToRun);
	}

	private Job executeReleaseTasks(TasksToRun tasksToRun, String name, Arguments args) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		JobBuilder builder = this.jobBuilderFactory.get(name);
		ReleaserTask task = iterator.next();
		SimpleJobBuilder startedBuilder = builder.start(createStep(task, args));
		while (iterator.hasNext()) {
			startedBuilder.next(createStep(iterator.next(), args));
		}
		return startedBuilder.build();
	}

	private Flow postReleaseFlow(TasksToRun tasksToRun, ReleaserProperties properties, Options options, ProjectsToRun projectsToRun) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		ReleaserTask task = iterator.next();
		Flow flow = flow(properties, options, projectsToRun, task);
		FlowBuilder<Flow> flowBuilder = new FlowBuilder<Flow>("parallelPostRelease")
				.start(flow);
		if (!iterator.hasNext()) {
			return flowBuilder.build();
		}
		FlowBuilder.SplitBuilder<Flow> builder = flowBuilder
				.split(new SimpleAsyncTaskExecutor());
		List<Flow> flows = new LinkedList<>();
		while (iterator.hasNext()) {
			flows.add(flow(properties, options, projectsToRun, task));
		}
		Flow[] objects = flows.toArray(new Flow[0]);
		return builder
				.add(objects)
				.build();
	}

	private Flow flow(ReleaserProperties properties, Options options, ProjectsToRun projectsToRun, ReleaserTask task) {
		return new FlowBuilder<Flow>(task.name() + "Flow")
				.start(createStep(task, Arguments.forPostRelease(properties, options, projectsToRun)))
				.build();
	}

	private Decision decide(Options options, ReleaserTask task) {
		boolean interactive = options.interactive;
		printLog(interactive, task);
		if (interactive) {
			boolean skipStep = stepSkipper.skipStep();
			return skipStep ? Decision.SKIP : Decision.CONTINUE;
		}
		return Decision.CONTINUE;
	}

	private void printLog(boolean interactive, ReleaserTask task) {
		log.info("\n\n\n=== {} ===\n\n{} {}\n\n", task.header(), task.description(),
				interactive ? MSG : "");
	}
}
