/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.release.internal.spring;

import java.util.Arrays;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 */
public class CompositeConsumerTests {

	@Test
	public void should_throw_exception_for_a_release_task() {
		CompositeConsumer compositeConsumer = new CompositeConsumer(Arrays.asList(
				new Task("foo", "foo", "foo", "foo",
						(args -> {})),
				new Task("bar", "bar", "bar", "bar",
						(args -> { throw new MyException(); }))
		));

		BDDAssertions.thenThrownBy(() ->
				compositeConsumer.accept(new Args(TaskType.RELEASE)))
				.isInstanceOf(MyException.class);
	}
}

class MyException extends RuntimeException {}