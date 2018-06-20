package org.springframework.cloud.release.internal.spring;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
public class TaskTests {

	@Rule public OutputCapture capture = new OutputCapture();

	@Test public void should_successfully_execute_task() {
		final AtomicBoolean someBool = new AtomicBoolean();
		Task task = new Task("foo", "bar", "baz", "descr", new Consumer<Args>() {
			@Override public void accept(Args args) {
				someBool.set(true);
			}
		});

		task.execute(new Args(TaskType.RELEASE));

		then(someBool.get()).isTrue();
	}

	@Test public void should_fail_with_nice_text_on_exception() {
		final AtomicBoolean someBool = new AtomicBoolean();
		Task task = new Task("foo", "bar", "baz", "descr", args -> {
			someBool.set(true);
			throw new RuntimeException("foooooooo");
		});

		thenThrownBy(() -> task.execute(new Args(TaskType.RELEASE)))
				.isInstanceOf(RuntimeException.class);
		then(someBool.get()).isTrue();
		then(this.capture.toString())
				.contains("BUILD FAILED!!!")
				.contains("Exception occurred for task <foo>")
				.contains("with description <descr>");
	}
}