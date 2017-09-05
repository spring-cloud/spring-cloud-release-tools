package org.springframework.cloud.release.internal.spring;

import edu.emory.mathcs.backport.java.util.Arrays;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
	Task firstTask = task("first", "1", "", "", first);
	List<Task> tasks = Arrays.asList(new Task[] {
			firstTask,
			task("second", "2", "", "", second),
			task("third", "3", "", "", third)
	});

	OptionsProcessor optionsProcessor;

	@Before
	public void setup() {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks);
	}

	@Test
	public void should_throw_exception_when_an_invalid_option_was_picked() throws Exception {
		Options options = nonInteractiveOpts().options();

		thenThrownBy(() -> this.optionsProcessor.processOptions(options, args()))
			.hasMessageContaining("You haven't picked any recognizable option");
	}

	@Test
	public void should_execute_only_tasks_after_the_provided_one_using_full_name() throws Exception {
		Options options = nonInteractiveOpts().startFrom("second").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}


	@Test
	public void should_execute_only_tasks_after_the_provided_one_using_short_name() throws Exception {
		Options options = nonInteractiveOpts().startFrom("2").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_full_name() throws Exception {
		Options options = nonInteractiveOpts().range("second-third").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_short_name() throws Exception {
		Options options = nonInteractiveOpts().range("2-3").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_full_name_with_same_range() throws Exception {
		Options options = nonInteractiveOpts().range("second-second").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_short_name_with_same_range() throws Exception {
		Options options = nonInteractiveOpts().range("2-2").options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_only_tasks_from_multi_using_full_name() throws Exception {
		Options options = nonInteractiveOpts().taskNames(list("first", "third")).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_multi_using_short_name() throws Exception {
		Options options = nonInteractiveOpts().taskNames(list("1", "3")).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_interactively_only_single_task() throws Exception {
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
	public void should_execute_interactively_range_of_tasks() throws Exception {
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
	public void should_execute_interactively_start_from() throws Exception {
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
	public void should_execute_interactively_multi() throws Exception {
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
	public void should_execute_full_release() throws Exception {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override Task releaseTask() {
				return firstTask;
			}
		};
		Options options = nonInteractiveOpts().fullRelease(true).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_full_verbose_release() throws Exception {
		this.optionsProcessor = new OptionsProcessor(this.releaser, new ReleaserProperties(), this.tasks) {
			@Override Task releaseVerboseTask() {
				return firstTask;
			}
		};
		Options options = interactiveOpts().fullRelease(true).options();

		this.optionsProcessor.processOptions(options, args());

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isFalse();
	}

	private OptionsBuilder interactiveOpts() {
		return new OptionsBuilder();
	}

	private OptionsBuilder nonInteractiveOpts() {
		return new OptionsBuilder().interactive(false);
	}

	private Args args() {
		return new Args(null, null, null, null, null, null, false);
	}

	private List<String> list(String... list) {
		return Arrays.asList(list);
	}

	static Task task(String name, String shortName, String header, String description, Consumer<Args> function) {
		return new Task(name, shortName, header, description, function) {
			@Override String chosenOption() {
				return "whatever";
			}
		};
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