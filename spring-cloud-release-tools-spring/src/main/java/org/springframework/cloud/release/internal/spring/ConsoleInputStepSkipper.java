package org.springframework.cloud.release.internal.spring;

/**
 * @author Marcin Grzejszczak
 */
class ConsoleInputStepSkipper implements StepSkipper {

	@Override public boolean skipStep() {
		String input = chosenOption();
		switch (input.toLowerCase()) {
		case "s":
			return true;
		case "q":
			System.exit(0);
			return true;
		default:
			return false;
		}
	}

	String chosenOption() {
		return System.console().readLine();
	}
}
