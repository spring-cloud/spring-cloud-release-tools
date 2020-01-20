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

package releaser.reactor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issue;
import com.jcabi.github.Issues;
import com.jcabi.github.Release;
import com.jcabi.github.Repo;
import com.jcabi.github.RepoCommit;
import com.jcabi.github.RepoCommits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.git.SimpleCommit;
import releaser.internal.spring.Arguments;
import releaser.internal.tasks.DryRunReleaseReleaserTask;
import releaser.internal.tasks.ProjectPostReleaseReleaserTask;
import releaser.internal.tasks.release.PushChangesReleaseTask;
import releaser.internal.tech.BuildUnstableException;
import releaser.internal.tech.ExecutionResult;

/**
 * @author Simon Baslé
 */
public class GenerateReleaseNotesTask
		implements ProjectPostReleaseReleaserTask, DryRunReleaseReleaserTask {

	private static final Logger log = LoggerFactory
			.getLogger(GenerateReleaseNotesTask.class);

	private static final String NAME = "releaseNotes";

	private static final String SHORTNAME = "rn";

	private final Github github;

	private final ProjectGitHandler gitHandler;

	public GenerateReleaseNotesTask(Github github, ProjectGitHandler gitHandler) {
		this.github = github;
		this.gitHandler = gitHandler;
	}

	@Override
	public int getOrder() {
		return PushChangesReleaseTask.ORDER + 5;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String shortName() {
		return SHORTNAME;
	}

	@Override
	public String header() {
		return "Generating release notes";
	}

	@Override
	public String description() {
		return "Generates the release notes as a Github release draft, from issues and commits";
	}

	@Override
	public ExecutionResult runTask(Arguments args)
			throws BuildUnstableException, RuntimeException {
		if (args.versionFromBom.isSnapshot()) {
			log.info("\nWon't generate notes for a snapshot version");
			return ExecutionResult.success();
		}

		Repo repo = github.repos().get(new Coordinates.Simple(
				args.properties.getGit().getOrgName(), args.projectToRun.name()));

		Issues issuesClient = repo.issues();
		EnumMap<Type, List<ChangelogEntry>> entries = new EnumMap<>(Type.class);

		String fromVersionTag = "v" + args.projectToRun.originalVersion.version;
		String toVersionTag = "v" + args.versionFromBom.version;

		if (Boolean.TRUE == args.options.interactive) {
			String defaultRange = fromVersionTag + ".." + toVersionTag;
			log.info("\nForce the log range if needed [{}]: ", defaultRange);
			String modifiedRange = System.console().readLine();
			if (!modifiedRange.trim().isEmpty()) {
				String[] range = modifiedRange.split("\\.\\.");
				if (range.length != 2) {
					log.warn("Improper range format: {}, will use: {}", modifiedRange,
							defaultRange);
				}
				else {
					fromVersionTag = range[0];
					toVersionTag = range[1];
				}
			}
		}
		else {
			log.info("Will fetch the log for range {}..{}", fromVersionTag, toVersionTag);
		}

		// gather commits. we use tags in the format `vVERSION`
		final List<SimpleCommit> revCommits = gitHandler.commitsBetween(args.project,
				fromVersionTag, toVersionTag);

		// parse and link to issues if possible, determining type
		for (SimpleCommit revCommit : revCommits) {
			ChangelogEntry entry = parseChangeLogEntry(issuesClient, revCommit);

			for (Type type : entry.types) {
				entries.computeIfAbsent(type, t -> new ArrayList<>()).add(entry);
			}
		}

		// generate the notes
		String notes = generateNotes(args, entries,
				extractContributorMentions(repo, revCommits));

		if (args.options.dryRun != null && args.options.dryRun) {
			// print out
			log.info("[Dry-Run] Generated release notes:");
			log.info("\n\n" + notes + "\n\n");
			return ExecutionResult.success();
		}
		else {
			// WARNING: double check the tag actually exists, otherwise this will create a
			// tag :(
			String tag = "v" + args.versionFromBom.version; // we force using the version
															// from BOM
			Optional<String> maybeTagSha1 = gitHandler.findTagSha1(args.project, tag);
			if (maybeTagSha1.isPresent()) {
				String tagSha1 = maybeTagSha1.get();
				try {
					repo.git().references().get("tags/" + tagSha1).json(); // has to call
																			// json()
					// to trigger fetch, and has to be with SHA1
					boolean releaseExists = StreamSupport
							.stream(repo.releases().iterate().spliterator(), true)
							.map(Release.Smart::new).anyMatch(r -> tagEquals(r, tag));
					if (releaseExists) {
						return ExecutionResult.failure(new IllegalStateException(
								"Release already exists for tag " + tag));
					}
				}
				catch (IOException e) {
					return ExecutionResult.failure(new IllegalStateException(
							"Shouldn't create a release if tag " + tag + " (sha1="
									+ tagSha1 + ") not visible in Github",
							e));
				}

				// create a draft release for the tag
				try {
					Release.Smart release = new Release.Smart(
							repo.releases().create(tag));
					release.draft(true);
					release.tag(tag);
					release.name(tag);
					release.body(notes);
					if (args.versionFromBom.isMilestone() || args.versionFromBom.isRc()) {
						release.prerelease(true);
					}
					return ExecutionResult.success();
				}
				catch (IOException e) {
					return ExecutionResult.failure(e);
				}
			}
			else {
				return ExecutionResult.failure(new IllegalStateException(
						"Attempting to draft release note but tag not found in repository: "
								+ toVersionTag));
			}
		}
	}

	private boolean tagEquals(Release.Smart release, final String tag) {
		try {
			return release.tag().equals(tag);
		}
		catch (IOException e) {
			return false;
		}
	}

	/**
	 * Generate the release notes.
	 */
	protected String generateNotes(Arguments args,
			EnumMap<Type, List<ChangelogEntry>> entries,
			List<String> contributorGithubMentions) {
		// TODO use a template? handlebars !
		StringBuilder notes = new StringBuilder().append(args.projectToRun.name())
				.append(" `").append(args.versionFromBom.version)
				.append("` is part of **`").append(args.releaseTrain().version)
				.append("` Release Train**.");

		notes.append("\n\n## :warning: Update considerations and deprecations");
		for (ChangelogEntry noteworthy : entries.getOrDefault(Type.NOTEWORTHY,
				Collections.emptyList())) {
			notes.append("\n - ").append(noteworthy.description);
			for (String issueTitle : noteworthy.associatedIssueLinksAndTitles.values()) {
				notes.append("\n\t - ").append(cleanupShortMessage(issueTitle));
			}
		}

		notes.append("\n\n## :sparkles: New features and improvements");
		for (ChangelogEntry feature : entries.getOrDefault(Type.FEATURE,
				Collections.emptyList())) {
			notes.append("\n - ").append(feature.description);
			for (String issueTitle : feature.associatedIssueLinksAndTitles.values()) {
				notes.append("\n\t - ").append(cleanupShortMessage(issueTitle));
			}
		}

		notes.append("\n\n## :beetle: Bug fixes");
		for (ChangelogEntry bug : entries.getOrDefault(Type.BUG,
				Collections.emptyList())) {
			notes.append("\n - ").append(bug.description);
			for (String issueTitle : bug.associatedIssueLinksAndTitles.values()) {
				notes.append("\n\t - ").append(cleanupShortMessage(issueTitle));
			}
		}

		notes.append("\n\n## :book: Documentation, Tests and Build");
		for (ChangelogEntry misc : entries.getOrDefault(Type.DOC_MISC,
				Collections.emptyList())) {
			notes.append("\n - ").append(misc.description);
			for (String issueTitle : misc.associatedIssueLinksAndTitles.values()) {
				notes.append("\n\t - ").append(cleanupShortMessage(issueTitle));
			}
		}

		notes.append("\n\n## **TODO DISPATCH THESE**");
		for (ChangelogEntry unclassified : entries.getOrDefault(Type.UNCLASSIFIED,
				Collections.emptyList())) {
			notes.append("\n - ").append(unclassified.description);
			for (String issueTitle : unclassified.associatedIssueLinksAndTitles
					.values()) {
				notes.append("\n\t - ").append(cleanupShortMessage(issueTitle));
			}
		}

		// contributors
		notes.append(
				"\n\n## :+1: Thanks to the following contributors that also participated to this release\n");
		notes.append(String.join(", ", contributorGithubMentions));

		return notes.toString();
	}

	/**
	 * Categorize into a {@link Type} from a set of labels and the commit's short message
	 * (eg. in case of specific message prefix).
	 * @param labels the set of labels found for issues referenced in the commit
	 * @param shortMessage the commit's title/short message
	 * @return a {@link Type} categorizing the commit, or {@link Type#UNCLASSIFIED} if not
	 * clear
	 */
	protected EnumSet<Type> extractTypes(Set<String> labels, String shortMessage) {
		List<Type> types = new ArrayList<>();
		for (String label : labels) {
			switch (label) {
			case "type/bug":
				types.add(Type.BUG);
				break;
			case "type/enhancement":
				types.add(Type.FEATURE);
				break;
			case "type/documentation":
			case "type/dependency-upgrade":
			case "type/chores":
				types.add(Type.DOC_MISC);
				break;
			default:
				if (label.startsWith("warn/")) {
					types.add(Type.NOTEWORTHY);
				}
				break;
			}
		}

		if (shortMessage.startsWith("[build]") || shortMessage.startsWith("[polish]")
				|| shortMessage.startsWith("[doc]")) {
			types.add(Type.DOC_MISC);
		}

		if (types.isEmpty()) {
			return EnumSet.of(Type.UNCLASSIFIED);
		}
		return EnumSet.copyOf(types);
	}

	protected String commitToGithubMention(RepoCommits commitsClient,
			SimpleCommit revCommit) {
		RepoCommit dumbCommit = commitsClient.get(revCommit.fullSha1);
		RepoCommit.Smart smartCommit = new RepoCommit.Smart(dumbCommit);
		try {
			JsonObject commitJson = smartCommit.json();
			return "@" + commitJson.getJsonObject("author").getString("login");
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Extract at-mentions of contributors, given a list of commits (will fetch
	 * contributor login from github). The list is deduplicated and sorted in
	 * case-insensitive alphabetical order.
	 */
	List<String> extractContributorMentions(Repo repo, List<SimpleCommit> revCommits) {
		RepoCommits commitsClient = repo.commits();
		return revCommits.stream().map(c -> commitToGithubMention(commitsClient, c))
				.filter(Objects::nonNull).distinct().sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.toList());
	}

	/**
	 * Clean up a short message, removing issue links in suffix and pr prefix forms.
	 */
	protected String cleanupShortMessage(String shortMessage) {
		final Matcher shortMessageMatcher = SHORT_MESSAGE.matcher(shortMessage);
		if (shortMessageMatcher.matches()) {
			return shortMessageMatcher.group(1).trim();
		}
		return shortMessage;
	}

	/**
	 * Clean up the short message of a {@link SimpleCommit}, ie detect message prefix like
	 * "fix #123 Something something (#124)". The prefix up to the issue link is removed,
	 * and so is the PR reference at the end.
	 */
	protected String cleanupShortMessage(SimpleCommit commit) {
		if (!commit.isMergeCommit) {
			return cleanupShortMessage(commit.title);
		}
		return commit.title;
	}

	/**
	 * Extract issue numbers from links like #123 in the whole commit message, in order.
	 */
	protected Set<Integer> extractIssueNumbers(SimpleCommit commit) {
		Set<Integer> issueNumbers = new LinkedHashSet<>();
		Matcher titleIssueMatcher = ISSUE_REF.matcher(commit.title);
		while (titleIssueMatcher.find()) {
			issueNumbers.add(Integer.valueOf(titleIssueMatcher.group(1)));
		}
		Matcher bodyIssueMatcher = ISSUE_REF.matcher(commit.fullMessage);
		while (bodyIssueMatcher.find()) {
			issueNumbers.add(Integer.valueOf(bodyIssueMatcher.group(1)));
		}
		return issueNumbers;
	}

	/**
	 * From the set of issue numbers (see {@link #extractIssueNumbers(SimpleCommit)}),
	 * extract github client Issue objects and fetch labels and titles from these issues.
	 * The labels and titles are injected in a {@link Set} of labels and {@link Map} of
	 * hashtag issue links to issue titles.
	 */
	protected void fetchIssueLabelsAndTitles(Issues issueClient,
			Set<Integer> issueNumbers, Set<String> labelsTarget,
			Map<String, String> referencedIssuesTarget) {
		issueNumbers.forEach(i -> {
			try {
				Issue.Smart issue = new Issue.Smart(issueClient.get(i));
				issue.labels().iterate().forEach(l -> labelsTarget.add(l.name()));
				referencedIssuesTarget.put("#" + issue.number(), issue.title());
			}
			catch (Exception e) {
				log.warn("Could not fetch issue information for #" + i, e);
			}
		});
	}

	/**
	 * Extract issue information from the commit and generate the {@link ChangelogEntry}.
	 */
	protected ChangelogEntry parseChangeLogEntry(Issues issueClient,
			SimpleCommit commit) {
		String cleanShortMessage = cleanupShortMessage(commit);
		Set<Integer> issueNumbers = extractIssueNumbers(commit);

		Set<String> labelsTarget = new HashSet<>();
		Map<String, String> referencedIssuesTarget = new LinkedHashMap<>();
		fetchIssueLabelsAndTitles(issueClient, issueNumbers, labelsTarget,
				referencedIssuesTarget);

		EnumSet<Type> types = extractTypes(labelsTarget, commit.title);

		return new ChangelogEntry(types, commit.abbreviatedSha1, cleanShortMessage,
				referencedIssuesTarget);
	}

	/*
	 * Pattern specific to reactor commit message convention: `fix #123 Some short
	 * description (#4567)`. We want to capture the middle part and use that in the
	 * release note. The `fix` part is optional, but occurs most of the time unless there
	 * is no issue associated. The `(#xxx)` part is also optional and much more rare,
	 * reflecting the PR number in case there has been extensive review/discussion in that
	 * PR. Both references would be caught by ISSUE_REF pattern below.
	 */
	static Pattern SHORT_MESSAGE = Pattern
			.compile("(?:[a-zA-Z]+ #[0-9]+)?([^\\(]+)(?:\\(#[0-9]+\\))?");

	/**
	 * A {@link Pattern} to find issue/pr numbers in a commit message, by detecting
	 * {@code #}.
	 */
	protected static final Pattern ISSUE_REF = Pattern.compile("#([0-9]+)");

	/**
	 * An enum of the 3 types of changes recognized by the changelog template, plus one
	 * type for noteworthy items (eg. breaking changes) and one additional type for the
	 * commits that couldn't be classified (eg. no relevant prefix and no associated
	 * issue).
	 */
	protected enum Type {

		NOTEWORTHY, BUG, FEATURE, DOC_MISC, UNCLASSIFIED;

	}

	/**
	 * Representation of an entry in the release notes changelog.
	 */
	protected static final class ChangelogEntry {

		/**
		 * The set of types of the change, or singleton {@link Type#UNCLASSIFIED} if
		 * unknown.
		 */
		public final EnumSet<Type> types;

		/**
		 * The human-friendly description to put in the release note for that commit.
		 */
		public final String description;

		/**
		 * The human-friendly SHA1 (typically abbreviated to 8 chars) to reference the
		 * commit in the release note if there is no associated issue.
		 */
		public final String commitSha1;

		/**
		 * An ordered map of github-compatible issue links (including the pound sign) and
		 * their titles, to be added to the draft as possible alternative descriptions.
		 * The links are automatically added to the end of the {@link #description} by the
		 * {@link #ChangelogEntry(EnumSet, String, String, Map)} constructor}.
		 */
		public final Map<String, String> associatedIssueLinksAndTitles;

		/**
		 * @param types the set of {@link Type} of the commit
		 * @param commitSha1 the commit's human-friendly sha1 (can and should be the
		 * abbreviated form)
		 * @param cleanShortMessage the commit's cleaned up title, as should be displayed
		 * in the release note entry
		 * @param associatedIssueLinksAndTitles the commit's associated
		 * issue(s)/pull-request(s), in order of importance. If non-empty, each issue will
		 * be mentioned at the end of the entry, and the issue titles will be added to the
		 * {@link #description} on their own lines as potential alternative descriptions
		 */
		ChangelogEntry(EnumSet<Type> types, String commitSha1, String cleanShortMessage,
				Map<String, String> associatedIssueLinksAndTitles) {
			this.types = types;
			this.commitSha1 = commitSha1;
			this.associatedIssueLinksAndTitles = associatedIssueLinksAndTitles;

			if (this.associatedIssueLinksAndTitles.isEmpty()) {
				description = cleanShortMessage + " (" + commitSha1 + ")";
			}
			else {
				this.description = cleanShortMessage + " ("
						+ String.join(", ", this.associatedIssueLinksAndTitles.keySet())
						+ ")";
			}
		}

		public String toString() {
			return this.description;
		}

	}

}
