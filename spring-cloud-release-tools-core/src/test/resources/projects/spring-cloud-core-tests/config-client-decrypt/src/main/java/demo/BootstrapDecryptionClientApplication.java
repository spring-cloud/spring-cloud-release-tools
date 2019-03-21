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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.env.Environment;

/**
 * @author Dave Syer
 */
@SpringBootApplication
public class BootstrapDecryptionClientApplication implements CommandLineRunner {

	private static Log logger = LogFactory
			.getLog(BootstrapDecryptionClientApplication.class);

	@Autowired
	private ConfigClientProperties config;

	@Autowired
	private Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(BootstrapDecryptionClientApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("Config Server URI: " + this.config.getUri());
		logger.info("And info.bar: " + this.environment.getProperty("info.bar"));
		logger.info("And info.spam: " + this.environment.getProperty("info.spam"));
	}

}
