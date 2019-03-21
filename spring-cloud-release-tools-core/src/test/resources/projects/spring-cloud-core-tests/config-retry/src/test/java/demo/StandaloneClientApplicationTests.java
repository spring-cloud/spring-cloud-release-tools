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

package demo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;

import apps.ConfigServer;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.AnnotationAwareRetryOperationsInterceptor;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
// Explicitly enable config client because test classpath has config server on it
@SpringBootTest(properties = { "spring.cloud.config.enabled=true",
		"logging.level.org.springframework.retry=TRACE" })
@DirtiesContext
public class StandaloneClientApplicationTests {

	private static ConfigurableApplicationContext context;
	@Autowired
	private ConfigServicePropertySourceLocator locator;

	@BeforeClass
	public static void delayConfigServer() {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000L);
				}
				catch (InterruptedException e) {
				}
				context = ConfigServer.start();
			}
		});
	}

	@AfterClass
	public static void shutdown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void contextLoads() throws Exception {
		assertTrue("ConfigServicePropertySourceLocator is not a proxy (so no retry)",
				AopUtils.isAopProxy(locator));
		AnnotationAwareRetryOperationsInterceptor advice = (AnnotationAwareRetryOperationsInterceptor) ((Advised) locator)
				.getAdvisors()[0].getAdvice();
		@SuppressWarnings("unchecked")
		Map<Method, MethodInterceptor> delegates = (Map<Method, MethodInterceptor>) ReflectionTestUtils
				.getField(advice, "delegates");
		Object obj = delegates.values().iterator().next();
		RetryTemplate retryTemplate = null;
		if (RetryOperationsInterceptor.class.isInstance(obj)) {
			// Prior to Boot 1.5.10
			RetryOperationsInterceptor interceptor = (RetryOperationsInterceptor) obj;
			retryTemplate = (RetryTemplate) ReflectionTestUtils.getField(interceptor,
					"retryOperations");
		}
		else if (Map.class.isInstance(obj)) {
			// Boot 1.5.10 and later
			Object[] methodInterceptors = ((Map) obj).values().toArray();
			RetryOperationsInterceptor interceptor = (RetryOperationsInterceptor) methodInterceptors[0];
			retryTemplate = (RetryTemplate) ReflectionTestUtils.getField(interceptor,
					"retryOperations");
			;
		}
		else {
			fail("Could not find RetryTemplate");
		}
		ExponentialBackOffPolicy backoff = (ExponentialBackOffPolicy) ReflectionTestUtils
				.getField(retryTemplate, "backOffPolicy");
		assertEquals(3000, backoff.getInitialInterval());
	}

}
