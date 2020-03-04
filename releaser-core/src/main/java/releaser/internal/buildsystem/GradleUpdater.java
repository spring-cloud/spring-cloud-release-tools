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

package releaser.internal.buildsystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class GradleUpdater {

	private static final Logger log = LoggerFactory.getLogger(GradleUpdater.class);

	/**
	 * For the given root folder (typically the working directory) performs the whole flow
	 * of updating {@code gradle.properties} with values from BOM project. Remember to
	 * pass the mapping from a property name inside {@code gradle.properties} to the
	 * project name via {@link ReleaserProperties.Gradle#getGradlePropsSubstitution}
	 * @param projectRoot - root folder with project to update
	 * @param projects - versions of projects used to update poms
	 * @param versionFromBom - version for the project from Spring Cloud Release
	 * @param assertVersions - should snapshots / milestone / rc presence be asserted
	 */
	public void updateProjectFromReleaseTrain(ReleaserProperties properties,
			File projectRoot, Projects projects, ProjectVersion versionFromBom,
			boolean assertVersions) {
		processAllGradleProps(properties, projectRoot, projects, versionFromBom,
				assertVersions);
	}

	private void processAllGradleProps(ReleaserProperties properties, File projectRoot,
			Projects projects, ProjectVersion versionFromBom, boolean assertVersions) {
		try {
			Files.walkFileTree(projectRoot.toPath(), new GradlePropertiesWalker(
					properties, projects, versionFromBom, assertVersions));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private final class GradlePropertiesWalker extends SimpleFileVisitor<Path> {

		private static final String GRADLE_PROPERTIES = "gradle.properties";

		private final ReleaserProperties properties;

		private final Projects projects;

		private final boolean skipVersionAssert;

		private final boolean assertVersions;

		private final List<Pattern> unacceptableVersionPatterns;

		private final GradleProjectNameExtractor extractor = new GradleProjectNameExtractor();

		private GradlePropertiesWalker(ReleaserProperties properties, Projects projects,
				ProjectVersion versionFromBom, boolean assertVersions) {
			this.properties = properties;
			this.projects = projects;
			List<Pattern> unacceptableVersionPatterns = versionFromBom
					.unacceptableVersionPatterns();
			this.unacceptableVersionPatterns = unacceptableVersionPatterns;
			this.skipVersionAssert = !assertVersions
					|| unacceptableVersionPatterns.isEmpty();
			this.assertVersions = assertVersions;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
			File file = path.toFile();
			if (GRADLE_PROPERTIES.equals(file.getName())) {
				if (pathIgnored(file)) {
					log.debug(
							"Ignoring file [{}] since it's on a list of patterns to ignore",
							file);
					return FileVisitResult.CONTINUE;
				}
				String parentName = file.getParentFile().getName();
				log.info("Will process the file [{}] and update its gradle properties",
						file);
				final String fileContents = asString(path);
				final AtomicReference<String> changedString = new AtomicReference<>(
						fileContents);
				Properties props = loadProps(file);
				final Map<String, String> substitution = this.properties.getGradle()
						.getGradlePropsSubstitution();
				props.forEach((key, value1) -> {
					String projectName = projectName(parentName, substitution, key);
					if (!this.projects.containsProject(projectName)) {
						log.warn(
								"Should update project with name [{}] but it wasn't found in the list of projects [{}]",
								projectName, this.projects.asList());
						return;
					}
					ProjectVersion value = this.projects.forName(projectName);
					if (!value.version.equalsIgnoreCase(value1.toString())) {
						log.info("Replacing [{}->{}] with [{}->{}]", key, value1, key,
								value);
						changedString.set(changedString.get().replace(key + "=" + value1,
								key + "=" + value));
					}
				});
				storeString(path, changedString.get());
				assertNoSnapshotsArePresent(path);
			}
			return FileVisitResult.CONTINUE;
		}

		private String projectName(String parentName, Map<String, String> substitution,
				Object key) {
			// version -> current project version
			if (key.equals("version")) {
				return parentName;
			}
			return this.extractor.projectName(substitution, key);
		}

		private void assertNoSnapshotsArePresent(Path path) {
			if (this.assertVersions && !this.skipVersionAssert) {
				log.debug(
						"Update should check if no wrong versions remained in the gradle prop. List of wrong patterns: {}",
						this.unacceptableVersionPatterns.stream().map(Pattern::pattern)
								.collect(Collectors.toList()));
				Scanner scanner = new Scanner(asString(path));
				int lineNumber = 0;
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					lineNumber++;
					Pattern matchingPattern = this.unacceptableVersionPatterns.stream()
							.filter(pattern -> pattern.matcher(line).matches())
							.findFirst().orElse(null);
					if (matchingPattern != null) {
						throw new IllegalStateException("The file [" + path
								+ "] matches the [ " + matchingPattern.pattern()
								+ "] pattern in line number [" + lineNumber + "]\n\n"
								+ line);
					}
				}
				log.info("No invalid versions remained in the gradle properties");
			}
		}

		private boolean pathIgnored(File file) {
			String path = file.getPath();
			return this.assertVersions && this.properties.getGradle()
					.getIgnoredGradleRegex().stream().anyMatch(path::matches);
		}

		private Properties loadProps(File file) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(file));
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return props;
		}

		private String asString(Path path) {
			try {
				return new String(Files.readAllBytes(path));
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		private void storeString(Path path, String content) {
			try {
				Files.write(path, content.getBytes());
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
