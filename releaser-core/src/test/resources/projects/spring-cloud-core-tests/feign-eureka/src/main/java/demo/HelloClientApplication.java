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

import java.util.Arrays;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Spencer Gibb
 */
@SpringBootApplication
@EnableDiscoveryClient
@RestController
@EnableFeignClients
@RibbonClient(name = "hello", configuration = HelloRibbonClientConfiguration.class)
public class HelloClientApplication {

	@Autowired
	HelloClient client;

	public static void main(String[] args) {
		SpringApplication.run(HelloClientApplication.class, args);
	}

	@RequestMapping("/")
	public String hello() {
		return client.hello();
	}

	@FeignClient("hello")
	interface HelloClient {

		@RequestMapping(value = "/", method = GET)
		String hello();

	}

}

// Load balancer with fixed server list for "hello" pointing to example.com
@Configuration
class HelloRibbonClientConfiguration {

	@Bean
	public ILoadBalancer ribbonLoadBalancer() {
		// because of this, it doesn't use eureka to lookup the server,
		// but the classpath is tested
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Arrays.asList(new Server("example.com", 80)));
		return balancer;
	}

}
