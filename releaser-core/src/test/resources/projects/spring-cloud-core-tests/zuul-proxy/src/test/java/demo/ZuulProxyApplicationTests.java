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

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class ZuulProxyApplicationTests {

	@Autowired
	DiscoveryClient discoveryClient;

	@LocalServerPort
	int port;

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Test
	public void ignoredPatternMissing() {
		ResponseEntity<String> result = this.restTemplate.getForEntity("http://localhost:" + this.port + "/missing",
				String.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
	}

	@Test
	public void forwardedPatternGood() {
		ResponseEntity<String> result = this.restTemplate.getForEntity("http://localhost:" + this.port + "/good",
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("GOOD!", result.getBody());
	}

	@Test
	public void forwardedPatternGoodWithPath() {
		ResponseEntity<String> result = this.restTemplate.getForEntity("http://localhost:" + this.port + "/good/stuff",
				String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("STUFF!", result.getBody());
	}

	@Test
	public void discoveryClientIsComposite() {
		assertTrue("discoveryClient is wrong type", this.discoveryClient instanceof CompositeDiscoveryClient);
	}

}
