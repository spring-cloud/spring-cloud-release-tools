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

package releaser.internal.github;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;

import com.jcabi.github.Assignees;
import com.jcabi.github.Branches;
import com.jcabi.github.Collaborators;
import com.jcabi.github.Contents;
import com.jcabi.github.Coordinates;
import com.jcabi.github.DeployKeys;
import com.jcabi.github.Forks;
import com.jcabi.github.Gists;
import com.jcabi.github.Git;
import com.jcabi.github.Github;
import com.jcabi.github.Gitignores;
import com.jcabi.github.Hooks;
import com.jcabi.github.IssueEvents;
import com.jcabi.github.Issues;
import com.jcabi.github.Labels;
import com.jcabi.github.Language;
import com.jcabi.github.Limits;
import com.jcabi.github.Markdown;
import com.jcabi.github.Milestones;
import com.jcabi.github.Notifications;
import com.jcabi.github.Organizations;
import com.jcabi.github.Pulls;
import com.jcabi.github.Releases;
import com.jcabi.github.Repo;
import com.jcabi.github.RepoCommits;
import com.jcabi.github.Repos;
import com.jcabi.github.Search;
import com.jcabi.github.Stars;
import com.jcabi.github.Users;
import com.jcabi.http.Request;

class CachingGithub implements Github, Closeable {

	private final AtomicReference<Repos> repos = new AtomicReference<>();

	private final Github delegate;

	CachingGithub(Github delegate) {
		this.delegate = delegate;
	}

	@Override
	public Request entry() {
		return this.delegate.entry();
	}

	@Override
	public Repos repos() {
		Repos repos = this.repos.get();
		if (repos == null) {
			this.repos.set(new CachingRepos(this.delegate.repos()));
		}
		return this.repos.get();
	}

	@Override
	public Gists gists() {
		return this.delegate.gists();
	}

	@Override
	public Users users() {
		return this.delegate.users();
	}

	@Override
	public Organizations organizations() {
		return this.delegate.organizations();
	}

	@Override
	public Markdown markdown() {
		return this.delegate.markdown();
	}

	@Override
	public Limits limits() {
		return this.delegate.limits();
	}

	@Override
	public Search search() {
		return this.delegate.search();
	}

	@Override
	public Gitignores gitignores() throws IOException {
		return this.delegate.gitignores();
	}

	@Override
	public JsonObject meta() throws IOException {
		return this.delegate.meta();
	}

	@Override
	public JsonObject emojis() throws IOException {
		return this.delegate.emojis();
	}

	@Override
	public boolean equals(Object o) {
		return this.delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public void close() throws IOException {
		Repos repos = this.repos.get();
		if (repos instanceof Closeable) {
			((Closeable) repos).close();
		}
	}

}

class CachingRepos implements Repos, Closeable {

	private static final Map<Object, Repo> CACHE = new ConcurrentHashMap<>();

	private final Repos delegate;

	CachingRepos(Repos delegate) {
		this.delegate = delegate;
	}

	@Override
	public Github github() {
		return this.delegate.github();
	}

	@Override
	public Repo create(RepoCreate repoCreate) throws IOException {
		return this.delegate.create(repoCreate);
	}

	@Override
	public Repo get(Coordinates coordinates) {
		return CACHE.computeIfAbsent(coordinates,
				o -> new CachingRepo(this.delegate.get(coordinates)));
	}

	@Override
	public void remove(Coordinates coordinates) throws IOException {
		this.delegate.remove(coordinates);
	}

	@Override
	public Iterable<Repo> iterate(String s) {
		return this.delegate.iterate(s);
	}

	@Override
	public boolean equals(Object o) {
		return this.delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public void close() throws IOException {
		CACHE.clear();
	}

}

class CachingRepo implements Repo {

	private static final Map<RepoKey, Object> CACHE = new ConcurrentHashMap<>();

	private final Repo delegate;

	CachingRepo(Repo delegate) {
		this.delegate = delegate;
	}

	@Override
	public Github github() {
		return this.delegate.github();
	}

	@Override
	public Coordinates coordinates() {
		return (Coordinates) CACHE.computeIfAbsent(
				new RepoKey(this.delegate, "coordinates"),
				s -> this.delegate.coordinates());
	}

	@Override
	public Issues issues() {
		return (Issues) CACHE.computeIfAbsent(new RepoKey(this.delegate, "issues"),
				s -> this.delegate.issues());
	}

	@Override
	public Milestones milestones() {
		return (Milestones) CACHE.computeIfAbsent(
				new RepoKey(this.delegate, "milestones"),
				s -> this.delegate.milestones());
	}

	@Override
	public Pulls pulls() {
		return (Pulls) CACHE.computeIfAbsent(new RepoKey(this.delegate, "pulls"),
				s -> this.delegate.pulls());
	}

	@Override
	public Hooks hooks() {
		return (Hooks) CACHE.computeIfAbsent(new RepoKey(this.delegate, "hooks"),
				s -> this.delegate.hooks());
	}

	@Override
	public IssueEvents issueEvents() {
		return (IssueEvents) CACHE.computeIfAbsent(
				new RepoKey(this.delegate, "issueEvents"),
				s -> this.delegate.issueEvents());
	}

	@Override
	public Labels labels() {
		return (Labels) CACHE.computeIfAbsent(new RepoKey(this.delegate, "labels"),
				s -> this.delegate.labels());
	}

	@Override
	public Assignees assignees() {
		return (Assignees) CACHE.computeIfAbsent(new RepoKey(this.delegate, "assignees"),
				s -> this.delegate.assignees());
	}

	@Override
	public Releases releases() {
		return (Releases) CACHE.computeIfAbsent(new RepoKey(this.delegate, "releases"),
				s -> this.delegate.releases());
	}

	@Override
	public DeployKeys keys() {
		return (DeployKeys) CACHE.computeIfAbsent(new RepoKey(this.delegate, "keys"),
				s -> this.delegate.keys());
	}

	@Override
	public Forks forks() {
		return (Forks) CACHE.computeIfAbsent(new RepoKey(this.delegate, "forks"),
				s -> this.delegate.forks());
	}

	@Override
	public RepoCommits commits() {
		return (RepoCommits) CACHE.computeIfAbsent(
				new RepoKey(this.delegate, "repoCommits"), s -> this.delegate.commits());
	}

	@Override
	public Branches branches() {
		return (Branches) CACHE.computeIfAbsent(new RepoKey(this.delegate, "branches"),
				s -> this.delegate.branches());
	}

	@Override
	public Contents contents() {
		return (Contents) CACHE.computeIfAbsent(new RepoKey(this.delegate, "contents"),
				s -> this.delegate.contents());
	}

	@Override
	public Collaborators collaborators() {
		return (Collaborators) CACHE.computeIfAbsent(
				new RepoKey(this.delegate, "collaborators"),
				s -> this.delegate.collaborators());
	}

	@Override
	public Git git() {
		return (Git) CACHE.computeIfAbsent(new RepoKey(this.delegate, "git"),
				s -> this.delegate.git());
	}

	@Override
	public Stars stars() {
		return (Stars) CACHE.computeIfAbsent(new RepoKey(this.delegate, "stars"),
				s -> this.delegate.stars());
	}

	@Override
	public Notifications notifications() {
		return (Notifications) CACHE.computeIfAbsent(
				new RepoKey(this.delegate, "notifications"),
				s -> this.delegate.notifications());
	}

	@Override
	public Iterable<Language> languages() throws IOException {
		return this.delegate.languages();
	}

	@Override
	public JsonObject json() throws IOException {
		return (JsonObject) CACHE.computeIfAbsent(new RepoKey(this.delegate, "json"),
				s -> {
					try {
						return this.delegate.json();
					}
					catch (IOException ex) {
						throw new IllegalStateException(ex);
					}
				});
	}

	@Override
	public void patch(JsonObject jsonObject) throws IOException {
		this.delegate.patch(jsonObject);
	}

	@Override
	public int compareTo(Repo o) {
		return this.delegate.compareTo(o);
	}

}

class RepoKey {

	final Repo repo;

	final String key;

	RepoKey(Repo repo, String key) {
		this.repo = repo;
		this.key = key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RepoKey repoKey = (RepoKey) o;
		return Objects.equals(this.repo, repoKey.repo)
				&& Objects.equals(this.key, repoKey.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.repo, this.key);
	}

}
