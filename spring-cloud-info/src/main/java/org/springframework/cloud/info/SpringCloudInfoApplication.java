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

package org.springframework.cloud.info;

import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({ SpringCloudInfoConfigurationProperties.class })
public class SpringCloudInfoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudInfoApplication.class, args);
	}

	@Bean
	public SpringCloudInfoService initializrSpringCloudVersionService(
			SpringCloudInfoConfigurationProperties properties) {
		Github github = new RtGithub(properties.getGit().getOauthToken());
		RestTemplate rest = new RestTemplateBuilder().build();
		return new InitializrSpringCloudInfoService(rest, github, new GithubPomReader(new MavenXpp3Reader(), rest));
	}

//	@Configuration
//	public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
//
//		@Override
//		protected void configure(HttpSecurity http) throws Exception {
//			http.authorizeRequests().anyRequest().permitAll().and().httpBasic().disable().csrf().disable();
//		}
//
//	}

}
