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

package releaser.internal.git;

import java.io.Closeable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;
import releaser.internal.project.ProjectVersion;
import releaser.internal.tech.TemporaryFileStorage;

import org.springframework.util.StringUtils;

/**
 * Contains business logic around Git operations.
 *
 * @author Marcin Grzejszczak
 */
public class ProjectGitHandler implements Closeable {

	private static final Map<URIish, File> CACHE = new ConcurrentHashMap<>();

	private static final Logger log = LoggerFactory.getLogger(ProjectGitHandler.class);

	private static final String MSG = "Bumping versions";

	private static final String PRE_RELEASE_MSG = "Update SNAPSHOT to %s";

	private static final String POST_RELEASE_MSG = "Going back to snapshots";

	private static final String POST_RELEASE_BUMP_MSG = "Bumping versions to %s after release";

	private final ReleaserProperties properties;

	public ProjectGitHandler(ReleaserProperties properties) {
		this.properties = properties;
		registerShutdownHook();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(TemporaryFileStorage::cleanup));
	}

	public void commitAndTagIfApplicable(File project, ProjectVersion version) {
		GitRepo gitRepo = gitRepo(project);
		if (version.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms",
					version);
			gitRepo.commit(MSG);
		}
		else {
			log.info(
					"NON-snapshot version [{}] found. Will commit the changed poms, tag the version and push the tag",
					version);
			gitRepo.commit(String.format(PRE_RELEASE_MSG, version.version));
			String tagName = "v" + version.version;
			gitRepo.tag(tagName);
			gitRepo.pushTag(tagName);
		}
	}

	public void commitAfterBumpingVersions(File project, ProjectVersion bumpedVersion) {
		if (bumpedVersion.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms",
					bumpedVersion);
			commit(project, String.format(POST_RELEASE_BUMP_MSG, bumpedVersion));
		}
		else {
			log.info("Non snapshot version [{}] found. Won't do anything", bumpedVersion);
		}
	}

	public void commit(File project, String message) {
		GitRepo gitRepo = gitRepo(project);
		gitRepo.commit(message);
	}

	public File cloneTestSamplesProject() {
		return cloneAndCheckOut(this.properties.getGit().getTestSamplesProjectUrl(),
				this.properties.getGit().getTestSamplesBranch());
	}

	public File cloneReleaseTrainWiki() {
		return cloneProject(this.properties.getGit().getReleaseTrainWikiUrl());
	}

	public File cloneReleaseTrainProject() {
		return cloneProject(this.properties.getGit().getReleaseTrainBomUrl());
	}

	public File cloneReleaseTrainDocumentationProject() {
		return cloneAndCheckOut(this.properties.getGit().getReleaseTrainDocsUrl(),
				this.properties.getGit().getReleaseTrainDocsBranch());
	}

	public File cloneDocumentationProject() {
		return cloneAndCheckOut(this.properties.getGit().getDocumentationUrl(),
				this.properties.getGit().getDocumentationBranch());
	}

	public File cloneSpringDocProject() {
		return cloneAndCheckOut(this.properties.getGit().getSpringProjectUrl(),
				this.properties.getGit().getSpringProjectBranch());
	}

	private File cloneAndCheckOut(String springProjectUrl,
			String springProjectUrlBranch) {
		File clonedProject = cloneProject(springProjectUrl);
		checkout(clonedProject, springProjectUrlBranch);
		return clonedProject;
	}

	/**
	 * For meta-release. Works with fixed versions only
	 * @param projectName - name of the project to clone
	 * @return location of the cloned project
	 */
	public File cloneProjectFromOrg(String projectName) {
		String orgUrl = this.properties.getMetaRelease().getGitOrgUrl();
		String fullUrl = orgUrl.endsWith("/") ? (orgUrl + projectName)
				: (orgUrl + "/" + projectName + suffixNonHttpRepo(orgUrl));
		if (log.isDebugEnabled()) {
			log.debug("Full url of the project is [{}]", fullUrl);
		}
		File clonedProject = cloneProject(fullUrl);
		if (log.isDebugEnabled()) {
			log.debug("Successfully cloned the project to [{}]", clonedProject);
		}
		String releaseTrainBranch = this.properties.getGit().getReleaseTrainBranch();
		if (!StringUtils.isEmpty(releaseTrainBranch)) {
			if (log.isDebugEnabled()) {
				log.debug("Checking out configured release train branch {}",
						releaseTrainBranch);
			}
			if (gitRepo(clonedProject).hasBranch(releaseTrainBranch)) {
				log.info("Branch [{}] exists. Will check it out", releaseTrainBranch);
				checkout(clonedProject, releaseTrainBranch);
			}
			return clonedProject;
		}
		String version = this.properties.getFixedVersions().get(projectName);
		if (StringUtils.isEmpty(version)) {
			throw new IllegalStateException(
					"You haven't provided a version for project [" + projectName + "]");
		}
		return findAndCheckOutBranchForVersion(clonedProject, new String[] { version });
	}

	/**
	 * From the version analyzes the branch and checks it out. E.g. - for
	 * spring-cloud-release’s `Finchley.RELEASE version will resolve either Finchley
	 * branch or will fallback to master if there’s no Finchley branch - for
	 * spring-cloud-sleuth’s `2.1.0.RELEASE version will resolve 2.1.x branch
	 * @param url - of project to clone
	 * @param versions - list of versions to check against, if none matches, will fallback
	 * to master
	 * @return location of the cloned project
	 */
	public File cloneAndGuessBranch(String url, String... versions) {
		File clonedProject = cloneProject(url);
		if (log.isDebugEnabled()) {
			log.debug("Successfully cloned the project to [{}]", clonedProject);
		}
		return findAndCheckOutBranchForVersion(clonedProject, versions);
	}

	private File findAndCheckOutBranchForVersion(File clonedProject, String[] versions) {
		if (log.isDebugEnabled()) {
			log.debug("Checking versions {} for project [{}]", versions, clonedProject);
		}
		String branchToCheckout = Arrays.stream(versions).map(this::branchFromVersion)
				.map(version -> gitRepo(clonedProject).hasBranch(version) ? version : "")
				.filter(StringUtils::hasText).findFirst().orElse("master");
		if ("master".equals(branchToCheckout)) {
			log.info(
					"None of the versions {} matches a branch. Assuming that should work with master branch",
					(Object) versions);
			return clonedProject;
		}
		log.info("Branch [{}] exists. Will check it out", branchToCheckout);
		checkout(clonedProject, branchToCheckout);
		return clonedProject;
	}

	/**
	 * Find the commits between two versions. The second version is actually turned into
	 * the head of the relevant branch.
	 * @param clonedProject location of the cloned project
	 * @param fromRef the ref to start from (tag, branch or sha1)
	 * @param toRef the ref to go to (tag, branch or sha1)
	 * @return the list of revisions between these two references
	 */
	public List<SimpleCommit> commitsBetween(File clonedProject, String fromRef,
			String toRef) {
		return gitRepo(clonedProject).log(fromRef, toRef).stream().map(SimpleCommit::new)
				.collect(Collectors.toList());
	}

	/**
	 * Attempt to find the SHA1 of a named tag (without the refs/tags/ prefix).
	 * @param clonedProject location of the cloned project
	 * @param tagName the name of the tag to find
	 * @return an {@link Optional} that is valued with the sha1, if found
	 */
	public Optional<String> findTagSha1(File clonedProject, String tagName) {
		return gitRepo(clonedProject).findTagIdByName(tagName, false)
				.map(AnyObjectId::getName);
	}

	private String suffixNonHttpRepo(String orgUrl) {
		return orgUrl.startsWith("http") || orgUrl.startsWith("git") ? "" : "/";
	}

	File cloneProject(String url) {
		try {
			URIish urIish = new URIish(url);
			// retrieve from cache
			// reset any changes and fetch the latest data
			File destinationDir = destinationDir();
			File clonedProject = CACHE.computeIfAbsent(urIish,
					urIish1 -> gitRepo(destinationDir).cloneProject(urIish));
			if (clonedProject.exists()) {
				log.info(
						"Project has already been cloned. Will try to reset the current branch and fetch the latest changes.");
				try {
					gitRepo(clonedProject).reset();
					gitRepo(clonedProject).fetch();
				}
				catch (Exception ex) {
					log.warn("Couldn't reset / fetch the repository, will continue", ex);
				}
				return clonedProject;
			}
			return clonedProject;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private File destinationDir() {
		return this.properties.getGit().getCloneDestinationDir() != null
				? new File(this.properties.getGit().getCloneDestinationDir())
				: TemporaryFileStorage.createTempDir("releaser");
	}

	private File humanishDestination(URIish projectUrl, File destinationFolder) {
		return new File(destinationFolder, projectUrl.getHumanishName());
	}

	public void checkout(File project, String branch) {
		gitRepo(project).checkout(branch);
	}

	// let's go with convention... If fixed version contains e.g.
	// 2.3.4.RELEASE of Sleuth, we will first check if `2.0.x` branch
	// exists. If not, then we will assume that `master` contains it
	private String branchFromVersion(String version) {
		// 2.3.4.RELEASE -> 2.3.4
		// 2.3.4-RELEASE -> 2.3.4
		// Camden.RELEASE -> Camden
		// Camden-SR -> Camden
		int indexOfDot = version.lastIndexOf(".");
		int indexOfDash = version.indexOf("-");
		int indexToPick = indexOfDot > 0 ? indexOfDot : indexOfDash;
		String versionTillPatch = version.substring(0, indexToPick);
		// 2.3.4 -> [2,3,4]
		// Camden -> [Camden]
		String[] splitVersion = versionTillPatch.split("\\.");
		if (splitVersion.length == 3) {
			// [2,3,4] -> 2.3.x
			return splitVersion[0] + "." + splitVersion[1] + ".x";
		}
		else if (splitVersion.length == 1) {
			// [Camden] -> [Camden.x]
			return splitVersion[0];
		}
		throw new IllegalStateException(
				"Wrong version [" + version + "]. Can't extract semver pieces of it");

	}

	public void revertChangesIfApplicable(File project, ProjectVersion version) {
		if (version.isSnapshot()) {
			log.info("Won't revert a snapshot version");
			return;
		}
		log.info("Reverting last commit");
		gitRepo(project).revert(POST_RELEASE_MSG);
	}

	public void pushCurrentBranch(File project) {
		gitRepo(project).pushCurrentBranch();
	}

	public String currentBranch(File project) {
		return gitRepo(project).currentBranch();
	}

	GitRepo gitRepo(File workingDir) {
		return new GitRepo(workingDir, this.properties);
	}

	@Override
	public void close() {
		CACHE.clear();
	}

	/**
	 * Find the tag names that match a {@link Pattern}.
	 * @param clonedProject the base dir for the cloned repository
	 * @param tagPattern the {@link Pattern} to use to filter tag names
	 * @return a {@link Stream} of the tags whose name match the given {@link Pattern}
	 */
	public Stream<String> findTagNamesMatching(File clonedProject, Pattern tagPattern) {
		return gitRepo(clonedProject).listTags()
				.filter(tagName -> tagPattern.matcher(tagName).matches());
	}

}
