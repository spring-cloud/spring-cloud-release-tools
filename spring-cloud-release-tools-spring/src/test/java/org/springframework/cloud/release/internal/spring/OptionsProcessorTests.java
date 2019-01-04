package org.springframework.cloud.release.internal.spring;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.release.internal.Releaser;
import org.springframework.cloud.release.internal.ReleaserProperties;
import org.springframework.cloud.release.internal.options.Options;
import org.springframework.cloud.release.internal.options.OptionsBuilder;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class OptionsProcessorTests {

	@Mock Releaser releaser;
	FirstConsumer first = new FirstConsumer();
	SecondConsumer second = new SecondConsumer();
	ThirdConsumer third = new ThirdConsumer();
	Task firstTask = task("first", "1", "", "", this.first);
	List<Task> tasks = Arrays.asList(new Task[] {
			this.firstTask,
			task("second", "2", "", "", this.second),
			task("third", "3", "", "", this.third)
	});

	OptionsProcessor optionsProcessor;

	@Before
	public void setup() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks);
		Task.stepSkipper = () -> false;
	}

	@After
	public void clean() {
		Task.stepSkipper = new ConsoleInputStepSkipper();
	}

	@Test
	public void should_throw_exception_when_an_invalid_option_was_picked() {
		Options options = nonInteractiveOpts().options();

		thenThrownBy(() -> this.optionsProcessor.processOptions(options, args()))
			.hasMessageContaining("You haven't picked any recognizable option");
	}

	@Test
	public void should_execute_only_tasks_after_the_provided_one_using_full_name() {
		Options options = nonInteractiveOpts().startFrom("second").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}


	@Test
	public void should_execute_only_tasks_after_the_provided_one_using_short_name() {
		Options options = nonInteractiveOpts().startFrom("2").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_full_name() {
		Options options = nonInteractiveOpts().range("second-third").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_short_name() {
		Options options = nonInteractiveOpts().range("2-3").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_full_name_with_same_range() {
		Options options = nonInteractiveOpts().range("second-second").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_short_name_with_same_range() {
		Options options = nonInteractiveOpts().range("2-2").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_only_tasks_from_multi_using_full_name() {
		Options options = nonInteractiveOpts().taskNames(list("first", "third")).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_multi_using_short_name() {
		Options options = nonInteractiveOpts().taskNames(list("1", "3")).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_interactively_only_single_task() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override String chosenOption() {
				return "0";
			}
		};
		Options options = interactiveOpts().options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_interactively_range_of_tasks() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override String chosenOption() {
				return "0-1";
			}
		};
		Options options = interactiveOpts().options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_interactively_start_from() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override String chosenOption() {
				return "1-";
			}
		};
		Options options = interactiveOpts().options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_interactively_multi() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override String chosenOption() {
				return "0,2";
			}
		};
		Options options = interactiveOpts().options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_full_release() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override Task releaseTask() {
				return OptionsProcessorTests.this.firstTask;
			}

			@Override String chosenOption() {
				return "0";
			}
		};
		Options options = nonInteractiveOpts().fullRelease(true).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_full_verbose_release() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override Task releaseVerboseTask() {
				return OptionsProcessorTests.this.firstTask;
			}

			@Override String chosenOption() {
				return "0";
			}
		};
		Options options = interactiveOpts().fullRelease(true).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_remove_single_quotes() {
		Options options = interactiveOpts().fullRelease(true)
				.range("'1-2'")
				.startFrom("'c'")
				.taskNames(Arrays.asList("'a'", "'b'"))
				.options();

		then(options.range).isEqualTo("1-2");
		then(options.startFrom).isEqualTo("c");
		then(options.taskNames).containsOnly("a", "b");
	}

	private OptionsBuilder interactiveOpts() {
		return new OptionsBuilder();
	}

	private OptionsBuilder nonInteractiveOpts() {
		return new OptionsBuilder().interactive(false);
	}

	private Args args() {
		return new Args(null, null, null, null, null, null, false, TaskType.RELEASE, null);
	}

	private List<String> list(String... list) {
		return Arrays.asList(list);
	}

	static Task task(String name, String shortName, String header, String description, Consumer<Args> function) {
		return new Task(name, shortName, header, description, function);
	}

}

class FirstConsumer implements Consumer<Args> {

	boolean executed;

	@Override public void accept(Args o) {
		this.executed = true;
	}
}

class SecondConsumer implements Consumer<Args> {

	boolean executed;

	@Override public void accept(Args o) {
		this.executed = true;
	}
}

class ThirdConsumer implements Consumer<Args> {

	boolean executed;

	@Override public void accept(Args o) {
		this.executed = true;
	}
}