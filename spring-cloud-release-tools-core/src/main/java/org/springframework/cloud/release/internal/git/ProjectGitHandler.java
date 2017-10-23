package org.springframework.cloud.release.internal.git;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.pom.ProjectVersion;

/**
 * Contains business logic around Git & Github operations
 *
 * @author Marcin Grzejszczak
 */
public class ProjectGitHandler {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String MSG = "Bumping versions";
	private static final String PRE_RELEASE_MSG = "Update SNAPSHOT to %s";
	private static final String POST_RELEASE_MSG = "Going back to snapshots";
	private static final String POST_RELEASE_BUMP_MSG = "Bumping versions to %s after release";

	private final ReleaserProperties properties;
	private final GithubMilestones githubMilestones;

	public ProjectGitHandler(ReleaserProperties properties) {
		this.properties = properties;
		this.githubMilestones = new GithubMilestones(properties);
	}

	public void commitAndTagIfApplicable(File project, ProjectVersion version) {
		GitRepo gitRepo = gitRepo(project);
		if (version.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms", version);
			gitRepo.commit(project, MSG);
		} else {
			log.info("NON-snapshot version [{}] found. Will commit the changed poms, tag the version and push the tag", version);
			gitRepo.commit(project, String.format(PRE_RELEASE_MSG, version.version));
			String tagName = "v" + version.version;
			gitRepo.tag(project, tagName);
			gitRepo.pushTag(project, tagName);
		}
	}

	public void commitAfterBumpingVersions(File project, ProjectVersion version) {
		GitRepo gitRepo = gitRepo(project);
		if (version.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms", version);
			gitRepo.commit(project, String.format(POST_RELEASE_BUMP_MSG, version.bumpedVersion()));
		} else {
			log.info("Non snapshot version [{}] found. Won't do anything", version);
		}
	}

	public File cloneScReleaseProject() {
		try {
			File destinationDir = properties.getGit().getCloneDestinationDir() != null ?
					new File(properties.getGit().getCloneDestinationDir()) :
					Files.createTempDirectory("releaser").toFile();
			return gitRepo(destinationDir).cloneProject(
					URI.create(this.properties.getGit().getSpringCloudReleaseGitUrl()));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void checkout(File project, String branch) {
		gitRepo(project).checkout(project, branch);

	}

	public void revertChangesIfApplicable(File project, ProjectVersion version) {
		if (version.isSnapshot()) {
			log.info("Won't revert a snapshot version");
			return;
		}
		log.info("Reverting last commit");
		gitRepo(project).revert(project, POST_RELEASE_MSG);
	}

	public void pushCurrentBranch(File project) {
		gitRepo(project).pushCurrentBranch(project);
	}

	public void closeMilestone(ProjectVersion releaseVersion) {
		this.githubMilestones.closeMilestone(releaseVersion);
	}

	public String milestoneUrl(ProjectVersion releaseVersion) {
		return this.githubMilestones.milestoneUrl(releaseVersion);
	}

	public String currentBranch(File project) {
		return gitRepo(project).currentBranch(project);
	}

	GitRepo gitRepo(File workingDir) {
		return new GitRepo(workingDir, this.properties);
	}
}
