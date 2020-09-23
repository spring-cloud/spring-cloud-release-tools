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

package releaser.internal.spring;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.Releaser;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;

import org.springframework.util.StringUtils;

class VersionsToBumpFactory implements Closeable {

	private static final Logger log = LoggerFactory
			.getLogger(VersionsToBumpFactory.class);

	private static final Map<File, ProjectsFromBom> CACHE = new ConcurrentHashMap<>();

	private final Releaser releaser;

	private final ReleaserProperties properties;

	VersionsToBumpFactory(Releaser releaser, ReleaserProperties properties) {
		this.releaser = releaser;
		this.properties = properties;
	}

	ProjectsFromBom withProject(File project) {
		return CACHE.computeIfAbsent(project, file -> {
			log.info("Fetch from git [{}], meta release [{}], project [{}]",
					this.properties.getGit().isFetchVersionsFromGit(),
					this.properties.getMetaRelease().isEnabled(), project);
			if (this.properties.getGit().isFetchVersionsFromGit()
					&& !this.properties.getMetaRelease().isEnabled()) {
				return fetchVersionsFromGitForSingleProject(project);
			}
			return fetchVersionsFromFixedProjects(project);
		});
	}

	private ProjectsFromBom fetchVersionsFromFixedProjects(File project) {
		ProjectVersion originalVersion = new ProjectVersion(project);
		Projects fixedVersions = this.releaser.fixedVersions();
		log.debug("Got the following fixed versions [{}]", fixedVersions);
		String fixedVersionForProject = fixedVersions.containsProject(project.getName())
				? fixedVersions.forName(project.getName()).version : "";
		log.debug("Found fixed version for this project is [{}]", fixedVersionForProject);
		ProjectVersion versionFromBom = StringUtils.hasText(fixedVersionForProject)
				? new ProjectVersion(originalVersion.projectName, fixedVersionForProject)
				: new ProjectVersion(project);
		fixedVersions.add(versionFromBom);
		printSettingVersionFromFixedVersions(fixedVersions);
		return cachedProjectsFromBom(versionFromBom, fixedVersions);
	}

	private ProjectsFromBom fetchVersionsFromGitForSingleProject(File project) {
		printVersionRetrieval();
		Projects projectsToUpdate = this.releaser.retrieveVersionsFromBom();
		ProjectVersion versionFromBom = assertNoSnapshotsForANonSnapshotProject(project,
				projectsToUpdate);
		return cachedProjectsFromBom(versionFromBom, projectsToUpdate);
	}

	private ProjectsFromBom cachedProjectsFromBom(ProjectVersion versionFromBom, Projects projectsToUpdate) {
		return new ProjectsFromBom(projectsToUpdate, versionFromBom);
	}

	ProjectVersion assertNoSnapshotsForANonSnapshotProject(File project,
			Projects projectsToUpdate) {
		ProjectVersion versionFromBom;
		versionFromBom = projectsToUpdate.forFile(project);
		assertNoSnapshotsForANonSnapshotProject(projectsToUpdate, versionFromBom);
		return versionFromBom;
	}

	private void assertNoSnapshotsForANonSnapshotProject(Projects projects,
			ProjectVersion versionFromBom) {
		if (!versionFromBom.isSnapshot() && projects.containsSnapshots()) {
			throw new IllegalStateException("You are trying to release a non snapshot "
					+ "version [" + versionFromBom + "] of the project ["
					+ versionFromBom.projectName + "] but "
					+ "there is at least one SNAPSHOT library version in the Spring Cloud Release project");
		}
	}

	private void printVersionRetrieval() {
		log.info("\n\n\n=== RETRIEVING VERSIONS ===\n\nWill clone the BOM project"
				+ " to retrieve all versions");
	}

	private void printSettingVersionFromFixedVersions(Projects projectsToUpdate) {
		log.info(
				"\n\n\n=== RETRIEVED VERSIONS ===\n\nWill use the fixed versions"
						+ " of projects\n\n{}",
				projectsToUpdate.stream().map(p -> p.projectName + " => " + p.version)
						.collect(Collectors.joining("\n")));
	}

	@Override
	public void close() throws IOException {
		CACHE.clear();
	}

}
