package org.springframework.cloud.release.internal.git;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.ReleaserPropertiesAware;
import org.springframework.cloud.release.internal.pom.ProjectVersion;
import org.springframework.cloud.release.internal.pom.Projects;
import org.springframework.util.StringUtils;

/**
 * Contains business logic around Git & Github operations
 *
 * @author Marcin Grzejszczak
 */
public class ProjectGitHandler implements ReleaserPropertiesAware {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String MSG = "Bumping versions";
	private static final String PRE_RELEASE_MSG = "Update SNAPSHOT to %s";
	private static final String POST_RELEASE_MSG = "Going back to snapshots";
	private static final String POST_RELEASE_BUMP_MSG = "Bumping versions to %s after release";

	private ReleaserProperties properties;
	private final GithubMilestones githubMilestones;
	private final GithubIssues githubIssues;

	public ProjectGitHandler(ReleaserProperties properties) {
		this.properties = properties;
		this.githubMilestones = new GithubMilestones(properties);
		this.githubIssues = new GithubIssues(properties);
	}

	public void commitAndTagIfApplicable(File project, ProjectVersion version) {
		GitRepo gitRepo = gitRepo(project);
		if (version.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms", version);
			gitRepo.commit(MSG);
		} else {
			log.info("NON-snapshot version [{}] found. Will commit the changed poms, tag the version and push the tag", version);
			gitRepo.commit(String.format(PRE_RELEASE_MSG, version.version));
			String tagName = "v" + version.version;
			gitRepo.tag(tagName);
			gitRepo.pushTag(tagName);
		}
	}

	public void commitAfterBumpingVersions(File project, ProjectVersion version) {
		if (version.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms", version);
			commit(project, String.format(POST_RELEASE_BUMP_MSG, version.bumpedVersion()));
		} else {
			log.info("Non snapshot version [{}] found. Won't do anything", version);
		}
	}

	public void commit(File project, String message) {
		GitRepo gitRepo = gitRepo(project);
		gitRepo.commit(message);
	}

	public File cloneScReleaseProject() {
		return cloneProject(this.properties.getGit().getSpringCloudReleaseGitUrl());
	}

	public File cloneDocumentationProject() {
		File clonedProject = cloneProject(this.properties.getGit().getDocumentationUrl());
		checkout(clonedProject, this.properties.getGit().getDocumentationBranch());
		return clonedProject;
	}

	/**
	 * For meta-release. Works with fixed versions only
	 * @param projectName - name of the project to clone
	 * @return location of the cloned project
	 */
	public File cloneProjectFromOrg(String projectName) {
		String orgUrl = this.properties.getMetaRelease().getGitOrgUrl();
		String fullUrl = orgUrl.endsWith("/") ? orgUrl + projectName : orgUrl + "/" +
				projectName + suffixNonHttpRepo(orgUrl);
		File clonedProject = cloneProject(fullUrl);
		String version = this.properties.getFixedVersions().get(projectName);
		if (StringUtils.isEmpty(version)) {
			throw new IllegalStateException("You haven't provided a version for project [" + projectName + "]");
		}
		String branchFromVersion = branchFromVersion(version);
		boolean branchExists = gitRepo(clonedProject).hasBranch(branchFromVersion);
		if (!branchExists) {
			log.info("Branch [{}] does not exist. Assuming that should work with master branch", branchFromVersion);
			return clonedProject;
		}
		log.info("Branch [{}] exists. Will check it out", branchFromVersion);
		checkout(clonedProject, branchFromVersion);
		return clonedProject;
	}

	private String suffixNonHttpRepo(String orgUrl) {
		return orgUrl.startsWith("http") || orgUrl.startsWith("git") ? "" : "/";
	}

	File cloneProject(String url) {
		try {
			File destinationDir = properties.getGit().getCloneDestinationDir() != null ?
					new File(properties.getGit().getCloneDestinationDir()) :
					Files.createTempDirectory("releaser").toFile();
			return gitRepo(destinationDir).cloneProject(URI.create(url));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void checkout(File project, String branch) {
		gitRepo(project).checkout(branch);
	}

	// let's go with convention... If fixed version contains e.g.
	// 2.3.4.RELEASE of Sleuth, we will first check if `2.0.x` branch
	// exists. If not, then we will assume that `master` contains it
	private String branchFromVersion(String version) {
		// 2.3.4.RELEASE -> 2.3.4
		// Camden.RELEASE -> Camden
		String versionTillPatch = version.substring(0, version.lastIndexOf("."));
		// 2.3.4 -> [2,3,4]
		// Camden -> [Camden]
		String[] splitVersion = versionTillPatch.split("\\.");
		if (splitVersion.length == 3) {
			// [2,3,4] -> 2.3.x
			return splitVersion[0] + "." + splitVersion[1] + ".x";
		} else if (splitVersion.length == 1) {
			// [Camden] -> [Camden.x]
			return splitVersion[0] + ".x";
		}
		throw new IllegalStateException("Wrong version [" + version + "]. Can't extract semver pieces of it");

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

	public void closeMilestone(ProjectVersion releaseVersion) {
		this.githubMilestones.closeMilestone(releaseVersion);
	}

	public void createIssueInSpringGuides(Projects projects, ProjectVersion version) {
		this.githubIssues.fileIssue(projects, version);
	}

	public String milestoneUrl(ProjectVersion releaseVersion) {
		return this.githubMilestones.milestoneUrl(releaseVersion);
	}

	public String currentBranch(File project) {
		return gitRepo(project).currentBranch();
	}

	GitRepo gitRepo(File workingDir) {
		return new GitRepo(workingDir, this.properties);
	}

	@Override public void setReleaserProperties(ReleaserProperties properties) {
		this.properties = properties;
	}
}
