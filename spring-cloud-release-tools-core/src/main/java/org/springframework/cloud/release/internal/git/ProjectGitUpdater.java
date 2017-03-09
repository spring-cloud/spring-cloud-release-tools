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
 * Contains business logic around Git operations
 *
 * @author Marcin Grzejszczak
 */
public class ProjectGitUpdater {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String MSG = "Bumping versions";
	private static final String PRE_RELEASE_MSG = "Bumping versions before release";
	private static final String POST_RELEASE_MSG = "Going back to snapshots";

	private final ReleaserProperties properties;

	public ProjectGitUpdater(ReleaserProperties properties) {
		this.properties = properties;
	}

	public void commitAndTagIfApplicable(File project, ProjectVersion version) {
		GitRepo gitRepo = gitRepo(project);
		if (version.isSnapshot()) {
			log.info("Snapshot version [{}] found. Will only commit the changed poms", version);
			gitRepo.commit(project, MSG);
		} else {
			log.info("NON-snapshot version [{}] found. Will commit the changed poms, tag the version and push the tag", version);
			gitRepo.commit(project, PRE_RELEASE_MSG);
			String tagName = "v" + version.version;
			gitRepo.tag(project, tagName);
			gitRepo.pushTag(project, tagName);
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
		gitRepo(project).revert(project, POST_RELEASE_MSG);
	}

	public void pushCurrentBranch(File project) {
		gitRepo(project).pushCurrentBranch(project);
	}

	GitRepo gitRepo(File workingDir) {
		return new GitRepo(workingDir);
	}
}
