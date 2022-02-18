/*
 * Copyright 2013-2020 the original author or authors.
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

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A simple representation of a git commit.
 *
 * @author Simon Baslé
 */
public class SimpleCommit {

	/**
	 * The abbreviated SHA1 of the commit, useful for human-readable output.
	 */
	public final String abbreviatedSha1;

	/**
	 * The full SHA1 of the commit, useful to identify it.
	 */
	public final String fullSha1;

	/**
	 * The title line of the commit (first line of the fullMessage).
	 */
	public final String title;

	/**
	 * The full commit message, including the title, body and separating newlines.
	 */
	public final String fullMessage;

	/**
	 * The name of the author of the commit, who contributed the code.
	 */
	public final String authorName;

	/**
	 * The email of the author of the commit, who contributed the code.
	 */
	public final String authorEmail;

	/**
	 * The name of the committer of the commit, who pushed the code to the repository.
	 */
	public final String committerName;

	/**
	 * The email of the committer of the commit, who pushed the code to the repository.
	 */
	public final String committerEmail;

	/**
	 * Is the commit a merge commit, ie. it has two parents.
	 */
	public final boolean isMergeCommit;

	SimpleCommit(RevCommit revCommit) {
		this(revCommit.abbreviate(8).name(), revCommit.name(), revCommit.getShortMessage(), revCommit.getFullMessage(),
				revCommit.getAuthorIdent().getName(), revCommit.getAuthorIdent().getEmailAddress(),
				revCommit.getCommitterIdent().getName(), revCommit.getCommitterIdent().getEmailAddress(),
				revCommit.getParentCount() > 1);
	}

	/**
	 * Create a {@link SimpleCommit}.
	 * @param abbreviatedSha1 the short version of the SHA-1, for human-readable output
	 * @param fullSha1 the full SHA-1
	 * @param title the first line of the commit message
	 * @param fullMessage the full commit message, including title
	 * @param authorName the name of the commit's author
	 * @param authorEmail the email of the commit's author
	 * @param committerName the name of the commit's committer
	 * @param committerEmail the email of the commit's committer
	 * @param isMergeCommit is the commit a merge commit (with 2 parents)
	 */
	public SimpleCommit(String abbreviatedSha1, String fullSha1, String title, String fullMessage, String authorName,
			String authorEmail, String committerName, String committerEmail, boolean isMergeCommit) {
		this.abbreviatedSha1 = abbreviatedSha1;
		this.fullSha1 = fullSha1;
		this.title = title;
		this.fullMessage = fullMessage;
		this.authorName = authorName;
		this.authorEmail = authorEmail;
		this.committerName = committerName;
		this.committerEmail = committerEmail;
		this.isMergeCommit = isMergeCommit;
	}

}
