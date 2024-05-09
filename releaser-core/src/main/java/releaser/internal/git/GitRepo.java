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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.ReleaserProperties;

import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Abstraction over a Git repo. Can cloned repo from a given location and check its
 * branch.
 *
 * @author Marcin Grzejszczak
 */
class GitRepo {

	private static final Logger log = LoggerFactory.getLogger(GitRepo.class);

	private final GitRepo.JGitFactory gitFactory;

	private final File basedir;

	GitRepo(File basedir, ReleaserProperties properties) {
		this.basedir = basedir;
		this.gitFactory = new GitRepo.JGitFactory(properties);
	}

	// for tests
	GitRepo(File basedir) {
		this.basedir = basedir;
		this.gitFactory = new GitRepo.JGitFactory();
	}

	// for tests
	GitRepo(File basedir, GitRepo.JGitFactory factory) {
		this.basedir = basedir;
		this.gitFactory = factory;
	}

	/**
	 * Clones the project.
	 * @param projectUri - URI of the project
	 * @return file where the project was cloned
	 */
	File cloneProject(URIish projectUri) {
		try {
			log.info("Cloning repo from [{}] to [{}]", projectUri, humanishDestination(projectUri, this.basedir));
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
	 * Checks out a branch for a project.
	 * @param branch - branch to check out
	 */
	void checkout(String branch) {
		try {
			log.info("Checking out branch [{}] for repo [{}]", branch, this.basedir);
			checkoutBranch(this.basedir, branch);
			log.info("Successfully checked out the branch [{}]", branch);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Fetch changes.
	 */
	void fetch() {
		try {
			log.info("Pull changes for repo [{}]", this.basedir);
			fetch(this.basedir);
			log.info("Successfully pulled the changes");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Reset changes.
	 */
	void reset() {
		try {
			log.info("Resetting changes for repo [{}]", this.basedir);
			reset(this.basedir);
			log.info("Successfully reset any changes");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Performs a commit.
	 * @param message - commit message
	 */
	void commit(String message) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			git.add().addFilepattern(".").call();
			git.commit().setAllowEmpty(false).setMessage(message).call();
			printLog(git);
		}
		catch (EmptyCommitException e) {
			log.info("There were no changes detected. Will not commit an empty commit");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	void checkoutTag(String tagName) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			Optional<ObjectId> tagId = findTagIdByName(tagName, false);
			if (tagId.isPresent()) {
				git.checkout().setName(tagId.get().getName()).call();
			}
			else {
				throw new IllegalStateException("Tag with name [" + tagName + "] not found");
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Attempt to retrieve a tag id from a name, prepending the name with /refs/tags/.
	 */
	Optional<ObjectId> findTagIdByName(String tagName, boolean unpeel) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			return git.tagList().call().stream().filter(ref -> ref.getName().equals("refs/tags/" + tagName)).findFirst()
					.map(ref -> {
						if (ref.isPeeled() && unpeel) {
							return ref.getPeeledObjectId();
						}
						return ref.getObjectId();
					});
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to fetch git tag id for refs/tags/" + tagName, e);
		}
	}

	/**
	 * Logs {@link RevCommit} between two tags / branches / hashes.
	 * @param from oldest revision
	 * @param to newest revision
	 */
	List<RevCommit> log(String from, String to) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			final Optional<Ref> fromRevisionOptional = findTagOrBranchHeadRevision(git, from);
			final Optional<Ref> toRevisionOptional = findTagOrBranchHeadRevision(git, to);

			ObjectId fromRevision;
			if (fromRevisionOptional.isPresent()) {
				Ref ref = fromRevisionOptional.get();
				if (ref.isPeeled()) {
					fromRevision = ref.getPeeledObjectId();
				}
				else {
					fromRevision = ref.getObjectId();
				}
			}
			else {
				fromRevision = ObjectId.fromString(from);
			}

			ObjectId toRevision;
			if (toRevisionOptional.isPresent()) {
				Ref ref = toRevisionOptional.get();
				if (ref.isPeeled()) {
					toRevision = ref.getPeeledObjectId();
				}
				else {
					toRevision = ref.getObjectId();
				}
			}
			else {
				toRevision = ObjectId.fromString(to);
			}

			LinkedList<RevCommit> commits = new LinkedList<>();
			git.log().addRange(fromRevision, toRevision).call().forEach(commits::add);
			return commits;
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to fetch git log for " + from + ".." + to, e);
		}
	}

	/**
	 * List all the tags in the repository.
	 * @return a {@link List} of all the tags in the repository.
	 */
	Stream<String> listTags() {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			final RevWalk walk = new RevWalk(git.getRepository());
			List<Ref> allTagsNewestFirst = git.tagList().call();
			Collections.sort(allTagsNewestFirst, (Comparator<Ref>) (o1, o2) -> {
				Date d1;
				Date d2;
				try {
					d1 = walk.parseTag(o1.getObjectId()).getTaggerIdent().getWhen();
				}
				catch (IOException ioe) {
					return 1; // put at end
				}
				try {
					d2 = walk.parseTag(o2.getObjectId()).getTaggerIdent().getWhen();
				}
				catch (IOException ioe) {
					return -1; // put ahead
				}
				return d2.compareTo(d1); // more recent first
			});
			return allTagsNewestFirst.stream().map(ref -> Repository.shortenRefName(ref.getName()));
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to fetch git tags", e);
		}
	}

	/**
	 * Look for a tag with the given name, and if not found looks for a branch.
	 */
	private Optional<Ref> findTagOrBranchHeadRevision(Git git, String tagOrBranch) throws GitAPIException, IOException {
		if (tagOrBranch.equals("HEAD")) {
			return Optional.of(git.getRepository().exactRef(Constants.HEAD).getTarget());
		}
		final Optional<Ref> tag = git.tagList().call().stream()
				.filter(ref -> ref.getName().equals("refs/tags/" + tagOrBranch)).findFirst();
		if (tag.isPresent()) {
			return tag;
		}

		return git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().stream()
				.filter(ref -> ref.getName().equals("refs/heads/" + tagOrBranch)).findFirst();
	}

	boolean hasBranch(String branch) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
			boolean present = refs.stream().anyMatch(ref -> branch.equals(nameOfBranch(ref.getName())));
			if (log.isDebugEnabled()) {
				log.debug("Branch [{}] is present [{}]", branch, present);
			}
			return present;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private String nameOfBranch(String branch) {
		// TODO careful: this doesn't take into account branches that follow a pattern
		// like `experiments/foo`
		return branch.substring(branch.lastIndexOf("/") + 1);
	}

	private void printLog(Git git) throws GitAPIException, IOException {
		int maxCount = 5;
		String currentBranch = git.getRepository().getBranch();
		log.info("Printing [{}] last commits for branch [{}]", maxCount, currentBranch);
		Iterable<RevCommit> commits = git.log().setMaxCount(maxCount).call();
		for (RevCommit commit : commits) {
			log.info("Name [{}], msg [{}]", commit.getId().name(), commit.getShortMessage());
		}
	}

	/**
	 * Creates a tag with a given name.
	 * @param tagName name of the tag to set
	 */
	void tag(String tagName) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			git.tag().setName(tagName).call();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Pushes the commits to {@code origin} remote branch.
	 * @param branch - remote branch to which the code should be pushed
	 */
	void pushBranch(String branch) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			String localBranch = git.getRepository().getFullBranch();
			RefSpec refSpec = new RefSpec(localBranch + ":" + branch);
			this.gitFactory.push(git).setPushTags().setRefSpecs(refSpec).call();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Pushes the commits od current branch.
	 */
	void pushCurrentBranch() {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			this.gitFactory.push(git).call();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Pushes the commits to {@code origin} remote tag.
	 * @param tagName - remote tag to which the code should be pushed
	 */
	void pushTag(String tagName) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			String localBranch = git.getRepository().getFullBranch();
			RefSpec refSpec = new RefSpec(localBranch + ":" + "refs/tags/" + tagName);
			this.gitFactory.push(git).setPushTags().setRefSpecs(refSpec).call();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	void revert(String message) {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			RevCommit commit = git.log().setMaxCount(1).call().iterator().next();
			String shortMessage = commit.getShortMessage();
			String id = commit.getId().getName();
			if (!shortMessage.contains("Update SNAPSHOT to ")) {
				throw new IllegalStateException(
						"Won't revert the commit with id [" + id + "] " + "and message [" + shortMessage
								+ "]. Only commit that updated " + "snapshot to another version can be reverted");
			}
			log.debug("The commit to be reverted is [{}]", commit);
			git.revert().include(commit).call();
			git.commit().setAmend(true).setMessage(message).call();
			printLog(git);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	String currentBranch() {
		try (Git git = this.gitFactory.open(file(this.basedir))) {
			return git.getRepository().getBranch();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private File file(File project) throws FileNotFoundException {
		return ResourceUtils.getFile(project.toURI()).getAbsoluteFile();
	}

	private Git cloneToBasedir(URIish projectUrl, File destinationFolder) throws GitAPIException {
		CloneCommand command = this.gitFactory.getCloneCommandByCloneRepository().setURI(projectUrl.toString() + ".git")
				.setDirectory(humanishDestination(projectUrl, destinationFolder));
		try {
			return command.call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		}
	}

	private File humanishDestination(URIish projectUrl, File destinationFolder) {
		return new File(destinationFolder, projectUrl.getHumanishName());
	}

	private FetchResult fetch(File projectDir) throws GitAPIException {
		Git git = this.gitFactory.open(projectDir);
		FetchCommand command = git.fetch();
		try {
			return command.setCredentialsProvider(gitFactory.provider).call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		}
		finally {
			git.close();
		}
	}

	private Ref reset(File projectDir) throws GitAPIException {
		Git git = this.gitFactory.open(projectDir);
		ResetCommand command = git.reset().setMode(ResetCommand.ResetType.HARD);
		try {
			return command.call();
		}
		catch (GitAPIException e) {
			deleteBaseDirIfExists();
			throw e;
		}
		finally {
			git.close();
		}
	}

	private Ref checkoutBranch(File projectDir, String branch) throws GitAPIException {
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
		}
		finally {
			git.close();
		}
	}

	private boolean shouldTrack(Git git, String label) throws GitAPIException {
		return isBranch(git, label) && !isLocalBranch(git, label);
	}

	private void trackBranch(CheckoutCommand checkout, String label) {
		checkout.setCreateBranch(true).setName(label).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
				.setStartPoint("origin/" + label);
	}

	private boolean isBranch(Git git, String label) throws GitAPIException {
		return containsBranch(git, label, ListBranchCommand.ListMode.ALL);
	}

	private boolean isLocalBranch(Git git, String label) throws GitAPIException {
		return containsBranch(git, label, null);
	}

	private boolean containsBranch(Git git, String label, ListBranchCommand.ListMode listMode) throws GitAPIException {
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

		private static final Logger log = LoggerFactory.getLogger(JGitFactory.class);

		private final JschConfigSessionFactory factory = new JschConfigSessionFactory() {

			@Override
			protected void configure(OpenSshConfig.Host host, Session session) {
			}

			@Override
			protected JSch createDefaultJSch(FS fs) throws JSchException {
				Connector connector = null;
				try {
					if (SSHAgentConnector.isConnectorAvailable()) {
						USocketFactory usf = new JNAUSocketFactory();
						connector = new SSHAgentConnector(usf);
					}
					log.info("Successfully connected to an agent");
				}
				catch (AgentProxyException e) {
					log.error("Exception occurred while trying to connect to agent. Will create"
							+ "the default JSch connection", e);
					return super.createDefaultJSch(fs);
				}
				final JSch jsch = super.createDefaultJSch(fs);
				if (connector != null) {
					JSch.setConfig("PreferredAuthentications", "publickey,password");
					IdentityRepository identityRepository = new RemoteIdentityRepository(connector);
					jsch.setIdentityRepository(identityRepository);
				}
				return jsch;
			}
		};

		private final CredentialsProvider provider;

		private final TransportConfigCallback callback = transport -> {
			if (transport instanceof SshTransport) {
				SshTransport sshTransport = (SshTransport) transport;
				sshTransport.setSshSessionFactory(this.factory);
			}
		};

		JGitFactory(ReleaserProperties releaserProperties) {
			if (StringUtils.hasText(releaserProperties.getGit().getUsername())) {
				log.info("Passed username and password - will set a custom credentials provider");
				this.provider = credentialsProvider(releaserProperties);
			}
			else {
				log.info("No custom credentials provider will be set");
				this.provider = null;
			}
		}

		// for tests
		JGitFactory() {
			this.provider = null;
		}

		CredentialsProvider credentialsProvider(ReleaserProperties properties) {
			return new UsernamePasswordCredentialsProvider(properties.getGit().getUsername(),
					properties.getGit().getPassword());
		}

		CloneCommand getCloneCommandByCloneRepository() {
			return Git.cloneRepository().setCredentialsProvider(this.provider)
					.setTransportConfigCallback(this.callback);
		}

		PushCommand push(Git git) {
			return git.push().setCredentialsProvider(this.provider).setTransportConfigCallback(this.callback);
		}

		Git open(File file) {
			try {
				return Git.open(file);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		Git init(File file) {
			try {
				return Git.init().setDirectory(file).call();
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
