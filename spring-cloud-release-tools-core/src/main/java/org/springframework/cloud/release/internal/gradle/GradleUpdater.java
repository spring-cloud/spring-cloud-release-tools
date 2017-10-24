package org.springframework.cloud.release.internal.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class GradleUpdater {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ReleaserProperties properties;

	public GradleUpdater(ReleaserProperties properties) {
		this.properties = properties;
	}

	/**
	 * For the given root folder (typically the working directory) performs the whole
	 * flow of updating {@code gradle.properties} with values from Spring Cloud Release project.
	 * Remember to pass the mapping from a property name inside {@code gradle.properties} to
	 * the project name via {@link ReleaserProperties.Gradle#gradlePropsSubstitution}
	 * @param projectRoot - root folder with project to update
	 * @param projects - versions of projects used to update poms
	 * @param versionFromScRelease - version for the project from Spring Cloud Release
	 */
	public void updateProjectFromSCRelease(File projectRoot, Projects projects,
			ProjectVersion versionFromScRelease) {
		processAllGradleProps(projectRoot, projects, versionFromScRelease);
	}

	private void processAllGradleProps(File projectRoot, Projects projects,
			ProjectVersion versionFromScRelease) {
		try {
			Files.walkFileTree(projectRoot.toPath(), new GradlePropertiesWalker(this.properties, projects, versionFromScRelease));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private class GradlePropertiesWalker extends SimpleFileVisitor<Path> {

		private static final String GRADLE_PROPERTIES = "gradle.properties";

		private final ReleaserProperties properties;
		private final Projects projects;
		private final ProjectVersion versionFromScRelease;

		private GradlePropertiesWalker(ReleaserProperties properties, Projects projects,
				ProjectVersion versionFromScRelease) {
			this.properties = properties;
			this.projects = projects;
			this.versionFromScRelease = versionFromScRelease;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
			File file = path.toFile();
			if (GRADLE_PROPERTIES.equals(file.getName())) {
				if (pathIgnored(file)) {
					log.debug("Ignoring file [{}] since it's on a list of patterns to ignore", file);
					return FileVisitResult.CONTINUE;
				}
				log.info("Will process the file [{}] and update its gradle properties", file);
				final String fileContents = asString(path);
				final AtomicReference<String> changedString = new AtomicReference<>(fileContents);
				Properties props = loadProps(file);
				final Map<String, String> substitution = this.properties.getGradle()
						.getGradlePropsSubstitution();
				props.entrySet().stream().forEach(entry -> {
					if (substitution.containsKey(entry.getKey())) {
						Object projectName = substitution.get(entry.getKey());
						ProjectVersion value = this.projects.forName((String) projectName);
						log.info("Replacing [{}->{}] with [{}->{}]", entry.getKey(), entry.getValue(), entry.getKey(), value);
						changedString.set(changedString.get().replace(entry.getKey() + "=" + entry.getValue(),
								entry.getKey() + "=" + value));
					}
				});
				storeString(path, changedString.get());
			}
			return FileVisitResult.CONTINUE;
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

		private boolean pathIgnored(File file) {
			String path = file.getPath();
			return bumpingToRelease() &&
					this.properties.getGradle().getIgnoredGradleRegex().stream().anyMatch(path::matches);
		}

		private boolean bumpingToRelease() {
			ProjectVersion version = this.projects
					.forName(this.versionFromScRelease.projectName);
			return version.isRelease() || version.isServiceRelease();
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
