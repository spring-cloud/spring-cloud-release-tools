/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.cloud.release.internal;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction over a Git repo. Can clonea repo from a given location
 * and check its branch.
 *
 * @author Marcin Grzejszczak
 */
class GitProjectRepo {

	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass());

	private final GitProjectRepo.JGitFactory gitFactory;

	private final File basedir;

	GitProjectRepo(File basedir) {
		this.basedir = basedir;
		this.gitFactory = new GitProjectRepo.JGitFactory();
	}

	GitProjectRepo(File basedir, GitProjectRepo.JGitFactory factory) {
		this.basedir = basedir;
		this.gitFactory = factory;
	}

	/**
	 * Clones the project
	 * @param projectUri - URI of the project
	 * @return file where the project was cloned
	 */
	File cloneProject(URI projectUri) {
		try {
			File file = new File(projectUri.getPath());
			URI modifiedUri = file.getName().endsWith(File.separator) ?
					projectUri : new File(file.getPath() + File.separator).toURI();
			log.debug("Cloning repo from [{}] to [{}]", modifiedUri, this.basedir);
			Git git = cloneToBasedir(modifiedUri, this.basedir);
			if (git != null) {
				git.close();
			}
			File clonedRepo = git.getRepository().getWorkTree();
			log.debug("Cloned repo to [{}]", clonedRepo);
			return clonedRepo;
		}
		catch (Exception e) {
			throw new IllegalStateException("Exception occurred while cloning repo", e);
		}
	}

	/**
	 * Checks out a branch for a project
	 * @param project - a Git project
	 * @param branch - branch to check out
	 */
	void checkout(File project, String branch) {
		try {
			log.debug("Checking out branch [{}] for repo [{}] to [{}]", this.basedir, branch);
			checkoutBranch(project, branch);
			log.debug("Successfully checked out the branch [{}]", branch);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Git cloneToBasedir(URI projectUrl, File destinationFolder)
			throws GitAPIException {
		CloneCommand command = this.gitFactory.getCloneCommandByCloneRepository()
				.setURI(projectUrl.toString() + ".git").setDirectory(destinationFolder);
		try {
			return command.call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		}
	}

	private Ref checkoutBranch(File projectDir, String branch)
			throws GitAPIException {
		Git git = this.gitFactory.open(projectDir);
		CheckoutCommand command = git.checkout().setName(branch);
		try {
			return command.call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		} finally {
			git.close();
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

		Git open(File file) {
			try {
				return Git.open(file);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}