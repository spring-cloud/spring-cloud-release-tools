/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.cloud.release.internal.pom;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectPomUpdater implements ReleaserPropertiesAware {

	private static final List<String> IGNORED_SNAPSHOT_LINE_PATTERNS = Arrays.asList(
			"^.*replace=.*$",
			// issue [#80]
			"^[\\s]*<!--.*-->.*$"
	);

	private static final Logger log = LoggerFactory.getLogger(ProjectPomUpdater.class);

	private ReleaserProperties properties;
	private final ProjectGitHandler gitRepo;
	private final PomUpdater pomUpdater = new PomUpdater();

	public ProjectPomUpdater(ReleaserProperties properties) {
		this.properties = properties;
		this.gitRepo = new ProjectGitHandler(properties);
	}

	/**
	 * For the given root folder (typically the working directory) retrieves list of versions
	 * for a given release version.
	 */
	public Projects retrieveVersionsFromReleaseTrainBom() {
		File clonedScRelease = this.gitRepo.cloneReleaseTrainProject();
		this.gitRepo.checkout(clonedScRelease, this.properties.getPom().getBranch());
		BomParser sCReleasePomParser = new BomParser(this.properties, clonedScRelease);
		Versions versions = sCReleasePomParser.allVersions();
		log.info("Will update the following versions manually [{}]", this.properties.getFixedVersions());
		this.properties.getFixedVersions().forEach(versions::setVersion);
		log.info("Retrieved the following versions\n{}", versions);
		return versions.toProjectVersions();
	}

	/**
	 * @return map of fixed versions
	 */
	public Projects fixedVersions() {
		Set<ProjectVersion> projectVersions = this.properties.getFixedVersions()
				.entrySet()
				.stream()
				.map(entry -> new ProjectVersion(entry.getKey(), entry.getValue()))
				.collect(Collectors.toSet());
		if (log.isDebugEnabled()) {
			log.debug("Will apply the following fixed versions {}", projectVersions);
		}
		return new Versions(projectVersions).toProjectVersions();
	}

	/**
	 * For the given root folder (typically the working directory) performs the whole
	 * flow of updating {@code pom.xml} with values from Spring Cloud Release project.
	 * @param projectRoot - root folder with project to update
	 * @param projects - versions of projects used to update poms
	 * @param versionFromScRelease - version for the built project taken from Spring Cloud Release project
	 * @param assertSnapshots - should snapshots present be asserted
	 */
	public void updateProjectFromSCRelease(File projectRoot, Projects projects,
			ProjectVersion versionFromScRelease, boolean assertSnapshots) {
		Versions versions = new Versions(projects);
		if (!this.pomUpdater.shouldProjectBeUpdated(projectRoot, versions)) {
			log.info("Skipping project updating");
			return;
		}
		updatePoms(projectRoot, projects, versionFromScRelease, assertSnapshots);
	}

	private void updatePoms(File projectRoot, Projects projects,
			ProjectVersion versionFromScRelease, boolean assertSnapshots) {
		File rootPom = new File(projectRoot, "pom.xml");
		ModelWrapper rootPomModel = this.pomUpdater.readModel(rootPom);
		processAllPoms(projectRoot, new PomWalker(rootPomModel, projects, this.pomUpdater,
				this.properties, versionFromScRelease, assertSnapshots));
	}

	private void processAllPoms(File projectRoot, PomWalker pomWalker) {
		try {
			Files.walkFileTree(projectRoot.toPath(), pomWalker);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

	private class PomWalker extends SimpleFileVisitor<Path> {

		private static final String POM_XML = "pom.xml";

		private final ModelWrapper rootPom;
		private final Versions versions;
		private final PomUpdater pomUpdater;
		private final ReleaserProperties properties;
		private final boolean snapshotVersion;
		private final boolean assertSnapshots;

		private PomWalker(ModelWrapper rootPom, Projects projects, PomUpdater pomUpdater,
				ReleaserProperties properties, ProjectVersion versionFromScRelease,
				boolean assertSnapshots) {
			this.rootPom = rootPom;
			this.versions = new Versions(projects);
			this.pomUpdater = pomUpdater;
			this.properties = properties;
			this.snapshotVersion = !assertSnapshots || versionFromScRelease.isSnapshot();
			this.assertSnapshots = assertSnapshots;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
			File file = path.toFile();
			if (POM_XML.equals(file.getName())) {
				if (pathIgnored(file)) {
					log.debug("Ignoring file [{}] since it's on a list of patterns to ignore", file);
					return FileVisitResult.CONTINUE;
				}
				ModelWrapper model = this.pomUpdater.updateModel(this.rootPom, file, this.versions);
				this.pomUpdater.overwritePomIfDirty(model, this.versions, file);
				if (this.assertSnapshots && !this.snapshotVersion && !this.pomUpdater.hasSkipDeployment(model.model)) {
					log.debug("Update is a non-snapshot one. Checking if no snapshot versions remained in the pom");
					Scanner scanner = new Scanner(asString(path));
					int lineNumber = 0;
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						lineNumber++;
						boolean containsSnapshot = line.contains("SNAPSHOT") &&
								IGNORED_SNAPSHOT_LINE_PATTERNS.stream().noneMatch(line::matches);
						if (containsSnapshot) {
							throw new IllegalStateException("The file [" + path + "] contains a SNAPSHOT "
									+ "version for a non snapshot release in line number [" + lineNumber + "]\n\n" + line);
						}
					}
					log.info("No snapshot versions remained in the pom");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		private boolean pathIgnored(File file) {
			String path = file.getPath();
			return this.assertSnapshots &&
					this.properties.getPom().getIgnoredPomRegex().stream().anyMatch(path::matches);
		}

		private String asString(Path path) {
			try {
				return new String(Files.readAllBytes(path));
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}


