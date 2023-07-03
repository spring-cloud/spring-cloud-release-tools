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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class TestReleaserProcessExecutor extends ReleaserProcessExecutor {

	public int counter = 0;

	final File temporaryFolder;

	public TestReleaserProcessExecutor(String workingDir, File temporaryFolder) {
		super(workingDir);
		this.temporaryFolder = temporaryFolder;
	}

	@Override
	ProcessExecutor processExecutor(String[] commands, String workingDir) {
		this.counter++;
		final ProcessExecutor processExecutor = super.processExecutor(commands, workingDir);

		File tempFile = tmpFile("/builder/resolved/resolved.log");
		try {
			tempFile.createNewFile();
			OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile));

			return processExecutor
					// use redirectOutputAlsoTo to avoid overriding all output
					// destinations
					.redirectOutput(fos);
		}
		catch (IOException e) {
			e.printStackTrace();
			return processExecutor;
		}
	}

	@Override
	public ProcessResult doExecute(ProcessExecutor processExecutor)
			throws IOException, InterruptedException, TimeoutException {
		return super.doExecute(processExecutor);
	}

	private File tmpFile(String relativePath) {
		return new File(this.temporaryFolder, relativePath);
	}

}
