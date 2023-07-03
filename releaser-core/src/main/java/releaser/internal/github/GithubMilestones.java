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

package releaser.internal.github;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.tech.ReleaserProcessExecutor;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 */
class GithubMilestones {

	static final Map<ProjectVersion, String> MILESTONE_URL_CACHE = new ConcurrentHashMap<>();
	static final Map<ProjectVersion, GHMilestone> MILESTONE_CACHE = new ConcurrentHashMap<>();

	static final AtomicReference<Path> DOWNLOADED_GITHUB_CHANGELOG = new AtomicReference<>();

	private static final Logger log = LoggerFactory.getLogger(GithubMilestones.class);

	private final GitHub github;

	private final ReleaserProperties properties;

	GithubMilestones(ReleaserProperties properties) {
		this(CachingGithub.getInstance(properties.getGit().getOauthToken(), properties.getGit().getCacheDirectory()),
				properties);
	}

	GithubMilestones(GitHub github, ReleaserProperties properties) {
		this.github = github;
		this.properties = properties;
	}

	void closeMilestone(ProjectVersion version) {
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You must set the value of the OAuth token. You can do it "
						+ "either via the command line [--releaser.git.oauth-token=...] "
						+ "or put it as an env variable in [~/.bashrc] or "
						+ "[~/.zshrc] e.g. [export RELEASER_GIT_OAUTH_TOKEN=...]");
		GHMilestone foundMilestone = MILESTONE_CACHE.get(version);
		String tagVersion = version.version;
		if (foundMilestone == null) {
			foundMilestone = matchingMilestone(tagVersion, openMilestones(version));
			if (foundMilestone != null) {
				MILESTONE_CACHE.put(version, foundMilestone);
			}
		}
		if (foundMilestone != null) {
			try {
				log.info("Found a matching milestone - closing it");
				foundMilestone.close();
				log.info("Closed the [{}] milestone", tagVersion);
			}
			catch (IOException e) {
				log.error("Exception occurred while trying to retrieve the milestone", e);
			}
		}
		else {
			log.warn("No matching milestone was found");
		}
	}

	void createReleaseNotesForMilestone(ProjectVersion version) {
		String contents = null;
		if (version.isReleaseTrain()) {
			String listOfReleases = this.properties.getFixedVersions().entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.filter(entry -> !entry.getKey().equals("spring-boot") && this.properties.getMetaRelease()
							.getReleaseTrainDependencyNames().stream().noneMatch(s -> entry.getKey().equals(s)))
					.map(entry -> "- " + entry.getKey() + " [" + entry.getValue() + "](https://github.com/" + org()
							+ "/" + entry.getKey() + "/releases/tag/v" + entry.getValue() + ")")
					.collect(Collectors.joining("\n"));
			contents = "## :star: Releases\n\n" + listOfReleases;
		}
		else {
			try {
				boolean notPreviouslyDownloaded = DOWNLOADED_GITHUB_CHANGELOG.compareAndSet(null,
						Files.createTempFile("releaser-changelog-generator", ".jar"));
				if (notPreviouslyDownloaded) {
					try (ReadableByteChannel readableByteChannel = Channels.newChannel(
							new URL(this.properties.getGit().getGithubChangelogGeneratorUrl()).openStream())) {
						downloadFatJarIfNotPresent(readableByteChannel);
					}
				}
				contents = readChangelogFromGeneratorOutput(version);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		createGithubMilestoneReleaseNotes(version, contents);
	}

	String readChangelogFromGeneratorOutput(ProjectVersion version) throws IOException {
		Path path = DOWNLOADED_GITHUB_CHANGELOG.get();
		Path workDir = path.getParent();
		ReleaserProcessExecutor processExecutor = new ReleaserProcessExecutor(workDir.toAbsolutePath().toString());
		// java -jar -Dchangelog.repository=spring-cloud/spring-cloud-sleuth
		// github-changelog-generator.jar 3.1.8 ./sleuth.md
		processExecutor.runCommand(new String[] { "java", "-jar",
				"-Dchangelog.repository=" + this.properties.getGit().getOrgName() + "/" + version.projectName,
				"-Dgithub.username=" + this.properties.getGit().getUsername(),
				"-Dgithub.password=" + this.properties.getGit().getOauthToken(), path.getFileName().toString(),
				version.version, version.projectName + ".md" }, 2L);
		return Files.readString(workDir.resolve(version.projectName + ".md"));
	}

	void createGithubMilestoneReleaseNotes(ProjectVersion version, String contents) {
		try {
			String tag = "v" + version.version;
			log.info("Creating a new release {} for project {}", tag, version.projectName);
			getRepository(version).createRelease(tag).name(version.version).body(contents).create();
			log.info("Created a new release");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void downloadFatJarIfNotPresent(ReadableByteChannel readableByteChannel) throws IOException {
		Path path = DOWNLOADED_GITHUB_CHANGELOG.get();
		log.info("Will download the github changelog generator from {} to {}",
				this.properties.getGit().getGithubChangelogGeneratorUrl(), path);
		FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
		fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		log.info("File downloaded");
	}

	GHMilestone matchingMilestone(String tagVersion, Iterable<GHMilestone> milestones) {
		log.debug("Successfully received list of milestones [{}]", milestones);
		log.info("Will try to match against tag version [{}]", tagVersion);
		try {
			int counter = 0;
			for (GHMilestone milestone : milestones) {
				if (counter++ >= this.properties.getGit().getNumberOfCheckedMilestones()) {
					log.warn(
							"No matching milestones were found within the provided threshold [{}] of checked milestones",
							this.properties.getGit().getNumberOfCheckedMilestones());
					return null;
				}
				String title = milestone.getTitle();
				if (tagVersion.equals(title) || numericVersion(tagVersion).equals(title)) {
					log.info("Found a matching milestone [{}]", milestone.getNumber());
					return milestone;
				}
			}
		}
		catch (Exception e) {
			log.error("Exception occurred while trying to retrieve the milestone", e);
			return null;
		}
		log.warn("No matching milestones were found");
		return null;
	}

	String milestoneUrl(ProjectVersion version) {
		String cachedUrl = MILESTONE_URL_CACHE.get(version);
		if (StringUtils.hasText(cachedUrl)) {
			return cachedUrl;
		}
		Assert.hasText(this.properties.getGit().getOauthToken(),
				"You have to pass Github OAuth token for milestone closing to be operational");
		String tagVersion = version.version;
		GHMilestone foundMilestone = matchingMilestone(tagVersion, closedMilestones(version));
		String foundUrl = "";
		if (foundMilestone != null) {
			try {
				URL url = foundMilestoneUrl(foundMilestone);
				log.info("Found a matching milestone with issues URL [{}]", url);
				foundUrl = url.toString().replace("https://api.github.com/repos", "https://github.com")
						.replace("milestones", "milestone") + "?closed=1";
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			catch (Exception e) {
				log.error("Exception occurred while trying to find milestone", e);
			}
		}
		MILESTONE_URL_CACHE.put(version, foundUrl);
		return foundUrl;
	}

	private String numericVersion(String version) {
		return version.contains("RELEASE") ? version.substring(0, version.lastIndexOf(".")) : "";
	}

	URL foundMilestoneUrl(GHMilestone milestone) throws IOException {
		return milestone.getUrl();
	}

	private Iterable<GHMilestone> openMilestones(ProjectVersion version) {
		try {
			return getRepository(version).listMilestones(GHIssueState.OPEN).toList();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private GHRepository getRepository(ProjectVersion version) throws IOException {
		return this.github.getRepository(org() + "/" + version.projectName);
	}

	private Iterable<GHMilestone> closedMilestones(ProjectVersion version) {
		try {
			return this.github.getRepository(org() + "/" + version.projectName).listMilestones(GHIssueState.CLOSED)
					.toList();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	String org() {
		return this.properties.getGit().getOrgName();
	}

}
