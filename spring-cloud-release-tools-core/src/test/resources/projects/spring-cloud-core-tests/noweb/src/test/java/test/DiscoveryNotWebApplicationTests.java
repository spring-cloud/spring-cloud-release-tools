/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test;

import java.util.List;

import demo.NotWebApplication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DiscoveryNotWebApplicationTests {

	@Autowired
	private DiscoveryClient discoveryClient;

	@Test
	public void testDiscoveryClientIsSimple() {
		assertTrue("discoveryClient is wrong instance type",
				discoveryClient instanceof CompositeDiscoveryClient);
	}

	/*
	 * This works because the SimpleDiscoveryClient is installed if there is
	 * no @EnableDiscoveryClient *and* there is no implementation on the classpath
	 */
	@SpringBootApplication
	protected static class NoDiscoveryApplication implements CommandLineRunner {

		private static Log logger = LogFactory.getLog(NotWebApplication.class);

		@Autowired
		private DiscoveryClient client;

		@Override
		public void run(String... args) throws Exception {
			List<String> services = client.getServices();
			logger.info("Services: " + services);
		}

	}

}
