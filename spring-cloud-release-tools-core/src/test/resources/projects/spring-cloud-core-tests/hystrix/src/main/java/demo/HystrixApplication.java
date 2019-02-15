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

package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableCircuitBreaker
@RestController
public class HystrixApplication {

	@Autowired
	private MyService myService;

	public static void main(String[] args) {
		SpringApplication.run(HystrixApplication.class, args);
	}

	@Bean
	public MyService myService() {
		return new MyService();
	}

	@RequestMapping("/")
	public String ok() {
		return this.myService.ok();
	}

	@RequestMapping("/fail")
	public String fail(
			@RequestHeader(name = "X-No-Fail", required = false) String noFail) {
		if (noFail != null) {
			return this.myService.fail(false);
		}
		return this.myService.fail(true);
	}

}
