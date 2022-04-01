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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class EurekaServerApplicationTests {

	@Autowired
	DiscoveryClient discoveryClient;

	@Test
	public void contextLoads() {
	}

	@Test
	public void discoveryClientIsEureka() {
		assertTrue("discoveryClient is wrong type " + discoveryClient.getClass(),
				discoveryClient instanceof CompositeDiscoveryClient);
		CompositeDiscoveryClient compositeDiscoveryClient = (CompositeDiscoveryClient) discoveryClient;
		assertTrue("composite discovery client should be composed of Eureka and Simple Discovery client's",
				compositeDiscoveryClient.getDiscoveryClients().size() == 2);

		assertTrue("the composed discovery client should have a EurekaDiscoveryClient with a higher precedence ",
				compositeDiscoveryClient.getDiscoveryClients().get(0) instanceof EurekaDiscoveryClient);
	}

}
