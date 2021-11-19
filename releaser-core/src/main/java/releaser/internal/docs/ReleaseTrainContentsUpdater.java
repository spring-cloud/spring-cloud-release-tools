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

package releaser.internal.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.project.ProjectVersion;
import releaser.internal.project.Projects;
import releaser.internal.template.TemplateGenerator;

import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
// TODO: [SPRING-CLOUD]
class ReleaseTrainContentsUpdater {

	private static final Logger log = LoggerFactory.getLogger(ReleaseTrainContentsUpdater.class);

	private final ReleaseTrainContentsGitHandler handler;

	private final ReleaseTrainContentsParser parser;

	private final TemplateGenerator templateGenerator;

	private final ReleaserProperties properties;

	ReleaseTrainContentsUpdater(ReleaserProperties properties, ProjectGitHandler handler,
			TemplateGenerator templateGenerator) {
		this.properties = properties;
		this.handler = new ReleaseTrainContentsGitHandler(handler);
		this.templateGenerator = templateGenerator;
		this.parser = new ReleaseTrainContentsParser();
	}

	/**
	 * Clones the test project, updates it and runs tests.
	 * @param projects - set of project with versions to assert against
	 */
	File updateReleaseTrainWiki(Projects projects) {
		if (!this.properties.getGit().isUpdateReleaseTrainWiki() || !this.properties.getMetaRelease().isEnabled()) {
			log.info("Will not clone and update the release train wiki, since the switch to do so "
					+ "is off or it's not a meta-release. Set [releaser.git.update-release-train-wiki] to [true] to change that");
			return null;
		}
		File releaseTrainWiki = this.handler.cloneReleaseTrainWiki();
		ProjectVersion releaseTrain = projects.releaseTrain(this.properties);
		String releaseTrainName = releaseTrain.major();
		String wikiPagePrefix = this.properties.getGit().getReleaseTrainWikiPagePrefix();
		String releaseTrainDocFileName = releaseTrainDocFileName(releaseTrainName, wikiPagePrefix);
		log.info("Reading the file [{}] for the current release train", releaseTrainDocFileName);
		File releaseTrainDocFile = releaseTrainDocFile(releaseTrainWiki, releaseTrainDocFileName);
		String releaseVersionFromCurrentFile = this.parser.latestReleaseTrainFromWiki(releaseTrainDocFile);
		log.info("Latest release train version in the file is [{}]", releaseVersionFromCurrentFile);
		if (!isThisReleaseTrainVersionNewer(releaseTrain, releaseVersionFromCurrentFile)) {
			log.info("Current release train version [{}] is not " + "newer than the version taken from the wiki [{}]",
					releaseTrain.version, releaseVersionFromCurrentFile);
			return releaseTrainWiki;
		}
		return generateNewWikiEntry(projects, releaseTrainWiki, releaseTrain, releaseTrainName, releaseTrainDocFile,
				releaseVersionFromCurrentFile);
	}

	private File generateNewWikiEntry(Projects projects, File releaseTrainWiki, ProjectVersion releaseTrain,
			String releaseTrainName, File releaseTrainDocFile, String releaseVersionFromCurrentFile) {
		File releaseNotes = this.templateGenerator.releaseNotes(projects);
		try {
			List<String> lines = Files.readAllLines(releaseTrainDocFile.toPath());
			int lineIndex;
			for (lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
				if (lines.get(lineIndex).contains(releaseVersionFromCurrentFile)) {
					break;
				}
			}
			return insertNewWikiContentBeforeTheLatestRelease(releaseTrainWiki, releaseTrain, releaseTrainName,
					releaseTrainDocFile, releaseNotes, lines, lineIndex);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private File insertNewWikiContentBeforeTheLatestRelease(File releaseTrainWiki, ProjectVersion releaseTrain,
			String releaseTrainName, File releaseTrainDocFile, File releaseNotes, List<String> lines, int lineIndex)
			throws IOException {
		String newContent = new StringJoiner("\n").add(String.join("\n", lines.subList(0, lineIndex))).add("\n")
				.add(new String(Files.readAllBytes(releaseNotes.toPath())))
				.add(String.join("\n", lines.subList(lineIndex, lines.size()))).toString();
		Files.write(releaseTrainDocFile.toPath(), newContent.getBytes());
		log.info("Successfully stored new wiki contents for release train [{}]", releaseTrainName);
		this.handler.commitAndPushChanges(releaseTrainWiki, releaseTrain);
		return releaseTrainWiki;
	}

	private String releaseTrainDocFileName(String releaseTrainName, String wikiPagePrefix) {
		return new StringJoiner("-").add(wikiPagePrefix).add(releaseTrainName).add("Release-Notes.md").toString();
	}

	private boolean isThisReleaseTrainVersionNewer(ProjectVersion releaseTrain, String releaseVersionFromCurrentFile) {
		if (StringUtils.hasText(releaseVersionFromCurrentFile)) {
			return releaseTrain.compareToReleaseTrain(releaseVersionFromCurrentFile) > 0;
		}
		return true;
	}

	private File releaseTrainDocFile(File releaseTrainWiki, String releaseTrainDocFileName) {
		File releaseTrainDocFile = new File(releaseTrainWiki, releaseTrainDocFileName);
		if (!releaseTrainDocFile.exists()) {
			try {
				if (!releaseTrainDocFile.createNewFile()) {
					throw new IllegalStateException("Failed to create releae train doc file");
				}
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return releaseTrainDocFile;
	}

}

class ReleaseTrainContentsGitHandler {

	private static final Logger log = LoggerFactory.getLogger(ReleaseTrainContentsGitHandler.class);

	private static final String PROJECT_PAGE_UPDATED_COMMIT_MSG = "Updating project page to release train [%s]";

	private final ProjectGitHandler handler;

	ReleaseTrainContentsGitHandler(ProjectGitHandler handler) {
		this.handler = handler;
	}

	File cloneReleaseTrainWiki() {
		return this.handler.cloneReleaseTrainWiki();
	}

	void commitAndPushChanges(File repo, ProjectVersion releaseTrain) {
		log.debug("Committing and pushing changes");
		this.handler.commit(repo, String.format(PROJECT_PAGE_UPDATED_COMMIT_MSG, releaseTrain.version));
		this.handler.pushCurrentBranch(repo);
	}

}

class ReleaseTrainContentsParser {

	private static final Logger log = LoggerFactory.getLogger(ReleaseTrainContentsParser.class);

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
					// We want to find only headers like # Finchley.RELEASE and not any
					// custom headers
					.filter(s -> s.trim().startsWith("#") && s.contains(".")).map(s -> s.substring(1).trim())
					.filter(ProjectVersion::isValid).findFirst().orElse("");
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
