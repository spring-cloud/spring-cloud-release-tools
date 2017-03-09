package org.springframework.cloud.release.internal.git;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	GitRepo gitRepo(File workingDir) {
		return new GitRepo(workingDir);
	}
}
