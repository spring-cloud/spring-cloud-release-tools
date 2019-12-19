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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
// import org.springframework.security.oauth2.client.OAuth2ClientContext;
// import org.springframework.security.oauth2.client.OAuth2RestTemplate;
// import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
// import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@SpringBootApplication
@RibbonClient("foo")
// @EnableOAuth2Client
// FIXME boot 2.0.0 spring-security 5.0 missing oauth
public class RibbonClientApplication {

	/*
	 * @LoadBalanced
	 *
	 * @Bean public OAuth2RestTemplate
	 * loadBalancedOauth2RestTemplate(OAuth2ProtectedResourceDetails resource,
	 * OAuth2ClientContext context) { return new OAuth2RestTemplate(resource, context); }
	 */

	public static void main(String[] args) {
		SpringApplication.run(RibbonClientApplication.class, args);
	}

}
