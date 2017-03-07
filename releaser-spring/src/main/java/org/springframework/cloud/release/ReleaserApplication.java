/*
 *  Copyright 2013-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.cloud.release;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.release.internal.ProjectUpdater;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootApplication
public class ReleaserApplication implements CommandLineRunner {

	private static final Logger log = getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		SpringApplication.run(ReleaserApplication.class, args);
	}

	@Autowired ProjectUpdater projectUpdater;

	@Override public void run(String... strings) throws Exception {
		String workingDir = System.getProperty("user.dir");
		log.info("Will run the application for root folder [{}]", workingDir);
		this.projectUpdater.updateProject(new File(workingDir));
		System.exit(0);
	}
}
