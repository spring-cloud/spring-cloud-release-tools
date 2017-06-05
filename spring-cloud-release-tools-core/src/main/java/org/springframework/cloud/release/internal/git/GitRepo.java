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
package org.springframework.cloud.release.internal.git;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.EmtpyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

/**
 * Abstraction over a Git repo. Can cloned repo from a given location
 * and check its branch.
 *
 * @author Marcin Grzejszczak
 */
class GitRepo {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final GitRepo.JGitFactory gitFactory;

	private final File basedir;

	GitRepo(File basedir) {
		this.basedir = basedir;
		this.gitFactory = new GitRepo.JGitFactory();
	}

	GitRepo(File basedir, GitRepo.JGitFactory factory) {
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
			log.info("Cloning repo from [{}] to [{}]", projectUri, this.basedir);
			Git git = cloneToBasedir(projectUri, this.basedir);
			if (git != null) {
				git.close();
			}
			File clonedRepo = git.getRepository().getWorkTree();
			log.info("Cloned repo to [{}]", clonedRepo);
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
			log.info("Checking out branch [{}] for repo [{}] to [{}]", this.basedir, branch);
			checkoutBranch(project, branch);
			log.info("Successfully checked out the branch [{}]", branch);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Performs a commit
	 * @param project - a Git project
	 * @param message - commit message
	 */
	void commit(File project, String message) {
		try(Git git = this.gitFactory.open(file(project))) {
			git.add().addFilepattern(".").call();
			git.commit().setAllowEmpty(false).setMessage(message).call();
		} catch (EmtpyCommitException e) {
			log.info("There were no changes detected. Will not commit an empty commit");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates a tag with a given name
	 * @param project
	 * @param tagName
	 */
	void tag(File project, String tagName) {
		try(Git git = this.gitFactory.open(file(project))) {
			git.tag().setName(tagName).call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Pushes the commits to {@code origin} remote branch
	 * @param project - Git project
	 * @param branch - remote branch to which the code should be pushed
	 */
	void pushBranch(File project, String branch) {
		try(Git git = this.gitFactory.open(file(project))) {
			String localBranch = git.getRepository().getFullBranch();
			RefSpec refSpec = new RefSpec(localBranch + ":" + branch);
			this.gitFactory.push(git).setPushTags().setRefSpecs(refSpec).call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Pushes the commits od current branch
	 * @param project - Git project
	 */
	void pushCurrentBranch(File project) {
		try(Git git = this.gitFactory.open(file(project))) {
			this.gitFactory.push(git).call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Pushes the commits to {@code origin} remote tag
	 * @param project - Git project
	 * @param tagName - remote tag to which the code should be pushed
	 */
	void pushTag(File project, String tagName) {
		try(Git git = this.gitFactory.open(file(project))) {
			String localBranch = git.getRepository().getFullBranch();
			RefSpec refSpec = new RefSpec(localBranch + ":" + "refs/tags/" + tagName);
			this.gitFactory.push(git).setPushTags().setRefSpecs(refSpec).call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	void revert(File project, String message) {
		try(Git git = this.gitFactory.open(file(project))) {
			RevCommit commit = git.log().setMaxCount(1).call().iterator().next();
			log.debug("The commit to be reverted is [{}]", commit);
			git.revert().include(commit).call();
			git.commit().setAmend(true).setMessage(message).call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private File file(File project) throws FileNotFoundException {
		return ResourceUtils.getFile(project.toURI()).getAbsoluteFile();
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
			if (shouldTrack(git, branch)) {
				trackBranch(command, branch);
			}
			return command.call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		} finally {
			git.close();
		}
	}

	private boolean shouldTrack(Git git, String label) throws GitAPIException {
		return isBranch(git, label) && !isLocalBranch(git, label);
	}

	private void trackBranch(CheckoutCommand checkout, String label) {
		checkout.setCreateBranch(true).setName(label)
				.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
				.setStartPoint("origin/" + label);
	}

	private boolean isBranch(Git git, String label) throws GitAPIException {
		return containsBranch(git, label, ListBranchCommand.ListMode.ALL);
	}

	private boolean isLocalBranch(Git git, String label) throws GitAPIException {
		return containsBranch(git, label, null);
	}

	private boolean containsBranch(Git git, String label, ListBranchCommand.ListMode listMode)
			throws GitAPIException {
		ListBranchCommand command = git.branchList();
		if (listMode != null) {
			command.setListMode(listMode);
		}
		List<Ref> branches = command.call();
		for (Ref ref : branches) {
			if (ref.getName().endsWith("/" + label)) {
				return true;
			}
		}
		return false;
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
		private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

		private final JschConfigSessionFactory factory = new JschConfigSessionFactory() {

			@Override protected void configure(OpenSshConfig.Host host, Session session) {
			}

			@Override
			protected JSch createDefaultJSch(FS fs) throws JSchException {
				Connector connector = null;
				try {
					if(SSHAgentConnector.isConnectorAvailable()){
						USocketFactory usf = new JNAUSocketFactory();
						connector = new SSHAgentConnector(usf);
					}
				} catch (AgentProxyException e) {
					log.error("Exception occurred while trying to connect to agent. Will create"
							+ "the default JSch connection", e);
					return super.createDefaultJSch(fs);
				}
				final JSch jsch = super.createDefaultJSch(fs);
				if (connector != null) {
					JSch.setConfig("PreferredAuthentications", "publickey");
					IdentityRepository identityRepository = new RemoteIdentityRepository(connector);
					jsch.setIdentityRepository(identityRepository);
				}
				return jsch;
			}
		};

		private final TransportConfigCallback callback = transport -> {
			if (transport instanceof SshTransport) {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(this.factory);
			}
		};

		CloneCommand getCloneCommandByCloneRepository() {
			return Git.cloneRepository().setTransportConfigCallback(this.callback);
		}

		PushCommand push(Git git) {
			return git.push().setTransportConfigCallback(this.callback);
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