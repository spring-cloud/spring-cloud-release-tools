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

package org.springframework.cloud.release.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.Parser;
import org.springframework.cloud.release.internal.spring.SpringReleaser;
import org.springframework.cloud.release.internal.tech.MakeBuildUnstableException;

@SpringBootApplication
public class ReleaserApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(ReleaserApplication.class);

	@Autowired
	SpringReleaser releaser;

	@Autowired
	Parser parser;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(ReleaserApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}

	@Override
	public void run(String... strings) {
		Options options = this.parser.parse(strings);
		try {
			this.releaser.release(options);
		}
		catch (MakeBuildUnstableException ex) {
			log.error(
					"[BUILD UNSTABLE] The following exceptions took place in the post release process",
					ex);
			log.error(
					"[BUILD UNSTABLE] The release happened successfully, but there were post release issues");
			log.error("[BUILD UNSTABLE] An exception that should make "
					+ "the build unstable occurred. Will not throw an exception.");
		}
		catch (Throwable th) {
			log.error("Exception occurred for the releaser", th);
			throw th;
		}
		System.exit(0);
	}

}
