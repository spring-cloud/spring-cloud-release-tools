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

import java.util.function.Consumer;

/**
 * Handles the result of the execution of the train or project release. Example: You can
 * print the results in a table, throw exceptions when the build was faulty or just log
 * them when it was unstable.
 */
public interface ExecutionResultHandler extends Consumer<ExecutionResult> {

}
