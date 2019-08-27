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

package org.springframework.cloud.release.internal.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.buildsystem.ProjectVersion;
import org.springframework.cloud.release.internal.git.ProjectGitHandler;
import org.springframework.cloud.release.internal.project.Projects;
import org.springframework.cloud.release.internal.tech.HandlebarsHelper;
import org.springframework.cloud.release.internal.template.TemplateGenerator;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
// TODO: [SPRING-CLOUD]
class ReleaseTrainContentsUpdater implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory
			.getLogger(ReleaseTrainContentsUpdater.class);

	private final ReleaseTrainContentsGitHandler handler;

	private final ReleaseTrainContentsParser parser;

	private final ReleaseTrainContentsGenerator generator;

	private final TemplateGenerator templateGenerator;

	private ReleaserProperties properties;

	ReleaseTrainContentsUpdater(ReleaserProperties properties, ProjectGitHandler handler,
			TemplateGenerator templateGenerator) {
		this.properties = properties;
		this.handler = new ReleaseTrainContentsGitHandler(handler);
		this.templateGenerator = templateGenerator;
		this.parser = new ReleaseTrainContentsParser();
		this.generator = new ReleaseTrainContentsGenerator(properties);
	}

	/**
	 * Updates the project page if current release train version is greater or equal than
	 * the one stored in the repo.
	 * @param projects projects to update project repo for
	 * @return {@link File cloned temporary directory} - {@code null} if wrong version is
	 * used or the switch is turned off
	 * @deprecated - index.html doesn't look like this anymore
	 */
	@Deprecated
	File updateProjectRepo(Projects projects) {
		if (!this.properties.getGit().isUpdateSpringProject()) {
			log.info("Will not update the Spring Project cause "
					+ "the switch is turned off. Set [releaser.git.update-spring-project=true].");
			return null;
		}
		File releaseTrainProject = this.handler.cloneSpringDocProject();
		File index = new File(releaseTrainProject, "index.html");
		ReleaseTrainContents contents = this.parser.parseProjectPage(index);
		if (contents == null) {
			log.warn(
					"There are no markers for the index.html page - I don't really know what to do, so I'll back away");
			return null;
		}
		String newContents = this.generator.releaseTrainContents(contents, projects);
		if (StringUtils.isEmpty(newContents)) {
			log.info("No changes to commit to the Spring Project page.");
			return releaseTrainProject;
		}
		return pushNewContents(projects, releaseTrainProject, index, newContents);
	}

	private File pushNewContents(Projects projects, File releaseTrainProject, File index,
			String newContents) {
		try {
			log.debug("Storing new contents to the page");
			Files.write(index.toPath(), newContents.getBytes());
			log.info("Successfully stored new contents of the page");
			this.handler.commitAndPushChanges(releaseTrainProject,
					this.generator.currentReleaseTrainProject(projects));
			return releaseTrainProject;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Clones the test project, updates it and runs tests.
	 * @param projects - set of project with versions to assert against
	 */
	File updateReleaseTrainWiki(Projects projects) {
		if (!this.properties.getGit().isUpdateReleaseTrainWiki()
				|| !this.properties.getMetaRelease().isEnabled()) {
			log.info(
					"Will not clone and update the release train wiki, since the switch to do so "
							+ "is off or it's not a meta-release. Set [releaser.git.update-release-train-wiki] to [true] to change that");
			return null;
		}
		File releaseTrainWiki = this.handler.cloneReleaseTrainWiki();
		ProjectVersion releaseTrain = projects.releaseTrain(this.properties);
		String releaseTrainName = releaseTrain.major();
		String wikiPagePrefix = this.properties.getGit().getReleaseTrainWikiPagePrefix();
		String releaseTrainDocFileName = releaseTrainDocFileName(releaseTrainName,
				wikiPagePrefix);
		log.info("Reading the file [{}] for the current release train",
				releaseTrainDocFileName);
		File releaseTrainDocFile = releaseTrainDocFile(releaseTrainWiki,
				releaseTrainDocFileName);
		String releaseVersionFromCurrentFile = this.parser
				.latestReleaseTrainFromWiki(releaseTrainDocFile);
		log.info("Latest release train version in the file is [{}]",
				releaseVersionFromCurrentFile);
		if (!isThisReleaseTrainVersionNewer(releaseTrain,
				releaseVersionFromCurrentFile)) {
			log.info(
					"Current release train version [{}] is not "
							+ "newer than the version taken from the wiki [{}]",
					releaseTrain.version, releaseVersionFromCurrentFile);
			return releaseTrainWiki;
		}
		return generateNewWikiEntry(projects, releaseTrainWiki, releaseTrain,
				releaseTrainName, releaseTrainDocFile, releaseVersionFromCurrentFile);
	}

	private File generateNewWikiEntry(Projects projects, File releaseTrainWiki,
			ProjectVersion releaseTrain, String releaseTrainName,
			File releaseTrainDocFile, String releaseVersionFromCurrentFile) {
		File releaseNotes = this.templateGenerator.releaseNotes(projects);
		try {
			List<String> lines = Files.readAllLines(releaseTrainDocFile.toPath());
			int lineIndex;
			for (lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
				if (lines.get(lineIndex).contains(releaseVersionFromCurrentFile)) {
					break;
				}
			}
			return insertNewWikiContentBeforeTheLatestRelease(releaseTrainWiki,
					releaseTrain, releaseTrainName, releaseTrainDocFile, releaseNotes,
					lines, lineIndex);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private File insertNewWikiContentBeforeTheLatestRelease(File releaseTrainWiki,
			ProjectVersion releaseTrain, String releaseTrainName,
			File releaseTrainDocFile, File releaseNotes, List<String> lines,
			int lineIndex) throws IOException {
		String newContent = new StringJoiner("\n")
				.add(String.join("\n", lines.subList(0, lineIndex))).add("\n")
				.add(new String(Files.readAllBytes(releaseNotes.toPath())))
				.add(String.join("\n", lines.subList(lineIndex, lines.size())))
				.toString();
		Files.write(releaseTrainDocFile.toPath(), newContent.getBytes());
		log.info("Successfully stored new wiki contents for release train [{}]",
				releaseTrainName);
		this.handler.commitAndPushChanges(releaseTrainWiki, releaseTrain);
		return releaseTrainWiki;
	}

	private String releaseTrainDocFileName(String releaseTrainName,
			String wikiPagePrefix) {
		return new StringJoiner("-").add(wikiPagePrefix).add(releaseTrainName)
				.add("Release-Notes.md").toString();
	}

	private boolean isThisReleaseTrainVersionNewer(ProjectVersion releaseTrain,
			String releaseVersionFromCurrentFile) {
		if (StringUtils.hasText(releaseVersionFromCurrentFile)) {
			return releaseTrain
					.compareToReleaseTrainName(releaseVersionFromCurrentFile) > 0;
		}
		return true;
	}

	private File releaseTrainDocFile(File releaseTrainWiki,
			String releaseTrainDocFileName) {
		File releaseTrainDocFile = new File(releaseTrainWiki, releaseTrainDocFileName);
		if (!releaseTrainDocFile.exists()) {
			try {
				if (!releaseTrainDocFile.createNewFile()) {
					throw new IllegalStateException(
							"Failed to create releae train doc file");
				}
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return releaseTrainDocFile;
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
		this.generator.setReleaserProperties(properties);
	}

}

/**
 * @author Marcin Grzejszczak
 */
class ReleaseTrainContentsGenerator implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory
			.getLogger(ReleaseTrainContentsGenerator.class);

	private static final String SPRING_PROJECT_TEMPLATE = "spring-project";

	private final File projectOutput;

	private ReleaserProperties properties;

	ReleaseTrainContentsGenerator(ReleaserProperties properties) {
		this.properties = properties;
		this.projectOutput = new File("target/index.html");
	}

	String releaseTrainContents(ReleaseTrainContents currentContents, Projects projects) {
		String trainProject = this.properties.getMetaRelease()
				.getReleaseTrainProjectName();
		ProjectVersion currentReleaseTrainProject = currentReleaseTrainProject(projects);
		ProjectVersion lastGa = new ProjectVersion(trainProject,
				currentContents.title.lastGaTrainName);
		ProjectVersion currentGa = new ProjectVersion(trainProject,
				currentContents.title.currentGaTrainName);
		ReleaseTrainContents newReleaseTrainContents = updateReleaseTrainContentsIfNecessary(
				currentContents, projects, currentReleaseTrainProject, lastGa, currentGa);
		if (!currentContents.equals(newReleaseTrainContents)) {
			Template template = HandlebarsHelper.template(
					this.properties.getTemplate().getTemplateFolder(),
					SPRING_PROJECT_TEMPLATE);
			return generate(this.projectOutput, template, newReleaseTrainContents);
		}
		log.warn("Current release train [{}] is neither last [{}] "
				+ "or current [{}] or the projects haven't changed. Will not update the contents",
				currentReleaseTrainProject.version, lastGa, currentGa);
		return "";
	}

	ProjectVersion currentReleaseTrainProject(Projects projects) {
		return projects.releaseTrain(this.properties);
	}

	private String generate(File contentOutput, Template template,
			ReleaseTrainContents releaseTrainContents) {
		try {
			Map<String, Object> map = ImmutableMap.<String, Object>builder()
					.put("lastGaTrainName", releaseTrainContents.title.lastGaTrainName)
					.put("currentGaTrainName",
							releaseTrainContents.title.currentGaTrainName)
					.put("currentSnapshotTrainName",
							releaseTrainContents.title.currentSnapshotTrainName)
					.put("projects", releaseTrainContents.rows).build();
			String contents = template.apply(map);
			Files.write(contentOutput.toPath(), contents.getBytes());
			return contents;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private ReleaseTrainContents updateReleaseTrainContentsIfNecessary(
			ReleaseTrainContents currentContents, Projects projects,
			ProjectVersion currentReleaseTrainProject, ProjectVersion lastGa,
			ProjectVersion currentGa) {
		ReleaseTrainContents newReleaseTrainContents = currentContents;
		// current GA is greater than the last GA
		if (greaterMinorOfLastGaReleaseTrain(currentReleaseTrainProject, lastGa)) {
			Title title = new Title(currentReleaseTrainProject.version,
					currentContents.title.currentGaTrainName,
					currentContents.title.currentSnapshotTrainName);
			return updatedReleaseTrainContents(currentContents, projects, title, true);
		}
		else if (currentReleaseTrainProject.isSameReleaseTrainName(currentGa.version)) {
			Title title = new Title(currentContents.title.lastGaTrainName,
					currentReleaseTrainProject.isReleaseOrServiceRelease()
							? currentReleaseTrainProject.version
							: currentContents.title.currentGaTrainName,
					currentReleaseTrainProject.isSnapshot()
							? currentReleaseTrainProject.version
							: currentContents.title.currentSnapshotTrainName);
			return updatedReleaseTrainContents(currentContents, projects, title, false);
		}
		return newReleaseTrainContents;
	}

	private boolean greaterMinorOfLastGaReleaseTrain(
			ProjectVersion currentReleaseTrainProject, ProjectVersion lastGa) {
		return currentReleaseTrainProject.isSameReleaseTrainName(lastGa.version)
				&& currentReleaseTrainProject.isReleaseOrServiceRelease()
				&& currentReleaseTrainProject
						.compareToReleaseTrainName(lastGa.version) > 0;
	}

	private ReleaseTrainContents updatedReleaseTrainContents(
			ReleaseTrainContents currentContents, Projects projects, Title title,
			boolean lastGa) {
		List<Row> rows = Row.fromProjects(projects, lastGa);
		return new ReleaseTrainContents(title,
				currentContents.rows.stream().map(current -> {
					Row projectRow = rows.stream().filter(
							row -> current.componentName.equals(row.componentName))
							.findFirst().orElse(current);
					if (projectRow == current) {
						return projectRow;
					}
					return from(current, projectRow);
				}).collect(Collectors.toCollection(LinkedList::new)));
	}

	private Row from(Row current, Row project) {
		return new Row(current.componentName,
				StringUtils.hasText(project.lastGaVersion) ? project.lastGaVersion
						: current.lastGaVersion,
				StringUtils.hasText(project.currentGaVersion) ? project.currentGaVersion
						: current.currentGaVersion,
				StringUtils.hasText(project.currentSnapshotVersion)
						? project.currentSnapshotVersion
						: current.currentSnapshotVersion);
	}

	@Override
	public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}

}

class ReleaseTrainContentsGitHandler {

	private static final Logger log = LoggerFactory
			.getLogger(ReleaseTrainContentsGitHandler.class);

	private static final String PROJECT_PAGE_UPDATED_COMMIT_MSG = "Updating project page to release train [%s]";

	private final ProjectGitHandler handler;

	ReleaseTrainContentsGitHandler(ProjectGitHandler handler) {
		this.handler = handler;
	}

	File cloneSpringDocProject() {
		return this.handler.cloneSpringDocProject();
	}

	File cloneReleaseTrainWiki() {
		return this.handler.cloneReleaseTrainWiki();
	}

	void commitAndPushChanges(File repo, ProjectVersion releaseTrain) {
		log.debug("Committing and pushing changes");
		this.handler.commit(repo,
				String.format(PROJECT_PAGE_UPDATED_COMMIT_MSG, releaseTrain.version));
		this.handler.pushCurrentBranch(repo);
	}

}

class ReleaseTrainContentsParser {

	private static final Logger log = LoggerFactory
			.getLogger(ReleaseTrainContentsParser.class);

	ReleaseTrainContents parseProjectPage(File rawHtml) {
		try {
			String contents = new String(Files.readAllBytes(rawHtml.toPath()));
			String[] split = contents.split("<!-- (BEGIN|END) COMPONENTS -->");
			if (split.length != 3) {
				log.warn("The page is missing the components table markers. "
						+ "Please add [<!-- BEGIN COMPONENTS -->] and [<!-- END COMPONENTS -->] to the file.");
				return null;
			}
			String table = split[1];
			String[] components = table.trim().split("\n");
			String[] titleRow = components[0].trim().split("\\|");
			Title title = new Title(titleRow);
			List<Row> rows = new LinkedList<>();
			for (int i = 2; i < components.length; i++) {
				String[] splitRow = components[i].split("\\|");
				rows.add(new Row(splitRow));
			}
			return new ReleaseTrainContents(title, rows);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	String latestReleaseTrainFromWiki(File rawMd) {
		try {
			return Files.readAllLines(rawMd.toPath()).stream()
					.filter(s -> s.trim().startsWith("#")).map(s -> s.substring(1).trim())
					.filter(s -> new ProjectVersion("foo", s).isValid()).findFirst()
					.orElse("");
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
