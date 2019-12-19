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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.ReleaserPropertiesAware;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.Project;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

/**
 * @author Marcin Grzejszczak
 */
public class ProjectPomUpdater implements ReleaserPropertiesAware, Closeable {

	private static final List<String> IGNORED_SNAPSHOT_LINE_PATTERNS = Arrays.asList(
			"^.*replace=.*$",
			// issue [#80]
			"^[\\s]*<!--.*-->.*$");

	private static final Logger log = LoggerFactory.getLogger(ProjectPomUpdater.class);

	private static final boolean UPDATE_FIXED_VERSIONS = true;

	private static final Map<String, VersionsFromBom> CACHE = new ConcurrentHashMap<>();

	private final ProjectGitHandler gitRepo;

	private final PomUpdater pomUpdater = new PomUpdater();

	private final List<BomParser> bomParsers;

	private ReleaserProperties properties;

	public ProjectPomUpdater(ReleaserProperties properties, List<BomParser> bomParsers) {
		this.properties = properties;
		this.gitRepo = new ProjectGitHandler(properties);
		this.bomParsers = bomParsers;
	}

	/**
	 * For the given root folder (typically the working directory) retrieves list of
	 * versions for a given release version.
	 * @return projects retrieved from the release train bom
	 */
	public Projects retrieveVersionsFromReleaseTrainBom() {
		return retrieveVersionsFromReleaseTrainBom(this.properties.getPom().getBranch(),
				UPDATE_FIXED_VERSIONS);
	}

	/**
	 * For the given root folder (typically the working directory) retrieves list of
	 * versions for a given release version.
	 * @param branch branch for which to pick the versions
	 * @param updateFixedVersions whether should update the retrieved versions with fixed
	 * ones
	 * @return projects retrieved from the release train bom
	 */
	// TODO: I don't like this flag but don't have a better idea
	public Projects retrieveVersionsFromReleaseTrainBom(String branch,
			boolean updateFixedVersions) {
		VersionsFromBom versionsFromBom = cachedVersionFromBom(branch);
		if (updateFixedVersions) {
			log.info("Will update the following versions manually [{}]",
					this.properties.getFixedVersions());
			this.properties.getFixedVersions().forEach(versionsFromBom::setVersion);
		}
		log.info("Retrieved the following versions\n{}", versionsFromBom);
		return versionsFromBom.toProjectVersions();
	}

	private VersionsFromBom cachedVersionFromBom(String branch) {
		return CACHE.computeIfAbsent(branch, s -> {
			File clonedBom = this.gitRepo.cloneReleaseTrainProject();
			this.gitRepo.checkout(clonedBom, branch);
			return compositeBomParser().versionsFromBom(clonedBom);
		});
	}

	private CompositeBomParser compositeBomParser() {
		return new CompositeBomParser(this.bomParsers);
	}

	/**
	 * @return map of fixed versions
	 */
	public Projects fixedVersions() {
		Set<Project> projects = this.properties.getFixedVersions().entrySet().stream()
				.map(entry -> new Project(entry.getKey(), entry.getValue()))
				.collect(Collectors.toSet());
		if (log.isDebugEnabled()) {
			log.debug("Will apply the following fixed versions {}", projects);
		}
		return new VersionsFromBomBuilder().releaserProperties(this.properties)
				.parsers(compositeBomParser().customBomParsers()).projects(projects)
				.merged().toProjectVersions();
	}

	/**
	 * For the given root folder (typically the working directory) performs the whole flow
	 * of updating {@code pom.xml} with values from release train (e.g. Spring Cloud
	 * Release project.)
	 * @param projectRoot - root folder with project to update
	 * @param projects - versions of projects used to update poms
	 * @param versionFromReleaseTrain - version for the built project taken from release
	 * train (e.g. Spring Cloud Release project)
	 * @param assertVersions - should version assertion take place
	 */
	public void updateProjectFromReleaseTrain(File projectRoot, Projects projects,
			ProjectVersion versionFromReleaseTrain, boolean assertVersions) {
		VersionsFromBom versionsFromBom = new VersionsFromBomBuilder()
				.thisProjectRoot(projectRoot).releaserProperties(this.properties)
				.projects(projects.asProjects()).merged();
		if (!this.pomUpdater.shouldProjectBeUpdated(projectRoot, versionsFromBom)) {
			log.debug("Skipping project updating");
			return;
		}
		updatePoms(projectRoot, versionsFromBom, versionFromReleaseTrain, assertVersions);
	}

	private void updatePoms(File projectRoot, VersionsFromBom projects,
			ProjectVersion versionFromScRelease, boolean assertVersions) {
		File rootPom = new File(projectRoot, "pom.xml");
		if (!rootPom.exists()) {
			log.info("No pom.xml present, skipping!");
			return;
		}
		ModelWrapper rootPomModel = this.pomUpdater.readModel(rootPom);
		processAllPoms(projectRoot, new PomWalker(rootPomModel, projects, this.pomUpdater,
				this.properties, versionFromScRelease, assertVersions));
	}

	private void processAllPoms(File projectRoot, PomWalker pomWalker) {
		try {
			Files.walkFileTree(projectRoot.toPath(), pomWalker);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

	@Override
	public void close() throws IOException {
		CACHE.clear();
	}

	private final class PomWalker extends SimpleFileVisitor<Path> {

		private static final String POM_XML = "pom.xml";

		private final ModelWrapper rootPom;

		private final VersionsFromBom versionsFromBom;

		private final PomUpdater pomUpdater;

		private final ReleaserProperties properties;

		private final boolean skipVersionAssert;

		private final boolean assertVersions;

		private final List<Pattern> unacceptableVersionPatterns;

		private PomWalker(ModelWrapper rootPom, VersionsFromBom projects,
				PomUpdater pomUpdater, ReleaserProperties properties,
				ProjectVersion versionFromScRelease, boolean assertVersions) {
			this.rootPom = rootPom;
			this.versionsFromBom = projects;
			this.pomUpdater = pomUpdater;
			this.properties = properties;
			List<Pattern> unacceptableVersionPatterns = versionFromScRelease
					.unacceptableVersionPatterns();
			this.unacceptableVersionPatterns = unacceptableVersionPatterns;
			this.skipVersionAssert = !assertVersions
					|| unacceptableVersionPatterns.isEmpty();
			this.assertVersions = assertVersions;
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
			File file = path.toFile();
			if (POM_XML.equals(file.getName())) {
				if (pathIgnored(file)) {
					log.debug(
							"Ignoring file [{}] since it's on a list of patterns to ignore",
							file);
					return FileVisitResult.CONTINUE;
				}
				ModelWrapper model = this.pomUpdater.updateModel(this.rootPom, file,
						this.versionsFromBom);
				this.pomUpdater.overwritePomIfDirty(model, this.versionsFromBom, file);
				if (this.assertVersions && !this.skipVersionAssert
						&& !this.pomUpdater.hasSkipDeployment(model.model)) {
					log.debug(
							"Update is a non-snapshot one. Checking if no snapshot versions remained in the pom");
					String text = asString(path);
					Scanner scanner = new Scanner(text);
					int lineNumber = 0;
					while (scanner.hasNextLine()) {
						String line = scanner.nextLine();
						lineNumber++;
						Pattern matchingPattern = this.unacceptableVersionPatterns
								.stream()
								.filter(pattern -> IGNORED_SNAPSHOT_LINE_PATTERNS.stream()
										.noneMatch(line::matches)
										&& pattern.matcher(line).matches())
								.findFirst().orElse(null);
						if (matchingPattern != null) {
							if (log.isDebugEnabled()) {
								log.debug("File text \n" + text);
							}
							throw new IllegalStateException("The file [" + path
									+ "] matches the [ " + matchingPattern.pattern()
									+ "] pattern in line number [" + lineNumber + "]\n\n"
									+ line);
						}
					}
					log.info("No invalid versions remained in the pom");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		private boolean pathIgnored(File file) {
			String path = file.getPath();
			return this.assertVersions && this.properties.getPom().getIgnoredPomRegex()
					.stream().anyMatch(path::matches);
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
