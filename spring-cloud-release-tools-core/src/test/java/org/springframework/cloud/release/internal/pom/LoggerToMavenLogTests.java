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

package org.springframework.cloud.release.internal.pom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.BDDMockito.then;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggerToMavenLogTests {

	@Mock
	Logger logger;

	@InjectMocks
	LoggerToMavenLog loggerToMavenLog;

	RuntimeException exception = new RuntimeException();

	@Test
	public void isDebugEnabled() {
		this.loggerToMavenLog.isDebugEnabled();

		then(this.logger).should().isDebugEnabled();
	}

	@Test
	public void debug() {
		this.loggerToMavenLog.debug("foo");

		then(this.logger).should().debug("foo");
	}

	@Test
	public void debug1() {
		this.loggerToMavenLog.debug("foo", this.exception);

		then(this.logger).should().debug("foo", this.exception);
	}

	@Test
	public void debug2() {
		this.loggerToMavenLog.debug(this.exception);

		then(this.logger).should().debug("Exception occurred", this.exception);
	}

	@Test
	public void isInfoEnabled() {
		this.loggerToMavenLog.isInfoEnabled();

		then(this.logger).should().isInfoEnabled();
	}

	@Test
	public void info() {
		this.loggerToMavenLog.info("foo");

		then(this.logger).should().info("foo");
	}

	@Test
	public void info1() {
		this.loggerToMavenLog.info("foo", this.exception);

		then(this.logger).should().info("foo", this.exception);
	}

	@Test
	public void info2() {
		this.loggerToMavenLog.info(this.exception);

		then(this.logger).should().info("Exception occurred", this.exception);
	}

	@Test
	public void isWarnEnabled() {
		this.loggerToMavenLog.isWarnEnabled();

		then(this.logger).should().isWarnEnabled();
	}

	@Test
	public void warn() {
		this.loggerToMavenLog.warn("foo");

		then(this.logger).should().warn("foo");
	}

	@Test
	public void warn1() {
		this.loggerToMavenLog.warn("foo", this.exception);

		then(this.logger).should().warn("foo", this.exception);
	}

	@Test
	public void warn2() {
		this.loggerToMavenLog.warn(this.exception);

		then(this.logger).should().warn("Exception occurred", this.exception);
	}

	@Test
	public void isErrorEnabled() {
		this.loggerToMavenLog.isErrorEnabled();

		then(this.logger).should().isErrorEnabled();
	}

	@Test
	public void error() {
		this.loggerToMavenLog.error("foo");

		then(this.logger).should().error("foo");
	}

	@Test
	public void error1() {
		this.loggerToMavenLog.error("foo", this.exception);

		then(this.logger).should().error("foo", this.exception);
	}

	@Test
	public void error2() {
		this.loggerToMavenLog.error(this.exception);

		then(this.logger).should().error("Exception occurred", this.exception);
	}

}
