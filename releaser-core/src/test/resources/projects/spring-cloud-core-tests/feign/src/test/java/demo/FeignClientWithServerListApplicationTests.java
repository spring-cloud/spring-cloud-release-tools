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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
// We disable Hystrix because we are not concerned about testing circuit breakers in this
// test and it eliminates hystrix timeouts from messing with the request
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "myexample.ribbon.listOfServers:example.com", "feign.hystrix.enabled=false" })
@DirtiesContext
public class FeignClientWithServerListApplicationTests {

	@Autowired
	private RestClient client;

	@Test
	public void clientConnects() {
		assertTrue(client.hello().contains("<html"));
	}

	@FeignClient(value = "myexample")
	static interface RestClient {

		@RequestMapping(value = "/", method = RequestMethod.GET)
		String hello();

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableFeignClients
	protected static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(FeignClientApplication.class, args);
		}

	}

}
