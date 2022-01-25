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

package releaser.internal.spring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import releaser.internal.tech.ExecutionResult;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class SpringBatchExecutionResultHandler implements ExecutionResultHandler {

	private static final Logger log = LoggerFactory.getLogger(SpringBatchExecutionResultHandler.class);

	private final ConfigurableApplicationContext context;

	private final BuildReportHandler buildReportHandler;

	SpringBatchExecutionResultHandler(BuildReportHandler buildReportHandler, ConfigurableApplicationContext context) {
		this.buildReportHandler = buildReportHandler;
		this.context = context;
	}

	@Override
	public void accept(ExecutionResult executionResult) {
		this.buildReportHandler.reportBuildSummary();
		if (executionResult.isFailure()) {
			log.error("At least one failure occurred while running the release process",
					executionResult.foundExceptions());
			handleFailedBuild();
			exitWithException();
			return;
		}
		else if (executionResult.isUnstable()) {
			log.warn("The release went fine, however at least one unstable post release task was found",
					executionResult.foundExceptions());
			handleUnstableException();
		}
		handleStableBuild();
		exitSuccessfully();
	}

	void exitSuccessfully() {
		System.exit(SpringApplication.exit(this.context, () -> 0));
	}

	void exitWithException() {
		System.exit(SpringApplication.exit(this.context, () -> 1));
	}

	// File creation required by Jenkins
	private void handleUnstableException() {
		File buildStatus = new File("build_status");
		try {
			buildStatus.createNewFile();
			String text = "[BUILD UNSTABLE] The release happened successfully, but there were post release issues";
			Files.write(buildStatus.toPath(), text.getBytes());
			log.info("\n\n\n  ___ _   _ ___ _    ___    _   _ _  _ ___ _____ _   ___ _    ___ \n"
					+ " | _ ) | | |_ _| |  |   \\  | | | | \\| / __|_   _/_\\ | _ ) |  | __|\n"
					+ " | _ \\ |_| || || |__| |) | | |_| | .` \\__ \\ | |/ _ \\| _ \\ |__| _| \n"
					+ " |___/\\___/|___|____|___/   \\___/|_|\\_|___/ |_/_/ \\_\\___/____|___|\n"
					+ "                                                                  ");
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"[BUILD UNSTABLE] Couldn't create a file to show that the build is unstable");
		}
	}

	// File creation required by Jenkins
	private void handleStableBuild() {
		File buildStatus = new File("build_status");
		if (buildStatus.exists()) {
			log.info("Build status file has already been created!");
			return;
		}
		try {
			buildStatus.createNewFile();
			String text = "[BUILD STABLE] All the release steps have been successfully executed!";
			Files.write(buildStatus.toPath(), text.getBytes());
			log.info("\n\n\n  ___ _   _ ___ _    ___    ___ _   _  ___ ___ ___ ___ ___ ___ _   _ _    \n"
					+ " | _ ) | | |_ _| |  |   \\  / __| | | |/ __/ __| __/ __/ __| __| | | | |   \n"
					+ " | _ \\ |_| || || |__| |) | \\__ \\ |_| | (_| (__| _|\\__ \\__ \\ _|| |_| | |__ \n"
					+ " |___/\\___/|___|____|___/  |___/\\___/ \\___\\___|___|___/___/_|  \\___/|____|\n"
					+ "                                                                          ");
		}
		catch (IOException e) {
			log.info("Failed to store the file but the build was stable");
		}
	}

	// File creation required by Jenkins
	private void handleFailedBuild() {
		File buildStatus = new File("build_status");
		if (buildStatus.exists()) {
			log.info("Build status file has already been created!");
			return;
		}
		try {
			buildStatus.createNewFile();
			String text = "[BUILD FAILED] There were exceptions while doing the release!";
			Files.write(buildStatus.toPath(), text.getBytes());
			log.info("\n\n\n  ___ _   _ ___ _    ___    ___ _   ___ _    ___ ___  \n"
					+ " | _ ) | | |_ _| |  |   \\  | __/_\\ |_ _| |  | __|   \\ \n"
					+ " | _ \\ |_| || || |__| |) | | _/ _ \\ | || |__| _|| |) |\n"
					+ " |___/\\___/|___|____|___/  |_/_/ \\_\\___|____|___|___/ \n"
					+ "                                                      ");
		}
		catch (IOException e) {
			throw new IllegalStateException("[BUILD FAILED] Couldn't create a file to show that the build is unstable");
		}
	}

}
