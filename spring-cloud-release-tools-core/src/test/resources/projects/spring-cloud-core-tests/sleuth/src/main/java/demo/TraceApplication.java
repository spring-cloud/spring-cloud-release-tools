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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TraceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraceApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}

@RestController
class HomeController {

	private static final Log log = LogFactory.getLog(HomeController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Value("${app.url:http://localhost:${local.server.port:${server.port:8080}}}")
	private String url;

	@RequestMapping("/")
	public String home() {
		log.info("Home");
		String s = this.restTemplate.getForObject(url + "/hi", String.class);
		return "hi/" + s;
	}

}

@RestController
class HiController {

	private static final Log log = LogFactory.getLog(HiController.class);

	@RequestMapping("/hi")
	public String hi() {
		log.info("Hi");
		return "hi";
	}

}