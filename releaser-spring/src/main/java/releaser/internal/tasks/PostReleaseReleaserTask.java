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

package releaser.internal.tasks;

import releaser.internal.spring.Arguments;
import releaser.internal.tech.ExecutionResult;

/**
 * Marker interface for running post release tasks.
 */
public interface PostReleaseReleaserTask extends ReleaserTask {

	/**
	 * Executes the task but catches exceptions and converts them into result. When an
	 * exception is throw will treat it as an instability.
	 * @param args - arguments to run the task
	 * @return execution result
	 */
	default ExecutionResult apply(Arguments args) {
		try {
			return runTask(args);
		}
		catch (Exception ex) {
			return ExecutionResult.unstable(ex);
		}
	}

}
