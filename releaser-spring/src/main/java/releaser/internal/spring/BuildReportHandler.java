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

/**
 * Handles reporting the results of the build. Similar to {@link ExecutionResultHandler}
 * with the difference that it could be invoked before the final result is determined, and
 * shouldn't handle exiting the application.
 */
public interface BuildReportHandler {

	/**
	 * Display a report summarizing the state of the build so far. Can be invoked during
	 * normal execution, so it should filter out eg. tasks that are running.
	 */
	void reportBuildSummary();

}
