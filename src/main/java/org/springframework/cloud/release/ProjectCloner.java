package org.springframework.cloud.release;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marcin Grzejszczak
 */
class ProjectCloner {

	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	private final ProjectCloner.JGitFactory gitFactory;

	private final File basedir;

	ProjectCloner(File basedir) {
		this.basedir = basedir;
		this.gitFactory = new ProjectCloner.JGitFactory();
	}

	ProjectCloner(File basedir, ProjectCloner.JGitFactory factory) {
		this.basedir = basedir;
		this.gitFactory = factory;
	}

	File cloneProject(URI projectUrl) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("Cloning repo from [{}] to [{}]", projectUrl, this.basedir);
			}
			Git git = cloneToBasedir(projectUrl, this.basedir);
			if (git != null) {
				git.close();
			}
			File clonedRepo = git.getRepository().getDirectory();
			if (log.isDebugEnabled()) {
				log.debug("Cloned repo to [{}]", clonedRepo);
			}
			return clonedRepo;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Git cloneToBasedir(URI projectUrl, File destinationFolder)
			throws GitAPIException {
		CloneCommand clone = this.gitFactory.getCloneCommandByCloneRepository()
				.setURI(projectUrl.toString() + ".git").setDirectory(destinationFolder);
		try {
			return clone.call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		}
	}

	private void deleteBaseDirIfExists() {
		if (this.basedir.exists()) {
			try {
				FileUtils.delete(this.basedir, FileUtils.RECURSIVE);
			}
			catch (IOException e) {
				throw new IllegalStateException("Failed to initialize base directory", e);
			}
		}
	}

	/**
	 * Wraps the static method calls to {@link org.eclipse.jgit.api.Git} and
	 * {@link org.eclipse.jgit.api.CloneCommand} allowing for easier unit testing.
	 */
	static class JGitFactory {
		CloneCommand getCloneCommandByCloneRepository() {
			return Git.cloneRepository();
		}
	}
}