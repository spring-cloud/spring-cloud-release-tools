package org.springframework.cloud.release.internal.project;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectBuilder {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String VERSION_MUSTACHE = "{{version}}";

	private final ReleaserProperties properties;
	private final ProcessExecutor executor;

	public ProjectBuilder(ReleaserProperties properties) {
		this.properties = properties;
		this.executor = new ProcessExecutor(properties);
	}

	ProjectBuilder(ReleaserProperties properties, ProcessExecutor executor) {
		this.properties = properties;
		this.executor = executor;
	}

	public void build() {
		try {
			String[] commands = commandWithSystemProps(this.properties.getMaven().getBuildCommand())
					.split(" ");
			runCommand(commands);
			assertNoHtmlFilesInDocsContainUnresolvedTags();
			log.info("No HTML files from docs contain unresolved tags");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String commandWithSystemProps(String command) {
		if (command.contains(ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER)) {
			return command;
		}
		return command.trim() + " " + ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER;
	}

	private void assertNoHtmlFilesInDocsContainUnresolvedTags() {
		String workingDir = this.properties.getWorkingDir();
		try {
			File docs = new File(workingDir, "docs");
			if (!docs.exists()) {
				return;
			}
			Files.walkFileTree(docs.toPath(), new HtmlFileWalker());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void deploy() {
		try {
			String[] commands = commandWithSystemProps(this.properties.getMaven().getDeployCommand())
					.split(" ");
			runCommand(commands);
			log.info("The project has successfully been deployed");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void runCommand(String[] commands) {
		String[] substitutedCommands = substituteSystemProps(commands);
		long waitTimeInMinutes = this.properties.getMaven().getWaitTimeInMinutes();
		this.executor.runCommand(substitutedCommands, waitTimeInMinutes);
	}

	public void publishDocs(String version) {
		try {
			for (String command : this.properties.getMaven().getPublishDocsCommands()) {
				command = command.replace(VERSION_MUSTACHE, version);
				String[] commands = command.split(" ");
				runCommand(commands);
			}
			log.info("The docs got published successfully");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * We need to insert the system properties as a list of -Dkey=value entries
	 * instead of just pasting the String that contains these values
	 */
	private String[] substituteSystemProps(String... commands) {
		boolean containsSystemProps = this.properties.getMaven().getSystemProperties().contains("-D");
		String[] splitSystemProps = StringUtils.delimitedListToStringArray(this.properties.getMaven()
				.getSystemProperties(), "-D");
		// first element might be empty even though the second one contains values
		if (splitSystemProps.length > 1) {
			splitSystemProps = StringUtils.isEmpty(splitSystemProps[0]) ?
					Arrays.copyOfRange(splitSystemProps, 1, splitSystemProps.length) :
					splitSystemProps;
		}
		String[] systemPropsWithPrefix = containsSystemProps ? Arrays.stream(splitSystemProps)
				.map(s -> "-D" + s.trim())
				.collect(Collectors.toList())
				.toArray(new String[splitSystemProps.length]) : splitSystemProps;
		final AtomicInteger index = new AtomicInteger(-1);
		for (int i = 0; i < commands.length; i++) {
			if (commands[i].contains(ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER)) {
				index.set(i);
				break;
			}
		}
		List<String> commandsList = new ArrayList<>(Arrays.asList(commands));
		List<String> systemPropsList = Arrays.asList(systemPropsWithPrefix);
		if (index.get() != -1) {
			commandsList.remove(index.get());
			if (index.get() >= commandsList.size()) {
				commandsList.addAll(systemPropsList);
			} else {
				// we need to reverse to set the objects in the same order as passed in the prop
				List<String> reversedSystemProps = new ArrayList<>(systemPropsList);
				Collections.reverse(reversedSystemProps);
				reversedSystemProps.forEach(s -> commandsList.add(index.get(), s));
			}
		}
		return commandsList.toArray(new String[commandsList.size()]);
	}
}

class ProcessExecutor {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ReleaserProperties properties;

	ProcessExecutor(ReleaserProperties properties) {
		this.properties = properties;
	}

	void runCommand(String[] commands, long waitTimeInMinutes) {
		try {
			String workingDir = this.properties.getWorkingDir();
			log.debug("Will run the build via {} and wait for result for [{}] minutes", commands, waitTimeInMinutes);
			ProcessBuilder builder = builder(commands, workingDir);
			Process process = startProcess(builder);
			boolean finished = process.waitFor(waitTimeInMinutes, TimeUnit.MINUTES);
			if (!finished) {
				log.error("The build hasn't managed to finish in [{}] minutes", waitTimeInMinutes);
				process.destroyForcibly();
				throw new IllegalStateException("Process waiting time of [" + waitTimeInMinutes + "] minutes exceeded");
			}
			if (process.exitValue() != 0) {
				throw new IllegalStateException("The process has exited with exit code [" + process.exitValue() + "]");
			}
		}
		catch (InterruptedException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	Process startProcess(ProcessBuilder builder) throws IOException {
		return builder.start();
	}

	ProcessBuilder builder(String[] commands, String workingDir) {
		return new ProcessBuilder(commands)
				.directory(new File(workingDir))
				.inheritIO();
	}
}

class HtmlFileWalker extends SimpleFileVisitor<Path> {

	private static final String HTML_EXTENSION = ".html";

	@Override public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
		File file = path.toFile();
		if (file.getName().endsWith(HTML_EXTENSION) && asString(file).contains("Unresolved")) {
			throw new IllegalStateException("File [" + file + "] contains a tag that wasn't resolved properly");
		}
		return FileVisitResult.CONTINUE;
	}

	private String asString(File file) {
		try {
			return new String(Files.readAllBytes(file.toPath()));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
