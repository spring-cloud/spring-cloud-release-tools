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

package org.springframework.cloud.release.internal.project;

import java.io.File;
import java.io.IOException;
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
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectBuilder implements ReleaserPropertiesAware {

	public enum MavenProfile {

		/**
		 * Profile used for milestone versions.
		 */
		MILESTONE,

		/**
		 * Profile used for ga versions.
		 */
		CENTRAL,

		/**
		 * Profile used to run integration tests.
		 */
		INTEGRATION,

		/**
		 * Profile used to run guides publishing.
		 */
		GUIDES;

		/**
		 * Converts the profile to lowercase, maven command line property.
		 * @return profile with prepended -P
		 */
		public String asMavenProfile() {
			return "-P" + this.name().toLowerCase();
		}

	}

	private static final Logger log = LoggerFactory.getLogger(ProjectBuilder.class);

	private static final String VERSION_MUSTACHE = "{{version}}";

	private ReleaserProperties properties;

	public ProjectBuilder(ReleaserProperties properties) {
		this.properties = properties;
	}

	public void build(ProjectVersion versionFromReleaseTrain, MavenProfile... profiles) {
		build(versionFromReleaseTrain, this.properties.getWorkingDir(), profiles);
	}

	public void build(ProjectVersion versionFromReleaseTrain, String projectRoot,
			MavenProfile... profiles) {
		try {
			String[] commands = commandWithSystemProps(
					this.properties.getMaven().getBuildCommand(), versionFromReleaseTrain,
					profiles).split(" ");
			runCommand(projectRoot, commands);
			assertNoHtmlFilesInDocsContainUnresolvedTags(projectRoot);
			log.info("No HTML files from docs contain unresolved tags");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void generateReleaseTrainDocs(String version, String projectRoot) {
		try {
			String updatedCommand = this.properties.getMaven()
					.getGenerateReleaseTrainDocsCommand()
					.replace(VERSION_MUSTACHE, version);
			runCommand(projectRoot, updatedCommand.split(" "));
			assertNoHtmlFilesInDocsContainUnresolvedTags(this.properties.getWorkingDir());
			log.info("No HTML files from docs contain unresolved tags");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String commandWithSystemProps(String command, ProjectVersion version,
			MavenProfile... profiles) {
		if (command.contains(ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER)) {
			return appendProfile(command, version, profiles);
		}
		return appendProfile(command, version, profiles) + " "
				+ ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER;
	}

	private String appendProfile(String command, ProjectVersion version,
			MavenProfile... profiles) {
		String trimmedCommand = command.trim();
		if (version.isMilestone() || version.isRc()) {
			log.info("Adding the milestone profile to the Maven build");
			return trimmedCommand + " " + MavenProfile.MILESTONE.asMavenProfile()
					+ profilesToString(profiles);
		}
		else if (version.isRelease() || version.isServiceRelease()) {
			log.info("Adding the central profile to the Maven build");
			return trimmedCommand + " " + MavenProfile.CENTRAL.asMavenProfile()
					+ profilesToString(profiles);
		}
		else {
			log.info("The build is a snapshot one - will not add any profiles");
		}
		return trimmedCommand;
	}

	private String profilesToString(MavenProfile... profiles) {
		return Arrays.stream(profiles).map(profile -> "-P" + profile)
				.collect(Collectors.joining(" "));
	}

	private void assertNoHtmlFilesInDocsContainUnresolvedTags(String workingDir) {
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

	public void deploy(ProjectVersion version, MavenProfile... profiles) {
		try {
			String[] commands = commandWithSystemProps(
					this.properties.getMaven().getDeployCommand(), version, profiles)
							.split(" ");
			runCommand(commands);
			log.info("The project has successfully been deployed");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private void runCommand(String[] commands) {
		runCommand(this.properties.getWorkingDir(), commands);
	}

	private void runCommand(String projectRoot, String[] commands) {
		String[] substitutedCommands = substituteSystemProps(commands);
		long waitTimeInMinutes = this.properties.getMaven().getWaitTimeInMinutes();
		executor(projectRoot).runCommand(substitutedCommands, waitTimeInMinutes);
	}

	ProcessExecutor executor(String workDir) {
		return new ProcessExecutor(workDir);
	}

	public void publishDocs(String version) {
		try {
			for (String command : this.properties.getMaven().getPublishDocsCommands()) {
				command = command.replace(VERSION_MUSTACHE, version);
				String[] commands = command.split(" ");
				runCommand(commands);
			}
			log.info("The docs got published successfully");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * We need to insert the system properties as a list of -Dkey=value entries instead of
	 * just pasting the String that contains these values.
	 */
	private String[] substituteSystemProps(String... commands) {
		boolean containsSystemProps = this.properties.getMaven().getSystemProperties()
				.contains("-D");
		String[] splitSystemProps = StringUtils.delimitedListToStringArray(
				this.properties.getMaven().getSystemProperties(), "-D");
		// first element might be empty even though the second one contains values
		if (splitSystemProps.length > 1) {
			splitSystemProps = StringUtils.isEmpty(splitSystemProps[0])
					? Arrays.copyOfRange(splitSystemProps, 1, splitSystemProps.length)
					: splitSystemProps;
		}
		String[] systemPropsWithPrefix = containsSystemProps ? Arrays
				.stream(splitSystemProps).map(s -> "-D" + s.trim())
				.collect(Collectors.toList()).toArray(new String[splitSystemProps.length])
				: splitSystemProps;
		final AtomicInteger index = new AtomicInteger(-1);
		for (int i = 0; i < commands.length; i++) {
			if (commands[i].contains(ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER)) {
				index.set(i);
				break;
			}
		}
		return toCommandList(systemPropsWithPrefix, index, commands);
	}

	private String[] toCommandList(String[] systemPropsWithPrefix, AtomicInteger index,
			String[] commands) {
		List<String> commandsList = new ArrayList<>(Arrays.asList(commands));
		List<String> systemPropsList = Arrays.asList(systemPropsWithPrefix);
		if (index.get() != -1) {
			commandsList.remove(index.get());
			if (index.get() >= commandsList.size()) {
				commandsList.addAll(systemPropsList);
			}
			else {
				// we need to reverse to set the objects in the same order as passed in
				// the prop
				List<String> reversedSystemProps = new ArrayList<>(systemPropsList);
				Collections.reverse(reversedSystemProps);
				reversedSystemProps.forEach(s -> commandsList.add(index.get(), s));
			}
		}
		return commandsList.toArray(new String[commandsList.size()]);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}

class ProcessExecutor implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

	private String workingDir;

	ProcessExecutor(String workingDir) {
		this.workingDir = workingDir;
	}

	void runCommand(String[] commands, long waitTimeInMinutes) {
		try {
			String workingDir = this.workingDir;
			log.debug(
					"Will run the command from [{}] via {} and wait for result for [{}] minutes",
					workingDir, commands, waitTimeInMinutes);
			ProcessBuilder builder = builder(commands, workingDir);
			Process process = startProcess(builder);
			boolean finished = process.waitFor(waitTimeInMinutes, TimeUnit.MINUTES);
			if (!finished) {
				log.error("The command hasn't managed to finish in [{}] minutes",
						waitTimeInMinutes);
				process.destroyForcibly();
				throw new IllegalStateException("Process waiting time of ["
						+ waitTimeInMinutes + "] minutes exceeded");
			}
			if (process.exitValue() != 0) {
				throw new IllegalStateException("The process has exited with exit code ["
						+ process.exitValue() + "]");
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
		return new ProcessBuilder(commands).directory(new File(workingDir)).inheritIO();
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.workingDir = properties.getWorkingDir();
	}

}

class HtmlFileWalker extends SimpleFileVisitor<Path> {

	private static final String HTML_EXTENSION = ".html";

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
		File file = path.toFile();
		if (file.getName().endsWith(HTML_EXTENSION)
				&& asString(file).contains("Unresolved")) {
			throw new IllegalStateException(
					"File [" + file + "] contains a tag that wasn't resolved properly");
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
