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

package releaser.internal.tech;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

public class ReleaserProcessExecutor {

	private static final Logger log = LoggerFactory.getLogger(ReleaserProcessExecutor.class);

	private static String[] OS_OPERATORS = { "|", "<", ">", "||", "&&" };

	private String workingDir;

	public ReleaserProcessExecutor(String workingDir) {
		this.workingDir = workingDir;
	}

	public void runCommand(String[] commands, long waitTimeInMinutes) {
		doRunCommand(commands, waitTimeInMinutes);
	}

	public String runCommandWithOutput(String[] commands, long waitTimeInMinutes) {
		return doRunCommand(commands, waitTimeInMinutes).outputUTF8();
	}

	private ProcessResult doRunCommand(String[] commands, long waitTimeInMinutes) {
		String workingDir = this.workingDir;
		log.info("Will run the command from [{}] and wait for result for [{}] minutes", workingDir, waitTimeInMinutes);

		try {
			ProcessExecutor processExecutor = processExecutor(commands, workingDir).timeout(waitTimeInMinutes,
					TimeUnit.MINUTES);
			final ProcessResult processResult = doExecute(processExecutor);
			int processExitValue = processResult.getExitValue();
			if (processExitValue != 0) {
				throw new IllegalStateException("The process has exited with exit code [" + processExitValue + "]");
			}
			return processResult;
		}
		catch (InterruptedException | IOException e) {
			throw new IllegalStateException("Process execution failed", e);
		}
		catch (TimeoutException e) {
			log.error("The command hasn't managed to finish in [{}] minutes", waitTimeInMinutes);
			throw new IllegalStateException("Process waiting time of [" + waitTimeInMinutes + "] minutes exceeded", e);
		}
	}

	ProcessResult doExecute(ProcessExecutor processExecutor)
			throws IOException, InterruptedException, TimeoutException {
		return processExecutor.execute();
	}

	ProcessExecutor processExecutor(String[] commands, String workingDir) {
		String[] commandsToRun = commands;
		String lastArg = String.join(" ", commands);
		if (Arrays.stream(OS_OPERATORS).anyMatch(lastArg::contains)) {
			commandsToRun = commandToExecute(lastArg);
		}
		log.info("Will run the command [{}]", Arrays.toString(commandsToRun));
		return new ProcessExecutor().command(commandsToRun).destroyOnExit().readOutput(true)
				// releaser.commands logger should be configured to redirect
				// only to a file (with additivity=false). ideally the root logger should
				// append to same file on top of whatever root appender, so that file
				// contains the most output
				.redirectOutputAlsoTo(Slf4jStream.of("releaser.commands").asInfo())
				.redirectErrorAlsoTo(Slf4jStream.of("releaser.commands").asWarn()).directory(new File(workingDir));
	}

	String[] commandToExecute(String lastArg) {
		return new String[] { "/bin/bash", "-c", lastArg };
	}

}
