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

package org.springframework.cloud.release.internal.spring;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ReleaserApplicationEventTests.Config.class,
		properties = "releaser.git.oauth-token=some-fake-token")
public class ReleaserApplicationEventTests {

	@Autowired
	ApplicationEventPublisher publisher;

	@Test
	public void should_throw_exceptions_when_at_least_one_task_is_failing() {
		this.publisher.publishEvent(
				new TaskCompleted(this, "foo", TaskAndException.skipped(Tasks.PUSH)));
		this.publisher.publishEvent(new TaskCompleted(this, "foo",
				TaskAndException.success(Tasks.CLOSE_MILESTONE)));
		this.publisher.publishEvent(new TaskCompleted(this, "foo",
				TaskAndException.failure(Tasks.DEPLOY, new RuntimeException("boom!"))));

		BDDAssertions.thenThrownBy(() -> {
			this.publisher.publishEvent(new BuildCompleted(this));
		}).hasMessageContaining("[BUILD UNSTABLE] The following");
	}

	@Test
	public void should_not_fail_when_all_tasks_not_failing() {
		this.publisher.publishEvent(
				new TaskCompleted(this, "foo", TaskAndException.skipped(Tasks.PUSH)));
		this.publisher.publishEvent(new TaskCompleted(this, "foo",
				TaskAndException.success(Tasks.CLOSE_MILESTONE)));

		this.publisher.publishEvent(new BuildCompleted(this));
	}

	@Configuration
	@EnableAutoConfiguration
	@ComponentScan({ "org.springframework.cloud.release.internal.options",
			"org.springframework.cloud.release.internal.sagan",
			"org.springframework.cloud.release.internal.spring" })
	static class Config {

	}

}
