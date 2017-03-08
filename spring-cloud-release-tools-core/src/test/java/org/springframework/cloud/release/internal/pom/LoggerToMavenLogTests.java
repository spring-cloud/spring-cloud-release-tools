package org.springframework.cloud.release.internal.pom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.cloud.release.internal.pom.LoggerToMavenLog;

import static org.mockito.BDDMockito.then;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggerToMavenLogTests {

	@Mock Logger logger;
	@InjectMocks LoggerToMavenLog loggerToMavenLog;
	RuntimeException exception = new RuntimeException();

	@Test public void isDebugEnabled() throws Exception {
		this.loggerToMavenLog.isDebugEnabled();

		then(this.logger).should().isDebugEnabled();
	}

	@Test public void debug() throws Exception {
		this.loggerToMavenLog.debug("foo");

		then(this.logger).should().debug("foo");
	}

	@Test public void debug1() throws Exception {
		this.loggerToMavenLog.debug("foo", this.exception);

		then(this.logger).should().debug("foo", this.exception);
	}

	@Test public void debug2() throws Exception {
		this.loggerToMavenLog.debug(exception);

		then(this.logger).should().debug("Exception occurred", this.exception);
	}

	@Test public void isInfoEnabled() throws Exception {
		this.loggerToMavenLog.isInfoEnabled();

		then(this.logger).should().isInfoEnabled();
	}

	@Test public void info() throws Exception {
		this.loggerToMavenLog.info("foo");

		then(this.logger).should().info("foo");
	}

	@Test public void info1() throws Exception {
		this.loggerToMavenLog.info("foo", this.exception);

		then(this.logger).should().info("foo", this.exception);
	}

	@Test public void info2() throws Exception {
		this.loggerToMavenLog.info(exception);

		then(this.logger).should().info("Exception occurred", this.exception);
	}

	@Test public void isWarnEnabled() throws Exception {
		this.loggerToMavenLog.isWarnEnabled();

		then(this.logger).should().isWarnEnabled();
	}

	@Test public void warn() throws Exception {
		this.loggerToMavenLog.warn("foo");

		then(this.logger).should().warn("foo");
	}

	@Test public void warn1() throws Exception {
		this.loggerToMavenLog.warn("foo", this.exception);

		then(this.logger).should().warn("foo", this.exception);
	}

	@Test public void warn2() throws Exception {
		this.loggerToMavenLog.warn(exception);

		then(this.logger).should().warn("Exception occurred", this.exception);
	}

	@Test public void isErrorEnabled() throws Exception {
		this.loggerToMavenLog.isErrorEnabled();

		then(this.logger).should().isErrorEnabled();
	}

	@Test public void error() throws Exception {
		this.loggerToMavenLog.error("foo");

		then(this.logger).should().error("foo");
	}

	@Test public void error1() throws Exception {
		this.loggerToMavenLog.error("foo", this.exception);

		then(this.logger).should().error("foo", this.exception);
	}

	@Test public void error2() throws Exception {
		this.loggerToMavenLog.error(exception);

		then(this.logger).should().error("Exception occurred", this.exception);
	}

}