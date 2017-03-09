package org.springframework.cloud.release.internal.builder;

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
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectBuilder {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
			String[] commands = this.properties.getMaven().getBuildCommand().split(" ");
			long waitTimeInMinutes = this.properties.getMaven().getWaitTimeInMinutes();
			this.executor.runCommand(commands, waitTimeInMinutes);
			assertNoHtmlFilesContainUnresolvedTags();
			log.info("No HTML files from docs contain unresolved tags");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void assertNoHtmlFilesContainUnresolvedTags() {
		String workingDir = StringUtils.hasText(this.properties.getWorkingDir()) ?
				this.properties.getWorkingDir() : System.getProperty("user.dir");
		try {
			Files.walkFileTree(new File(workingDir).toPath(), new HtmlFileWalker());
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void deploy() {

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
			String workingDir = StringUtils.hasText(this.properties.getWorkingDir()) ?
					this.properties.getWorkingDir() : System.getProperty("user.dir");
			log.info("Will run the build via {} and wait for result for [{}] minutes", commands, waitTimeInMinutes);
			ProcessBuilder builder = builder(commands, workingDir);
			Process process = builder.start();
			boolean finished = process.waitFor(waitTimeInMinutes, TimeUnit.MINUTES);
			if (!finished) {
				log.error("The build hasn't managed to finish in [{}] minutes", waitTimeInMinutes);
				process.destroyForcibly();
				throw new IllegalStateException("Build waiting time of [" + waitTimeInMinutes + "] minutes exceeded");
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
