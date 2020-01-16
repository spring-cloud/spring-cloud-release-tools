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

import com.jcabi.github.Github;
import com.jcabi.github.mock.MkGithub;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import releaser.internal.Releaser;
import releaser.internal.git.ProjectGitHandler;
import releaser.internal.options.Parser;
import releaser.internal.spring.ExecutionResultHandler;
import releaser.internal.spring.SpringReleaser;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * A configuration for tests, that mocks external clients but reuses the base bean
 * declaration for tasks.
 *
 * @author Simon Baslé
 */
@Configuration
@Profile("test")
public class ReactorTestConfiguration {

	@Bean
	RestartSiteProjectPostReleaseTask restartSiteProjectPostReleaseTask(Releaser releaser, CfClient cfClient, @Value("${cf.reactorAppName}") String reactorAppName) {
		return new RestartSiteProjectPostReleaseTask(releaser, cfClient, reactorAppName);
	}

	@Bean
	GenerateReleaseNotesTask releaseNotesTask(Github github, ProjectGitHandler gitHandler) {
		return new GenerateReleaseNotesTask(github, gitHandler);
	}

	@Bean
	ProjectGitHandler mockGitHandler() {
		return Mockito.mock(ProjectGitHandler.class);
	}

	@Bean
	MkGithub mockGithub() {
		try {
			return new MkGithub();
		}
		catch (IOException e) {
			throw new BeanCreationException("Unable to create mock Github bean", e);
		}
	}

	@Bean
	CfClient mockCfClient() {
		return BDDMockito.mock(CfClient.class);
	}

	@Bean
	@Primary
	SpringReleaser mockReleaser() {
		return Mockito.mock(SpringReleaser.class);
	}

	@Bean
	@Primary
	ExecutionResultHandler mockExecutionResultHandler() {
		return Mockito.mock(ExecutionResultHandler.class);
	}

	@Bean
	@Primary
	Parser mockParser() {
		return Mockito.mock(Parser.class);
	}

}
