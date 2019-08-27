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

package org.springframework.cloud.release.internal.github;

import com.jcabi.github.Coordinates;
import com.jcabi.github.Github;
import com.jcabi.github.Issues;
import com.jcabi.github.Milestones;
import com.jcabi.github.Releases;
import com.jcabi.github.Repo;
import com.jcabi.github.Repos;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

class CachingGithubTests {

	@Test
	void should_call_repos_only_once_for_same_github() {
		Github github = mock(Github.class);
		Repos repos = mock(Repos.class);
		given(github.repos()).willReturn(repos);
		CachingGithub cachingGithub = new CachingGithub(github);

		cachingGithub.repos();
		cachingGithub.repos();
		cachingGithub.repos();

		verify(github, only()).repos();
	}

	@Test
	void should_not_cache_repos_calls_for_different_githubs() {
		Github github1 = mock(Github.class);
		Github github2 = mock(Github.class);
		Github github3 = mock(Github.class);

		new CachingGithub(github1).repos();
		new CachingGithub(github2).repos();
		new CachingGithub(github3).repos();

		verify(github1).repos();
		verify(github2).repos();
		verify(github3).repos();
	}

	@Test
	void should_get_repo_only_once_for_same_coordinates() {
		Repos repos = mock(Repos.class);
		CachingRepos cachingRepos = new CachingRepos(repos);
		Coordinates.Simple simple = new Coordinates.Simple("foo", "bar");

		cachingRepos.get(simple);
		cachingRepos.get(simple);
		cachingRepos.get(simple);

		verify(repos, only()).get(simple);
	}

	@Test
	void should_not_cache_calls_for_different_coordinates() {
		Repos repos = mock(Repos.class);
		CachingRepos cachingRepos = new CachingRepos(repos);
		Coordinates.Simple simple1 = new Coordinates.Simple("foo", "bar1");
		Coordinates.Simple simple2 = new Coordinates.Simple("foo", "bar2");
		Coordinates.Simple simple3 = new Coordinates.Simple("foo", "bar3");

		cachingRepos.get(simple1);
		cachingRepos.get(simple2);
		cachingRepos.get(simple3);

		verify(repos).get(simple1);
		verify(repos).get(simple2);
		verify(repos).get(simple3);
	}

	@Test
	void should_coordinates_only_once_for_same_repo() {
		Repo repo = mock(Repo.class);
		given(repo.coordinates()).willReturn(new Coordinates.Simple("foo", "bar"));
		CachingRepo cachingRepo = new CachingRepo(repo);

		cachingRepo.coordinates();
		cachingRepo.coordinates();
		cachingRepo.coordinates();

		verify(repo, only()).coordinates();
	}

	@Test
	void should_not_cache_calls_for_different_coordinates_for_repo() {
		Repo repo1 = mock(Repo.class);
		given(repo1.coordinates()).willReturn(new Coordinates.Simple("foo", "bar"));
		Repo repo2 = mock(Repo.class);
		given(repo2.coordinates()).willReturn(new Coordinates.Simple("foo", "bar"));
		Repo repo3 = mock(Repo.class);
		given(repo3.coordinates()).willReturn(new Coordinates.Simple("foo", "bar"));

		new CachingRepo(repo1).coordinates();
		new CachingRepo(repo2).coordinates();
		new CachingRepo(repo3).coordinates();

		verify(repo1).coordinates();
		verify(repo2).coordinates();
		verify(repo3).coordinates();
	}

	@Test
	void should_issues_only_once_for_same_repo() {
		Repo repo = mock(Repo.class);
		given(repo.issues()).willReturn(BDDMockito.mock(Issues.class));
		CachingRepo cachingRepo = new CachingRepo(repo);

		cachingRepo.issues();
		cachingRepo.issues();
		cachingRepo.issues();

		verify(repo, only()).issues();
	}

	@Test
	void should_not_cache_calls_for_different_issues_for_repo() {
		Repo repo1 = mock(Repo.class);
		given(repo1.issues()).willReturn(BDDMockito.mock(Issues.class));
		Repo repo2 = mock(Repo.class);
		given(repo2.issues()).willReturn(BDDMockito.mock(Issues.class));
		Repo repo3 = mock(Repo.class);
		given(repo3.issues()).willReturn(BDDMockito.mock(Issues.class));

		new CachingRepo(repo1).issues();
		new CachingRepo(repo2).issues();
		new CachingRepo(repo3).issues();

		verify(repo1).issues();
		verify(repo2).issues();
		verify(repo3).issues();
	}

	@Test
	void should_call_milestones_only_once_for_same_repo() {
		Repo repo = mock(Repo.class);
		given(repo.milestones()).willReturn(BDDMockito.mock(Milestones.class));
		CachingRepo cachingRepo = new CachingRepo(repo);

		cachingRepo.milestones();
		cachingRepo.milestones();
		cachingRepo.milestones();

		verify(repo, only()).milestones();
	}

	@Test
	void should_not_cache_calls_for_different_milestones_for_repo() {
		Repo repo1 = mock(Repo.class);
		given(repo1.milestones()).willReturn(BDDMockito.mock(Milestones.class));
		Repo repo2 = mock(Repo.class);
		given(repo2.milestones()).willReturn(BDDMockito.mock(Milestones.class));
		Repo repo3 = mock(Repo.class);
		given(repo3.milestones()).willReturn(BDDMockito.mock(Milestones.class));

		new CachingRepo(repo1).milestones();
		new CachingRepo(repo2).milestones();
		new CachingRepo(repo3).milestones();

		verify(repo1).milestones();
		verify(repo2).milestones();
		verify(repo3).milestones();
	}

	@Test
	void should_call_releases_only_once_for_same_repo() {
		Repo repo = mock(Repo.class);
		given(repo.releases()).willReturn(BDDMockito.mock(Releases.class));
		CachingRepo cachingRepo = new CachingRepo(repo);

		cachingRepo.releases();
		cachingRepo.releases();
		cachingRepo.releases();

		verify(repo, only()).releases();
	}

	@Test
	void should_not_cache_calls_for_different_releases_for_repo() {
		Repo repo1 = mock(Repo.class);
		given(repo1.releases()).willReturn(BDDMockito.mock(Releases.class));
		Repo repo2 = mock(Repo.class);
		given(repo2.releases()).willReturn(BDDMockito.mock(Releases.class));
		Repo repo3 = mock(Repo.class);
		given(repo3.releases()).willReturn(BDDMockito.mock(Releases.class));

		new CachingRepo(repo1).releases();
		new CachingRepo(repo2).releases();
		new CachingRepo(repo3).releases();

		verify(repo1).releases();
		verify(repo2).releases();
		verify(repo3).releases();
	}

}
