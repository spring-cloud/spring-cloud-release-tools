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
public class SpringReleaserTests {

	@Mock Releaser releaser;
	FirstConsumer first = new FirstConsumer();
	SecondConsumer second = new SecondConsumer();
	ThirdConsumer third = new ThirdConsumer();
	List<Task> tasks = Arrays.asList(new Task[] {
			Tasks.task("first", "1", "", "", first),
			Tasks.task("second", "2", "", "", second),
			Tasks.task("third", "3", "", "", third)
	});
	Args nonInteractiveArgs = nonInteractiveArgs();

	SpringReleaser springReleaser;

	@Before
	public void setup() {
		this.springReleaser = new SpringReleaser(this.releaser, new ReleaserProperties(), this.tasks);
	}

	@Test
	public void should_throw_exception_when_an_invalid_option_was_picked() throws Exception {
		Options options = new OptionsBuilder().interactive(false).options();

		thenThrownBy(() -> this.springReleaser.processOptions(options, this.nonInteractiveArgs))
			.hasMessageContaining("You haven't picked any recognizable option");
	}

	@Test
	public void should_execute_only_tasks_after_the_provided_one_using_full_name() throws Exception {
		Options options = new OptionsBuilder().startFrom("second").options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}


	@Test
	public void should_execute_only_tasks_after_the_provided_one_using_short_name() throws Exception {
		Options options = new OptionsBuilder().startFrom("2").options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_full_name() throws Exception {
		Options options = new OptionsBuilder().range("second-third").options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_short_name() throws Exception {
		Options options = new OptionsBuilder().range("2-3").options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_full_name_with_same_range() throws Exception {
		Options options = new OptionsBuilder().range("second-second").options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_only_tasks_from_range_using_short_name_with_same_range() throws Exception {
		Options options = new OptionsBuilder().range("2-2").options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isFalse();
		then(this.second.executed).isTrue();
		then(this.third.executed).isFalse();
	}

	@Test
	public void should_execute_only_tasks_from_multi_using_full_name() throws Exception {
		Options options = new OptionsBuilder().taskNames(list("first", "third")).options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	@Test
	public void should_execute_only_tasks_from_multi_using_short_name() throws Exception {
		Options options = new OptionsBuilder().taskNames(list("1", "3")).options();

		this.springReleaser.processOptions(options, this.nonInteractiveArgs);

		then(this.first.executed).isTrue();
		then(this.second.executed).isFalse();
		then(this.third.executed).isTrue();
	}

	private Args interactiveArgs() {
		return new Args(null, null, null, null, null, null, true);
	}

	private Args nonInteractiveArgs() {
		return new Args(null, null, null, null, null, null, false);
	}

	private List<String> list(String... list) {
		return Arrays.asList(list);
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