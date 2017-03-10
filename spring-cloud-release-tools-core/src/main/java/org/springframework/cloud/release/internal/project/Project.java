package org.springframework.cloud.release.internal.project;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;

/**
 * @author Marcin Grzejszczak
 */
public class Project {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ReleaserProperties properties;
	private final ProcessExecutor executor;

	public Project(ReleaserProperties properties) {
		this.properties = properties;
		this.executor = new ProcessExecutor(properties);
	}

	Project(ReleaserProperties properties, ProcessExecutor executor) {
		this.properties = properties;
		this.executor = executor;
	}

	public void build() {
		try {
			String[] commands = this.properties.getMaven().getBuildCommand().split(" ");
			runCommand(commands);
			assertNoHtmlFilesContainUnresolvedTags();
			log.info("No HTML files from docs contain unresolved tags");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void assertNoHtmlFilesContainUnresolvedTags() {
		String workingDir = this.properties.getWorkingDir();
		try {
			Files.walkFileTree(new File(workingDir).toPath(), new HtmlFileWalker());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void deploy() {
		try {
			String[] commands = this.properties.getMaven().getDeployCommand().split(" ");
			runCommand(commands);
			log.info("The project has successfully been deployed");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void runCommand(String[] commands) {
		long waitTimeInMinutes = this.properties.getMaven().getWaitTimeInMinutes();
		this.executor.runCommand(commands, waitTimeInMinutes);
	}

	public void publishDocs() {
		try {
			for (String command : this.properties.getMaven().getPublishDocsCommands()) {
				String[] commands = command.split(" ");
				runCommand(commands);
			}
			log.info("The docs got published successfully");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void bumpVersions(String version) {
		try {
			log.info("Bumping versions to [{}]", version);
			String[] commands = String.format(bumpVersionsCommand(), version).split(" ");
			runCommand(commands);
			log.info("Versions successfully bumped");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	String bumpVersionsCommand() {
		return this.properties.getMaven().getBumpVersionsCommand();
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
			log.info("Will run the build via {} and wait for result for [{}] minutes", commands, waitTimeInMinutes);
			ProcessBuilder builder = builder(commands, workingDir);
			Process process = builder.start();
			boolean finished = process.waitFor(waitTimeInMinutes, TimeUnit.MINUTES);
			if (!finished) {
				log.error("The build hasn't managed to finish in [{}] minutes", waitTimeInMinutes);
				process.destroyForcibly();
				throw new IllegalStateException("Process waiting time of [" + waitTimeInMinutes + "] minutes exceeded");
			}
		}
		catch (InterruptedException | IOException e) {
			throw new IllegalStateException(e);
		}
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
