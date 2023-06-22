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

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

final class CachingGithub {

	private CachingGithub() {
		throw new IllegalStateException("Can't instantiate this");
	}

	static GitHub INSTANCE;

	static GitHub getInstance(String oauthToken, String cacheDirectory) {
		if (INSTANCE == null) {
			INSTANCE = github(oauthToken, cacheDirectory);
		}
		return INSTANCE;
	}

	private static GitHub github(String oauthToken, String cacheDirectory) {
		Cache cache = new Cache(new File(cacheDirectory), 10 * 1024 * 1024); // 10MB cache
		try {
			return new GitHubBuilder().withOAuthToken(oauthToken)
					.withConnector(new OkHttpGitHubConnector(new OkHttpClient.Builder().cache(cache).build())).build();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
