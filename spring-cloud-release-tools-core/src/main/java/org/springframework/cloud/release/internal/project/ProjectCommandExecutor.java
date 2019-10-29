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
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectCommandExecutor implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory
			.getLogger(ProjectCommandExecutor.class);

	private static final String VERSION_MUSTACHE = "{{version}}";

	private ReleaserProperties properties;

	public ProjectCommandExecutor(ReleaserProperties properties) {
		this.properties = properties;
	}

	// If you want to call commands that are not parameterized via the props
	public ProjectCommandExecutor() {
		this.properties = new ReleaserProperties();
	}

	public void build(ProjectVersion versionFromReleaseTrain) {
		build(versionFromReleaseTrain, this.properties.getWorkingDir());
	}

	public String version() {
		return executeCommand(
				new CommandPicker(this.properties, this.properties.getWorkingDir())
						.version());
	}

	public String groupId() {
		return executeCommand(
				new CommandPicker(this.properties, this.properties.getWorkingDir())
						.groupId());
	}

	private String executeCommand(String command) {
		try {
			String projectRoot = this.properties.getWorkingDir();
			String[] commands = command.split(" ");
			return runCommand(projectRoot, commands);
		}
		catch (IllegalStateException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void build(ProjectVersion versionFromReleaseTrain, String projectRoot) {
		try {
			String[] commands = new CommandPicker(this.properties, projectRoot)
					.buildCommand(versionFromReleaseTrain).split(" ");
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
			String updatedCommand = new CommandPicker(properties, projectRoot)
					.generateReleaseTrainDocsCommand(
							new ProjectVersion(new File(projectRoot)))
					.replace(VERSION_MUSTACHE, version);
			runCommand(projectRoot, updatedCommand.split(" "));
			assertNoHtmlFilesInDocsContainUnresolvedTags(this.properties.getWorkingDir());
			log.info("No HTML files from docs contain unresolved tags");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
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

	public void deploy(ProjectVersion version) {
		doDeploy(new CommandPicker(properties, this.properties.getWorkingDir())
				.deployCommand(version));
	}

	public void deployGuides(ProjectVersion version) {
		doDeploy(new CommandPicker(properties, this.properties.getWorkingDir())
				.deployGuidesCommand(version));
	}

	private void doDeploy(String command) {
		try {
			String[] commands = command.split(" ");
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

	private String runCommand(String projectRoot, String[] commands) {
		String[] substitutedCommands = substituteSystemProps(commands);
		long waitTimeInMinutes = new CommandPicker(properties, projectRoot)
				.waitTimeInMinutes();
		return executor(projectRoot).runCommand(substitutedCommands, waitTimeInMinutes);
	}

	ProcessExecutor executor(String workDir) {
		return new ProcessExecutor(workDir);
	}

	public void publishDocs(String version) {
		try {
			for (String command : new CommandPicker(properties).publishDocsCommands()) {
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
		String systemProperties = new CommandPicker(this.properties).systemProperties();
		String systemPropertiesPlaceholder = new CommandPicker(this.properties)
				.systemPropertiesPlaceholder();
		boolean containsSystemProps = systemProperties.contains("-D");
		String[] splitSystemProps = StringUtils
				.delimitedListToStringArray(systemProperties, "-D");
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
			if (commands[i].contains(systemPropertiesPlaceholder)) {
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

	String runCommand(String[] commands, long waitTimeInMinutes) {
		try {
			String workingDir = this.workingDir;
			log.info(
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
			return convertStreamToString(process.getInputStream());
		}
		catch (InterruptedException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String convertStreamToString(InputStream is) {
		Scanner scanner = new Scanner(is).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}

	Process startProcess(ProcessBuilder builder) throws IOException {
		return builder.start();
	}

	ProcessBuilder builder(String[] commands, String workingDir) {
		// TODO: Improve this to not pass arrays in the first place
		String lastArg = String.join(" ", commands);
		String[] commandsWithBash = commandToExecute(lastArg);
		return new ProcessBuilder(commandsWithBash).directory(new File(workingDir))
				.inheritIO();
	}

	String[] commandToExecute(String lastArg) {
		return new String[] { "/bin/bash", "-c", lastArg };
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.workingDir = properties.getWorkingDir();
	}

}

class CommandPicker {

	private static final Log log = LogFactory.getLog(CommandPicker.class);

	private final ReleaserProperties releaserProperties;

	private final ProjectType projectType;

	CommandPicker(ReleaserProperties releaserProperties, String projectRoot) {
		this.releaserProperties = releaserProperties;
		this.projectType = guessProjectType(projectRoot);
	}

	CommandPicker(ReleaserProperties releaserProperties) {
		this.releaserProperties = releaserProperties;
		String projectRoot = releaserProperties.getWorkingDir();
		this.projectType = guessProjectType(projectRoot);
	}

	private ProjectType guessProjectType(String projectRoot) {
		if (new File(projectRoot, "pom.xml").exists()) {
			return ProjectType.MAVEN;
		}
		else if (new File(projectRoot, "build.gradle").exists()) {
			return ProjectType.GRADLE;
		}
		return ProjectType.BASH;
	}

	String systemProperties() {
		if (projectType == ProjectType.GRADLE) {
			return releaserProperties.getGradle().getSystemProperties();
		}
		else if (projectType == ProjectType.MAVEN) {
			return releaserProperties.getMaven().getSystemProperties();
		}
		return releaserProperties.getBash().getSystemProperties();
	}

	public String[] publishDocsCommands() {
		if (projectType == ProjectType.GRADLE) {
			return releaserProperties.getGradle().getPublishDocsCommands();
		}
		else if (projectType == ProjectType.MAVEN) {
			return releaserProperties.getMaven().getPublishDocsCommands();
		}
		return releaserProperties.getBash().getPublishDocsCommands();
	}

	String systemPropertiesPlaceholder() {
		if (projectType == ProjectType.GRADLE) {
			return ReleaserProperties.Gradle.SYSTEM_PROPS_PLACEHOLDER;
		}
		else if (projectType == ProjectType.MAVEN) {
			return ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER;
		}
		return ReleaserProperties.Bash.SYSTEM_PROPS_PLACEHOLDER;
	}

	String buildCommand(ProjectVersion version) {
		if (projectType == ProjectType.GRADLE) {
			return gradleCommandWithSystemProps(
					releaserProperties.getGradle().getBuildCommand());
		}
		else if (projectType == ProjectType.MAVEN) {
			return mavenCommandWithSystemProps(
					releaserProperties.getMaven().getBuildCommand(), version);
		}
		return bashCommandWithSystemProps(releaserProperties.getBash().getBuildCommand());
	}

	String version() {
		// makes more sense to use PomReader
		if (projectType == ProjectType.GRADLE) {
			return "./gradlew properties | grep version: | awk '{print $2}'";
		}
		return "./mvnw -q" + " -Dexec.executable=\"echo\""
				+ " -Dexec.args=\"\\${project.version}\"" + " --non-recursive"
				+ " org.codehaus.mojo:exec-maven-plugin:1.3.1:exec | tail -1";
	}

	String groupId() {
		// makes more sense to use PomReader
		if (projectType == ProjectType.GRADLE) {
			return "./gradlew groupId -q | tail -1";
		}
		return "./mvnw -q" + " -Dexec.executable=\"echo\""
				+ " -Dexec.args=\"\\${project.groupId}\"" + " --non-recursive"
				+ " org.codehaus.mojo:exec-maven-plugin:1.3.1:exec | tail -1";
	}

	String generateReleaseTrainDocsCommand(ProjectVersion version) {
		if (projectType == ProjectType.GRADLE) {
			return gradleCommandWithSystemProps(
					releaserProperties.getGradle().getGenerateReleaseTrainDocsCommand());
		}
		else if (projectType == ProjectType.MAVEN) {
			return mavenCommandWithSystemProps(
					releaserProperties.getMaven().getGenerateReleaseTrainDocsCommand(),
					version);
		}
		return bashCommandWithSystemProps(
				releaserProperties.getBash().getGenerateReleaseTrainDocsCommand());
	}

	String deployCommand(ProjectVersion version) {
		if (projectType == ProjectType.GRADLE) {
			return gradleCommandWithSystemProps(
					releaserProperties.getGradle().getDeployCommand());
		}
		else if (projectType == ProjectType.MAVEN) {
			return mavenCommandWithSystemProps(
					releaserProperties.getMaven().getDeployCommand(), version);
		}
		return bashCommandWithSystemProps(
				releaserProperties.getBash().getDeployCommand());
	}

	String deployGuidesCommand(ProjectVersion version) {
		if (projectType == ProjectType.GRADLE) {
			return gradleCommandWithSystemProps(
					releaserProperties.getGradle().getDeployGuidesCommand());
		}
		else if (projectType == ProjectType.MAVEN) {
			return mavenCommandWithSystemProps(
					releaserProperties.getMaven().getDeployGuidesCommand(), version,
					MavenProfile.GUIDES, MavenProfile.INTEGRATION);
		}
		return bashCommandWithSystemProps(
				releaserProperties.getBash().getDeployGuidesCommand());
	}

	long waitTimeInMinutes() {
		if (projectType == ProjectType.GRADLE) {
			return releaserProperties.getGradle().getWaitTimeInMinutes();
		}
		else if (projectType == ProjectType.MAVEN) {
			return releaserProperties.getMaven().getWaitTimeInMinutes();
		}
		return releaserProperties.getBash().getWaitTimeInMinutes();
	}

	private String gradleCommandWithSystemProps(String command) {
		if (command.contains(ReleaserProperties.Gradle.SYSTEM_PROPS_PLACEHOLDER)) {
			return command;
		}
		return command + " " + ReleaserProperties.Gradle.SYSTEM_PROPS_PLACEHOLDER;
	}

	private String mavenCommandWithSystemProps(String command, ProjectVersion version,
			MavenProfile... profiles) {
		if (command.contains(ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER)) {
			return appendMavenProfile(command, version, profiles);
		}
		return appendMavenProfile(command, version, profiles) + " "
				+ ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER;
	}

	private String bashCommandWithSystemProps(String command) {
		if (command.contains(ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER)) {
			return command;
		}
		return command + " " + ReleaserProperties.Maven.SYSTEM_PROPS_PLACEHOLDER;
	}

	private String appendMavenProfile(String command, ProjectVersion version,
			MavenProfile... profiles) {
		String trimmedCommand = command.trim();
		if (version.isMilestone() || version.isRc()) {
			log.info("Adding the milestone profile to the Maven build");
			return withProfile(trimmedCommand, MavenProfile.MILESTONE.asMavenProfile(),
					profiles);
		}
		else if (version.isRelease() || version.isServiceRelease()) {
			log.info("Adding the central profile to the Maven build");
			return withProfile(trimmedCommand, MavenProfile.CENTRAL.asMavenProfile(),
					profiles);
		}
		else {
			log.info("The build is a snapshot one - will not add any profiles");
		}
		return trimmedCommand;
	}

	private String withProfile(String command, String profile, MavenProfile... profiles) {
		if (command.contains(ReleaserProperties.Maven.PROFILE_PROPS_PLACEHOLDER)) {
			return command.replace(ReleaserProperties.Maven.PROFILE_PROPS_PLACEHOLDER,
					profile + appendProfiles(profiles));
		}
		return command + " " + profile + appendProfiles(profiles);
	}

	private String appendProfiles(MavenProfile[] profiles) {
		return profiles.length > 0 ? " " + profilesToString(profiles) : "";
	}

	private String profilesToString(MavenProfile... profiles) {
		return Arrays.stream(profiles).map(profile -> "-P" + profile)
				.collect(Collectors.joining(" "));
	}

	private enum ProjectType {

		MAVEN, GRADLE, BASH;

	}

	/**
	 * Enumeration over commonly used Maven profiles.
	 */
	private enum MavenProfile {

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
