package org.springframework.cloud.release.internal.spring;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;
import org.springframework.cloud.release.internal.options.Parser;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class OptionsParser implements Parser {

	private static final Logger log = LoggerFactory.getLogger(OptionsParser.class);

	@Override
	public Options parse(String[] args) {
		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		try {
			ArgumentAcceptingOptionSpec<Boolean> metaReleaseOpt = parser
					.acceptsAll(Arrays.asList("x", "meta-release"),
							"Do you want to do the meta release?")
					.withRequiredArg().ofType(Boolean.class).defaultsTo(false);
			ArgumentAcceptingOptionSpec<Boolean> fullReleaseOpt = parser
					.acceptsAll(Arrays.asList("f", "full-release"),
							"Do you want to do the full release of a single project?")
					.withOptionalArg().ofType(Boolean.class).defaultsTo(false);
			ArgumentAcceptingOptionSpec<Boolean> interactiveOpt = parser
					.acceptsAll(Arrays.asList("i", "interactive"),
							"Do you want to set the properties from the command line of a single project?")
					.withRequiredArg().ofType(Boolean.class).defaultsTo(true);
			Tasks.NON_COMPOSITE_TASKS.forEach(task ->
					parser.acceptsAll(Arrays.asList(task.shortName, task.name),
							task.description)
							.withOptionalArg());
			ArgumentAcceptingOptionSpec<String> startFromOpt = parser
					.acceptsAll(Arrays.asList("a", "start-from"),
							"Starts all release task starting from the given task. Requires passing the task name (either one letter or the full name)")
					.withRequiredArg().ofType(String.class);
			ArgumentAcceptingOptionSpec<String> taskNamesOpt = parser
					.acceptsAll(Arrays.asList("tn", "task-names"),
							"Starts all release task for the given task names")
					.withRequiredArg().ofType(String.class).defaultsTo("");
			ArgumentAcceptingOptionSpec<String> rangeOpt = parser.acceptsAll(Arrays.asList("r", "range"),
					"Runs release tasks from the given range. Requires passing the task names with a hyphen. The first task is inclusive, the second inclusive. E.g. 's-m' would mean running 'snapshot', 'push' and 'milestone' tasks")
					.withRequiredArg().ofType(String.class);
			parser.acceptsAll(Arrays.asList("h", "help"))
					.withOptionalArg();
			OptionSet options = parser.parse(args);
			if (options.has("h")) {
				printHelpMessage(parser);
				System.exit(0);
			}
			Boolean metaRelease = options.valueOf(metaReleaseOpt);
			Boolean interactive = options.valueOf(interactiveOpt);
			Boolean fullRelease = options.has(fullReleaseOpt);
			List<String> providedTaskNames = StringUtils.hasText(options.valueOf(taskNamesOpt)) ?
					Arrays.asList(options.valueOf(taskNamesOpt).split(",")) :
					new ArrayList<>();
			List<String> allTaskNames = Tasks.NON_COMPOSITE_TASKS.stream()
					.map(task -> task.name)
					.collect(Collectors.toList());
			List<String> tasksFromOptions = Tasks.NON_COMPOSITE_TASKS.stream()
					.filter(task -> options.has(task.name) || options.has(task.shortName))
					.map(task -> task.name).collect(Collectors.toList());
			if (providedTaskNames.isEmpty()) {
				providedTaskNames.addAll(tasksFromOptions.isEmpty() ? allTaskNames : tasksFromOptions);
			}
			List<String> taskNames = allTaskNames.stream()
					.filter(providedTaskNames::contains)
					.collect(Collectors.toList());
			String startFrom = options.valueOf(startFromOpt);
			String range = options.valueOf(rangeOpt);
			Options buildOptions = new OptionsBuilder()
					.metaRelease(metaRelease)
					.fullRelease(fullRelease)
					.interactive(interactive)
					.taskNames(taskNames)
					.startFrom(startFrom)
					.range(range)
					.options();
			log.info("\n\nWill use the following options to process the project\n\n{}\n\n", buildOptions);
			return buildOptions;
		}
		catch (Exception e) {
			printErrorMessage(e, parser);
			throw e;
		}
	}

	private void printErrorMessage(Exception e, OptionParser parser) {
		System.err.println("Following exception has occurred: ");
		System.err.println(e.getMessage());
		e.printStackTrace();
		System.err.println(intro());
		System.err.println(
				"java -jar spring-cloud-release-tools-spring-1.0.0.BUILD-SNAPSHOT.jar [options...] ");
		try {
			parser.printHelpOn(System.err);
		} catch (IOException e1) {
			throw new IllegalStateException(e1);
		}
		System.err.println(examples());
	}

	private void printHelpMessage(OptionParser parser) {
		try {
			System.out.println(intro());
			parser.printHelpOn(System.out);
			System.out.println(examples());
		} catch (IOException e1) {
			throw new IllegalStateException(e1);
		}
	}

	private String intro() {
		return "\nHere you can find the list of tasks in order\n\n[" + Tasks.allTasksInOrder() + "]\n\n";
	}

	private String examples() {
		return "\nExamples of usage:\n\n"
				+ "Run 'build' & 'commit' & 'deploy'\n"
				+ "java -jar jar.jar -b -c -d\n\n"
				+ "Start from 'push'\n"
				+ "java -jar releaser.jar -a push\n\n"
				+ "Range 'docs' -> 'push'\n"
				+ "java -jar releaser.jar -r o-p\n\n"
				+ "\n\n";
	}
}
