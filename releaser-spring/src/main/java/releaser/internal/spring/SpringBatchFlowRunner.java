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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.options.Options;
import releaser.internal.tasks.CompositeReleaserTask;
import releaser.internal.tasks.PostReleaseReleaserTask;
import releaser.internal.tasks.ReleaseReleaserTask;
import releaser.internal.tasks.ReleaserTask;
import releaser.internal.tech.BuildUnstableException;
import releaser.internal.tech.ExecutionResult;

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
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.task.TaskExecutor;

class SpringBatchFlowRunner implements FlowRunner, Closeable {

	private static final Logger log = LoggerFactory
			.getLogger(SpringBatchFlowRunner.class);

	private static final String MSG = "\nPress 'q' to quit, 's' to skip, any key to continue\n\n";

	private final ConsoleInputStepSkipper stepSkipper;

	private final StepBuilderFactory stepBuilderFactory;

	private final JobBuilderFactory jobBuilderFactory;

	private final ProjectsToRunFactory projectsToRunFactory;

	private final JobLauncher jobLauncher;

	private final FlowRunnerTaskExecutorSupplier flowRunnerTaskExecutorSupplier;

	private static final List<TaskExecutor> EXECUTORS = new ArrayList<>();

	private final ExecutorService executorService;

	private final ReleaserProperties releaserProperties;

	SpringBatchFlowRunner(StepBuilderFactory stepBuilderFactory,
			JobBuilderFactory jobBuilderFactory,
			ProjectsToRunFactory projectsToRunFactory, JobLauncher jobLauncher,
			FlowRunnerTaskExecutorSupplier flowRunnerTaskExecutorSupplier,
			ConfigurableApplicationContext context, ReleaserProperties releaserProperties,
			BuildReportHandler reportHandler) {
		this.stepBuilderFactory = stepBuilderFactory;
		this.jobBuilderFactory = jobBuilderFactory;
		this.projectsToRunFactory = projectsToRunFactory;
		this.jobLauncher = jobLauncher;
		this.flowRunnerTaskExecutorSupplier = flowRunnerTaskExecutorSupplier;
		this.stepSkipper = new ConsoleInputStepSkipper(context, reportHandler);
		this.releaserProperties = releaserProperties;
		this.executorService = Executors.newFixedThreadPool(
				this.releaserProperties.getMetaRelease().getReleaseGroupThreadCount());
	}

	@Override
	public Decision beforeTask(Options options, ReleaserProperties properties,
			ReleaserTask releaserTask) {
		return decide(options, releaserTask);
	}

	private Step createStep(ReleaserTask releaserTask,
			NamedArgumentsSupplier argsSupplier) {
		return this.stepBuilderFactory
				.get(argsSupplier.projectName + "_" + releaserTask.name())
				.tasklet((contribution, chunkContext) -> {
					Arguments args = argsSupplier.get();
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
						String status = result.toStringResult();
						ExecutionResultReport entity = buildEntity(releaserTask, args,
								status, errors);
						contribution.getStepExecution().getExecutionContext()
								.put("entity", entity);
						if (result.isFailureOrUnstable()) {
							log.warn("The execution of [{}] failed or was unstable",
									entity.getReleaserTaskType().getSimpleName());
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
				}).listener(releaserListener(argsSupplier, releaserTask)).build();
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
		return executionResult(releaserTask, args);
	}

	private ExecutionResult executionResult(ReleaserTask releaserTask, Arguments args) {
		try {
			return releaserTask.apply(args);
		}
		catch (BuildUnstableException bue) {
			log.error("Unstable exception occurred while trying to execute the task [{}]",
					releaserTask.getClass().getSimpleName(), bue);
			return ExecutionResult.unstable(bue);
		}
		catch (Exception ex) {
			log.error("Exception occurred while trying to execute the task [{}]",
					releaserTask.getClass().getSimpleName(), ex);
			if (releaserTask instanceof ReleaseReleaserTask) {
				return ExecutionResult.failure(ex);
			}
			return ExecutionResult.unstable(ex);
		}
	}

	private StepExecutionListener releaserListener(NamedArgumentsSupplier argsSupplier,
			ReleaserTask releaserTask) {
		return new StepExecutionListenerSupport() {
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				Arguments args = argsSupplier.get();
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
		ProjectsToReleaseGroups groups = new ProjectsToReleaseGroups(properties);
		List<ReleaseGroup> releaseGroups = groups.toReleaseGroup(projectsToRun);
		if (groups.hasGroups()) {
			log.info("Found the following release groups {}", releaseGroups);
		}
		List<StuffToRun> flows = releaseGroups.stream()
				.map(group -> buildFlowForGroup(tasksToRun, group))
				.collect(Collectors.toCollection(LinkedList::new));
		Iterator<StuffToRun> flowsIterator = flows.iterator();
		if (!flowsIterator.hasNext()) {
			// nothing to run
			return ExecutionResult.success();
		}
		if (flows.stream().allMatch(StuffToRun::hasCompositeTask)) {
			log.info("You've picked composite jobs to run");
			return runComposites(flows, groups);
		}
		return runJob(buildJobForFlows(flowsIterator));
	}

	private ExecutionResult runComposites(List<StuffToRun> flows,
			ProjectsToReleaseGroups groups) {
		if (groups.hasGroups()) {
			// will run in parallel
			List<ExecutionResult> results = new LinkedList<>();
			for (StuffToRun flow : flows) {
				log.info("Releasing group [{}]", flow.releaseGroup);
				ExecutionResult executionResult = runInParallel(flow);
				log.info("Group [{}] execution result is [{}]", flow.releaseGroup,
						executionResult);
				if (executionResult.isFailure()) {
					// stop running any additional flows when an release task exception
					// was found
					throw executionResult.foundExceptions();
				}
				results.add(executionResult);
			}
			return results.stream().reduce(new ExecutionResult(), ExecutionResult::merge);
		}
		// will run in sequence
		return runInSequence(flows);
	}

	private ExecutionResult result(Future<ExecutionResult> future) {
		try {
			return future.get();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private ExecutionResult runInParallel(StuffToRun stuffToRun) {
		ExecutionResult results = ExecutionResult.success();
		log.info("Running composite tasks in parallel for {}", stuffToRun.releaseGroup);
		for (ProjectToRun.ProjectToRunSupplier s : stuffToRun.releaseGroup.projectsToRun) {
			CompositeReleaserTask releaserTask = stuffToRun.task;
			log.info("Scheduling a build for project [{}]", s.projectName());
			List<Future<ExecutionResult>> futures = new LinkedList<>();
			futures.add(this.executorService.submit(() -> {
				log.info("Running a composite task [{}] in parallel",
						releaserTask.name());
				return releaserTask.apply(Arguments.forProject(s.get()));
			}));
			boolean atLeastOneFailure = false;
			for (Future<ExecutionResult> future : futures) {
				ExecutionResult result = result(future);
				results = results.merge(result);
				if (result.isFailure()) {
					atLeastOneFailure = true;
				}
			}
			if (atLeastOneFailure) {
				log.warn(
						"At least one project failed within the group, will NOT continue with subsequent groups");
				break;
			}
		}
		return results;
	}

	private ExecutionResult runInSequence(List<StuffToRun> stuffToRunList) {
		ExecutionResult result = ExecutionResult.success();
		for (StuffToRun stuffToRun : stuffToRunList) {
			for (ProjectToRun.ProjectToRunSupplier s : stuffToRun.releaseGroup.projectsToRun) {
				result = result
						.merge(stuffToRun.task.apply(Arguments.forProject(s.get())));
				if (result.isFailure()) {
					return result;
				}
			}
		}
		return result;
	}

	private Job buildJobForFlows(Iterator<StuffToRun> flowsIterator) {
		JobBuilder release = this.jobBuilderFactory
				.get("release_" + System.currentTimeMillis());
		StuffToRun stuffToRun = flowsIterator.next();
		Flow first = stuffToRun.flow;
		JobFlowBuilder start = release.start(first);
		FlowBuilder<FlowJobBuilder> next = null;
		while (flowsIterator.hasNext()) {
			StuffToRun toRun = flowsIterator.next();
			next = start.next(toRun.flow);
		}
		FlowJobBuilder builder = next != null ? next.build() : start.build();
		return builder.build();
	}

	private StuffToRun buildFlowForGroup(TasksToRun tasksToRun, ReleaseGroup group) {
		FlowBuilder<Flow> flowBuilder = new FlowBuilder<>(
				group.flowName() + (group.shouldRunInParallel() ? "_Parallel" : "") + "_"
						+ System.currentTimeMillis());
		Iterator<ProjectToRun.ProjectToRunSupplier> iterator = group.iterator();
		if (!iterator.hasNext() || tasksToRun.isEmpty()) {
			return new StuffToRun(group, flowBuilder.build());
		}
		// only one project to run - run in sequence
		final ProjectToRun.ProjectToRunSupplier project = iterator.next();
		if (tasksToRun.size() == 1
				&& tasksToRun.get(0) instanceof CompositeReleaserTask) {
			return new StuffToRun(group, (CompositeReleaserTask) tasksToRun.get(0));
		}
		flowBuilder.start(toFlowOfTasks(tasksToRun,
				new NamedArgumentsSupplier(project.projectName(),
						() -> Arguments.forProject(project.get())),
				flowBuilder(project.projectName())));
		if (!iterator.hasNext()) {
			return new StuffToRun(group, flowBuilder.build());
		}
		// more projects, run them in parallel
		FlowBuilder.SplitBuilder<Flow> split = flowBuilder.split(taskExecutor());
		List<Flow> flows = new LinkedList<>();
		while (iterator.hasNext()) {
			final ProjectToRun.ProjectToRunSupplier nextProject = iterator.next();
			String projectName = project.projectName();
			flows.add(toFlowOfTasks(tasksToRun,
					new NamedArgumentsSupplier(project.projectName(),
							() -> Arguments.forProject(nextProject.get())),
					flowBuilder(projectName)));
		}
		return new StuffToRun(group, split.add(flows.toArray(new Flow[0])).end());
	}

	private FlowBuilder<Flow> flowBuilder(String projectName) {
		return new FlowBuilder<>(projectName + "_Flow_" + System.currentTimeMillis());
	}

	@Override
	public ExecutionResult runPostReleaseTrainTasks(Options options,
			ReleaserProperties properties, String taskName, TasksToRun tasksToRun) {
		ProjectsToRun projectsToRun = postReleaseTrainProjects(
				new OptionsAndProperties(properties, options));
		Flow flow = postReleaseFlow(tasksToRun, properties, projectsToRun);
		String name = taskName + "_" + System.currentTimeMillis();
		if (flow == null) {
			log.info("No release train post release tasks to run, will do nothing");
			return ExecutionResult.success();
		}
		Job job = this.jobBuilderFactory.get(name).start(flow).build().build();
		return runJob(job);
	}

	private ProjectsToRun postReleaseTrainProjects(OptionsAndProperties options) {
		return this.projectsToRunFactory.postReleaseTrain(options);
	}

	private Flow toFlowOfTasks(TasksToRun tasksToRun, NamedArgumentsSupplier args,
			FlowBuilder<Flow> flowBuilder) {
		Iterator<? extends ReleaserTask> iterator = tasksToRun.iterator();
		ReleaserTask task = iterator.next();
		flowBuilder.start(createStep(task, args));
		while (iterator.hasNext()) {
			flowBuilder.next(createStep(iterator.next(), args));
		}
		return flowBuilder.build();
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
			return ExecutionResult.failure(thrownExceptions);
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
		FlowBuilder.SplitBuilder<Flow> builder = flowBuilder.split(taskExecutor());
		List<Flow> flows = new LinkedList<>();
		while (iterator.hasNext()) {
			flows.add(flow(properties, projectsToRun, iterator.next()));
		}
		Flow[] objects = flows.toArray(new Flow[0]);
		return builder.add(objects).build();
	}

	private TaskExecutor taskExecutor() {
		TaskExecutor taskExecutor = this.flowRunnerTaskExecutorSupplier.get();
		EXECUTORS.add(taskExecutor);
		return taskExecutor;
	}

	private Flow flow(ReleaserProperties properties, ProjectsToRun projectsToRun,
			ReleaserTask task) {
		return new FlowBuilder<Flow>(task.name() + "Flow")
				.start(createStep(task, new NamedArgumentsSupplier("postRelease",
						() -> Arguments.forPostRelease(properties, projectsToRun))))
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
		String taskType = task instanceof PostReleaseReleaserTask ? "Post Release Task"
				: "Release Task";
		log.info("\n\n\n=== {} [{}] ===\n\n{} {}\n\n", task.header(), taskType,
				task.description(), interactive ? MSG : "");
	}

	@Override
	public void close() {
		EXECUTORS.stream().filter(t -> t instanceof DisposableBean).forEach(e -> {
			try {
				((DisposableBean) e).destroy();
			}
			catch (Exception ex) {
				log.debug("Exception occurred while trying to destroy the bean", ex);
			}
		});
		this.executorService.shutdown();
	}

}

class NamedArgumentsSupplier implements Supplier<Arguments> {

	final String projectName;

	final Supplier<Arguments> argumentsSupplier;

	final AtomicReference<Arguments> arguments = new AtomicReference<>();

	NamedArgumentsSupplier(String projectName, Supplier<Arguments> argumentsSupplier) {
		this.projectName = projectName;
		this.argumentsSupplier = argumentsSupplier;
	}

	@Override
	public Arguments get() {
		if (this.arguments.get() != null) {
			return arguments.get();
		}
		Arguments arguments = this.argumentsSupplier.get();
		this.arguments.set(arguments);
		return arguments;
	}

}

class ConsoleInputStepSkipper {

	private final ConfigurableApplicationContext context;

	private final BuildReportHandler reportHandler;

	ConsoleInputStepSkipper(ConfigurableApplicationContext context,
			BuildReportHandler reportHandler) {
		this.context = context;
		this.reportHandler = reportHandler;
	}

	public boolean skipStep() {
		String input = chosenOption();
		switch (input.toLowerCase()) {
		case "s":
			return true;
		case "q":
			reportHandler.reportBuildSummary();
			System.exit(SpringApplication.exit(this.context, () -> 0));
			return true;
		default:
			return false;
		}
	}

	String chosenOption() {
		return System.console().readLine();
	}

}

class StuffToRun {

	final ReleaseGroup releaseGroup;

	final Flow flow;

	final CompositeReleaserTask task;

	StuffToRun(ReleaseGroup releaseGroup, Flow flow) {
		this.releaseGroup = releaseGroup;
		this.flow = flow;
		this.task = null;
	}

	StuffToRun(ReleaseGroup releaseGroup, CompositeReleaserTask task) {
		this.releaseGroup = releaseGroup;
		this.task = task;
		this.flow = null;
	}

	boolean hasCompositeTask() {
		return this.task != null;
	}

}

class ProjectsToReleaseGroups {

	private final ReleaserProperties releaserProperties;

	ProjectsToReleaseGroups(ReleaserProperties releaserProperties) {
		this.releaserProperties = releaserProperties;
	}

	List<ReleaseGroup> toReleaseGroup(ProjectsToRun projectsToRun) {
		ReleaseGroups releaseGroups = new ReleaseGroups(
				this.releaserProperties.getMetaRelease().getReleaseGroups());
		return releaseGroup(releaseGroups, null, new LinkedList<>(),
				projectsToRun.iterator());
	}

	boolean hasGroups() {
		return !this.releaserProperties.getMetaRelease().getReleaseGroups().isEmpty();
	}

	private List<ReleaseGroup> releaseGroup(ReleaseGroups releaseGroups,
			ReleaseGroup currentGroup, List<ReleaseGroup> merged,
			Iterator<ProjectToRun.ProjectToRunSupplier> iterator) {
		if (iterator.hasNext()) {
			// sleuth
			ProjectToRun.ProjectToRunSupplier supplier = iterator.next();
			// sleuth,contract,gateway
			String[] group = releaseGroups.group(supplier.projectName());
			if (currentGroup != null && !currentGroup.sameGroup(group)) {
				// we've been adding things to the current group, but we need to stop now
				merged.add(currentGroup);
				currentGroup = null;
			}
			if (group.length == 0) {
				// if no group matching, return a single entry
				ReleaseGroup current = new ReleaseGroup(supplier, group);
				merged.add(current);
				return releaseGroup(releaseGroups, null, merged, iterator);
			}
			// if still in group add to the current one
			if (currentGroup != null) {
				currentGroup.add(supplier);
			}
			else {
				currentGroup = new ReleaseGroup(supplier, group);
			}
			return releaseGroup(releaseGroups, currentGroup, merged, iterator);
		}
		if (currentGroup != null) {
			merged.add(currentGroup);
		}
		return merged;
	}

}

class ReleaseGroup {

	final List<ProjectToRun.ProjectToRunSupplier> projectsToRun = new LinkedList<>();

	final String[] group;

	ReleaseGroup(ProjectToRun.ProjectToRunSupplier supplier, String[] group) {
		this.projectsToRun.add(supplier);
		this.group = group;
	}

	Iterator<ProjectToRun.ProjectToRunSupplier> iterator() {
		return projectsToRun.iterator();
	}

	void add(ProjectToRun.ProjectToRunSupplier projectToRun) {
		this.projectsToRun.add(projectToRun);
	}

	boolean sameGroup(String[] group) {
		return Arrays.equals(group, this.group);
	}

	boolean shouldRunInParallel() {
		return this.projectsToRun.size() > 1;
	}

	String flowName() {
		return this.projectsToRun.get(0).projectName() + "_Flow";
	}

	private String projectName() {
		return this.projectsToRun.isEmpty() ? "EMPTY"
				: this.projectsToRun.get(0).projectName();
	}

	@Override
	public String toString() {
		return "ReleaseGroup{" + "group="
				+ (group.length > 0 ? Arrays.toString(group) : projectName()) + '}';
	}

}

class ReleaseGroups {

	private final List<String> releaseGroups;

	ReleaseGroups(List<String> releaseGroups) {
		this.releaseGroups = releaseGroups;
	}

	String[] group(String name) {
		String foundGroup = this.releaseGroups.stream().filter(s -> s.contains(name))
				.findFirst().orElse(null);
		if (foundGroup == null) {
			return new String[0];
		}
		return foundGroup.split(",");
	}

}
