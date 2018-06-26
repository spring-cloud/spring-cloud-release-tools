package org.springframework.cloud.release.internal.spring;

/**
 * Reads input (e.g. from the console)
 *
 * @author Marcin Grzejszczak
 */
interface StepSkipper {
	boolean skipStep();
}
