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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.jcabi.github.mock.MkGithub;
import org.junit.jupiter.api.Test;
import releaser.internal.git.SimpleCommit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static releaser.reactor.GenerateReleaseNotesTask.Type;

/**
 * @author Simon Baslé
 */
@SpringBootTest
@ActiveProfiles("test")
class GenerateReleaseNotesTaskTest {

	@Autowired
	GenerateReleaseNotesTask task;

	@Autowired
	MkGithub githubClient;

	@Test
	void extractTypeFromLabel() {
		assertThat(task.extractTypes(Collections.singleton("type/bug"), ""))
				.as("type/bug").containsOnly(Type.BUG);

		assertThat(task.extractTypes(Collections.singleton("type/enhancement"), ""))
				.as("type/enhancement").containsOnly(Type.FEATURE);

		assertThat(task.extractTypes(Collections.singleton("type/documentation"), ""))
				.as("type/documentation").containsOnly(Type.DOC_MISC);

		assertThat(task.extractTypes(Collections.singleton("type/chores"), ""))
				.as("type/chores").containsOnly(Type.DOC_MISC);

		assertThat(task.extractTypes(Collections.singleton("warn/something"), ""))
				.as("warn/*").containsOnly(Type.NOTEWORTHY);

		assertThat(task.extractTypes(Collections.singleton("type/whatever"), ""))
				.as("type/whatever").containsOnly(Type.UNCLASSIFIED);
	}

	@Test
	void extractTypeFromMultipleLabels() {
		Set<String> labels = new LinkedHashSet<>();
		labels.add("type/bug");
		labels.add("type/enhancement");
		labels.add("type/documentation");
		labels.add("whatever");

		assertThat(task.extractTypes(labels, "")).as("multiple labels")
				.containsOnly(Type.BUG, Type.FEATURE, Type.DOC_MISC);
	}

	@Test
	void extractMiscTypeFromBuildMessagePrefix() {
		String message = "[build] Foo";

		assertThat(task.extractTypes(Collections.emptySet(), message))
				.containsOnly(Type.DOC_MISC);
	}

	@Test
	void extractMiscTypeFromPolishMessagePrefix() {
		String message = "[polish] Foo";

		assertThat(task.extractTypes(Collections.emptySet(), message))
				.containsOnly(Type.DOC_MISC);
	}

	@Test
	void extractMiscTypeFromDocMessagePrefix() {
		String message = "[doc] Foo";

		assertThat(task.extractTypes(Collections.emptySet(), message))
				.containsOnly(Type.DOC_MISC);
	}

	@Test
	void extractUnclassifiedTypeFromRandomMessagePrefix() {
		String message = "fix #123 There was a [bug], needed to [polish] the [doc]";

		assertThat(task.extractTypes(Collections.emptySet(), message))
				.containsOnly(Type.UNCLASSIFIED);
	}

	@Test
	void noTitleCleanupFromMergeCommit() throws IOException {
		SimpleCommit mergeCommit = new SimpleCommit("sha1", "fullsha1",
				"merge #123 into 3.3 (#123)", "merge #123 into 3.3", "Simon Baslé",
				"sbasle@pivotal.io", "Simon Baslé", "sbasle@pivotal.io", true);

		assertThat(task.cleanupShortMessage(mergeCommit))
				.isEqualTo("merge #123 into 3.3 (#123)");
	}

	@Test
	void titleCleanupFixPrefix() throws IOException {
		SimpleCommit commit = new SimpleCommit("sha1", "fullsha1",
				"fix #123 Text from title", "fix #123 Some more text", "Simon Baslé",
				"sbasle@pivotal.io", "Simon Baslé", "sbasle@pivotal.io", false);

		assertThat(task.cleanupShortMessage(commit)).isEqualTo("Text from title");
	}

	@Test
	void titleCleanupSeePrefix() throws IOException {
		SimpleCommit commit = new SimpleCommit("sha1", "fullsha1",
				"see #123 Text from title", "see #123 Some more text", "Simon Baslé",
				"sbasle@pivotal.io", "Simon Baslé", "sbasle@pivotal.io", false);

		assertThat(task.cleanupShortMessage(commit)).isEqualTo("Text from title");
	}

	@Test
	void titleCleanupPrStyleSuffix() throws IOException {
		SimpleCommit commit = new SimpleCommit("sha1", "fullsha1",
				"Commit without issue (#123)", "fullMessage", "Simon Baslé",
				"sbasle@pivotal.io", "Simon Baslé", "sbasle@pivotal.io", false);

		assertThat(task.cleanupShortMessage(commit)).isEqualTo("Commit without issue");
	}

	@Test
	void titleCleanupBothPrefixAndSuffix() throws IOException {
		SimpleCommit commit = new SimpleCommit("sha1", "fullsha1",
				"prefix #123 Commit title (#123)", "fullMessage", "Simon Baslé",
				"sbasle@pivotal.io", "Simon Baslé", "sbasle@pivotal.io", false);

		assertThat(task.cleanupShortMessage(commit)).isEqualTo("Commit title");
	}

	// TODO test login extraction by mocking the task's commitToGithubMention method

	// TODO find a way to mock commits and thus test extractContributors

}
